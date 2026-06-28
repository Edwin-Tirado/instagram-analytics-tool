package ec.ucsg.analytics.application.service;

import ec.ucsg.analytics.application.dto.request.UpdateEventRequest;
import ec.ucsg.analytics.application.dto.response.EventResponse;
import ec.ucsg.analytics.application.dto.response.EventSummaryResponse;
import ec.ucsg.analytics.application.dto.response.ZoneResponse;
import ec.ucsg.analytics.application.mapper.EventMapper;
import ec.ucsg.analytics.domain.enums.EventStatus;
import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.model.Event;
import ec.ucsg.analytics.domain.model.Zone;
import ec.ucsg.analytics.domain.repository.EventRepository;
import ec.ucsg.analytics.domain.repository.UserRepository;
import ec.ucsg.analytics.domain.repository.ZoneRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository  userRepository;
    private final ZoneRepository  zoneRepository;
    private final EventMapper     eventMapper;

    // ── Endpoints públicos (mapa) ────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> getApprovedForMap(Pageable pageable) {
        return eventRepository
            .findUpcomingApproved(LocalDateTime.now(), pageable)
            .map(eventMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public EventResponse getById(UUID id) {
        Event event = findEvent(id);
        if (event.getStatus() != EventStatus.APPROVED) {
            throw new EntityNotFoundException("Evento no encontrado: " + id);
        }
        return eventMapper.toResponse(event);
    }

    @Transactional(readOnly = true)
    public List<ZoneResponse> getAllZones() {
        return zoneRepository.findAll().stream()
            .map(this::toZoneResponse)
            .toList();
    }

    // ── Endpoints del administrador (todos los eventos) ─────────────

    /**
     * Lista paginada de todos los eventos (cualquier status).
     * Si se pasa status, filtra; si es null, devuelve todos.
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEventsForAdmin(EventStatus status, Pageable pageable) {
        Page<Event> page = (status != null)
            ? eventRepository.findByStatus(status, pageable)
            : eventRepository.findAll(pageable);
        return page.map(eventMapper::toResponse);
    }

    /** Aprueba un evento PENDING. */
    @Transactional
    public EventResponse approveEvent(UUID eventId, String adminEmail) {
        Event event = findEvent(eventId);
        AppUser admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + adminEmail));
        event.approve(admin);
        Event saved = eventRepository.save(event);
        log.info("Evento '{}' aprobado por {}", saved.getTitle(), adminEmail);
        return eventMapper.toResponse(saved);
    }

    /** Rechaza un evento PENDING con un motivo opcional. */
    @Transactional
    public EventResponse rejectEvent(UUID eventId, String reason, String adminEmail) {
        Event event = findEvent(eventId);
        AppUser admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + adminEmail));
        event.reject(admin, reason);
        Event saved = eventRepository.save(event);
        log.info("Evento '{}' rechazado por {}", saved.getTitle(), adminEmail);
        return eventMapper.toResponse(saved);
    }

    // ── Endpoints del supervisor (gestión de eventos publicados) ─────

    /** Lista paginada de eventos APROBADOS (publicados) para que el supervisor los gestione. */
    @Transactional(readOnly = true)
    public Page<EventResponse> getApprovedEventsForSupervisor(Pageable pageable) {
        return eventRepository
            .findByStatus(EventStatus.APPROVED, pageable)
            .map(eventMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getSupervisorEventById(UUID id) {
        return eventMapper.toResponse(findEvent(id));
    }

    /**
     * Edita un evento publicado (título, fecha, zona, texto de ubicación).
     * Solo se actualizan los campos no-nulos del request.
     */
    @Transactional
    public EventResponse updateEvent(UUID eventId, UpdateEventRequest request, String supervisorEmail) {
        Event event = findEvent(eventId);

        if (request.title() != null && !request.title().isBlank()) {
            event.setTitle(request.title().trim());
        }
        if (request.eventDate() != null) {
            event.setEventDate(request.eventDate());
        }
        if (request.zoneId() != null) {
            Zone zone = zoneRepository.findById(request.zoneId())
                .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada: " + request.zoneId()));
            event.setZone(zone);
            // Si no se envió locationText explícito, se sincroniza con el nombre de la zona
            if (request.locationText() == null) {
                event.setLocationText(zone.getName());
            }
        }
        if (request.locationText() != null) {
            event.setLocationText(request.locationText().trim());
        }

        Event saved = eventRepository.save(event);
        log.info("Evento '{}' editado por {}", saved.getTitle(), supervisorEmail);
        return eventMapper.toResponse(saved);
    }

    /** Elimina permanentemente un evento publicado y su carrusel de imágenes. */
    @Transactional
    public void deleteEvent(UUID eventId, String supervisorEmail) {
        Event event = findEvent(eventId);
        eventRepository.delete(event);
        log.info("Evento '{}' eliminado por {}", event.getTitle(), supervisorEmail);
    }

    // ── Privados ─────────────────────────────────────────────────────

    private Event findEvent(UUID id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Evento no encontrado: " + id));
    }

    private ZoneResponse toZoneResponse(Zone zone) {
        Double lat = zone.getLocation() != null ? zone.getLocation().getY() : null;
        Double lng = zone.getLocation() != null ? zone.getLocation().getX() : null;
        return new ZoneResponse(zone.getId(), zone.getName(), zone.getDescription(), lat, lng);
    }
}
