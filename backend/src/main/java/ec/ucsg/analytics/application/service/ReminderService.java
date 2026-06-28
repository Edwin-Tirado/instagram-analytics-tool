package ec.ucsg.analytics.application.service;

import ec.ucsg.analytics.application.dto.request.ReminderRequest;
import ec.ucsg.analytics.application.dto.response.ReminderResponse;
import ec.ucsg.analytics.domain.enums.EventStatus;
import ec.ucsg.analytics.infrastructure.mail.EmailNotificationService;
import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.model.Event;
import ec.ucsg.analytics.domain.model.Reminder;
import ec.ucsg.analytics.domain.repository.EventRepository;
import ec.ucsg.analytics.domain.repository.ReminderRepository;
import ec.ucsg.analytics.domain.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository       reminderRepository;
    private final EventRepository          eventRepository;
    private final UserRepository           userRepository;
    private final EmailNotificationService emailService;

    // ── Crear recordatorio ───────────────────────────────────────────

    @Transactional
    public ReminderResponse createReminder(String userEmail, ReminderRequest request) {
        AppUser user  = findUser(userEmail);
        Event   event = findApprovedFutureEvent(request.eventId());

        try {
            Reminder reminder = Reminder.builder()
                .user(user)
                .event(event)
                .minutesBefore(request.minutesBefore())
                .build();

            Reminder saved = reminderRepository.save(reminder);
            log.info("Recordatorio creado: evento='{}', usuario={}, minutos={}",
                event.getTitle(), userEmail, request.minutesBefore());

            // Email de confirmación inmediato
            emailService.sendReminderConfirmation(user, event, saved);

            return toResponse(saved);

        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                "Ya tienes un recordatorio de %d minutos para este evento".formatted(request.minutesBefore()));
        }
    }

    // ── Eliminar recordatorio ────────────────────────────────────────

    @Transactional
    public void deleteReminder(UUID reminderId, String userEmail) {
        Reminder reminder = reminderRepository.findById(reminderId)
            .orElseThrow(() -> new EntityNotFoundException("Recordatorio no encontrado: " + reminderId));

        if (!reminder.getUser().getEmail().equals(userEmail)) {
            throw new IllegalStateException("No tienes permiso para eliminar este recordatorio");
        }

        reminderRepository.delete(reminder);
        log.info("Recordatorio {} eliminado por {}", reminderId, userEmail);
    }

    // ── Listar recordatorios del usuario ────────────────────────────

    @Transactional(readOnly = true)
    public List<ReminderResponse> getUserReminders(String userEmail) {
        AppUser user = findUser(userEmail);
        return reminderRepository.findByUserId(user.getId()).stream()
            .map(this::toResponse)
            .toList();
    }

    // ── Privados ─────────────────────────────────────────────────────

    private AppUser findUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + email));
    }

    private Event findApprovedFutureEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EntityNotFoundException("Evento no encontrado: " + eventId));

        if (event.getStatus() != EventStatus.APPROVED) {
            throw new IllegalStateException("El evento no está publicado");
        }
        if (event.getEventDate() == null || event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("El evento ya finalizó o no tiene fecha definida");
        }
        return event;
    }

    private ReminderResponse toResponse(Reminder r) {
        return new ReminderResponse(
            r.getId(),
            r.getEvent().getId(),
            r.getEvent().getTitle(),
            r.getEvent().getEventDate(),
            r.getMinutesBefore(),
            r.isSent(),
            r.getCreatedAt()
        );
    }
}
