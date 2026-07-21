package ec.ucsg.analytics.application.service;

import ec.ucsg.analytics.application.dto.request.CreateZoneRequest;
import ec.ucsg.analytics.application.dto.request.UpdateEventRequest;
import ec.ucsg.analytics.application.dto.request.UpdateZoneRequest;
import ec.ucsg.analytics.application.dto.response.AuditLogResponse;
import ec.ucsg.analytics.application.dto.response.EventResponse;
import ec.ucsg.analytics.application.dto.response.EventSummaryResponse;
import ec.ucsg.analytics.application.dto.response.PageResponse;
import ec.ucsg.analytics.application.dto.response.ZoneRematchResponse;
import ec.ucsg.analytics.application.dto.response.ZoneResponse;
import ec.ucsg.analytics.application.mapper.EventMapper;
import ec.ucsg.analytics.domain.enums.AuditAction;
import ec.ucsg.analytics.domain.enums.EventStatus;
import ec.ucsg.analytics.domain.model.AppUser;
import ec.ucsg.analytics.domain.model.AuditLog;
import ec.ucsg.analytics.domain.model.Event;
import ec.ucsg.analytics.domain.model.Reminder;
import ec.ucsg.analytics.domain.model.Zone;
import ec.ucsg.analytics.domain.repository.AuditLogRepository;
import ec.ucsg.analytics.domain.repository.EventRepository;
import ec.ucsg.analytics.domain.repository.ReminderRepository;
import ec.ucsg.analytics.domain.repository.UserRepository;
import ec.ucsg.analytics.domain.repository.ZoneRepository;
import ec.ucsg.analytics.infrastructure.mail.EmailNotificationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
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

    private final EventRepository            eventRepository;
    private final UserRepository             userRepository;
    private final ZoneRepository             zoneRepository;
    private final EventMapper                eventMapper;
    private final AuditLogRepository         auditLogRepository;
    private final EventApprovalNotifier      eventApprovalNotifier;
    private final ZoneMatchingService        zoneMatchingService;
    private final ReminderRepository         reminderRepository;
    private final EmailNotificationService   emailService;

    // ── Endpoints públicos (mapa) ────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<EventSummaryResponse> getApprovedForMap(Pageable pageable) {
        return eventRepository
            .findUpcomingApproved(pageable)
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

    /**
     * Crea una nueva ubicación (zona) del campus — usada desde "Editar evento"
     * cuando el supervisor/admin necesita agregar un lugar que todavía no
     * existe. Aparece de inmediato en el selector de zonas y en el footer
     * público (ambos leen /api/public/zones en vivo).
     */
    @Transactional
    public ZoneResponse createZone(CreateZoneRequest request) {
        String name = request.name().trim();
        if (zoneRepository.findByNameIgnoreCase(name).isPresent()) {
            throw new IllegalStateException("Ya existe una ubicación con ese nombre: " + name);
        }

        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Coordinate coordinate = new Coordinate(request.longitude(), request.latitude());

        Zone zone = Zone.builder()
            .name(name)
            .location(geometryFactory.createPoint(coordinate))
            .build();

        Zone saved = zoneRepository.save(zone);
        log.info("Nueva ubicación creada: '{}' ({}, {})", name, request.latitude(), request.longitude());
        return toZoneResponse(saved);
    }

    /**
     * Renombra una ubicación (zona) existente — usada desde el panel de
     * administrador para corregir/actualizar el nombre mostrado sin tocar
     * las coordenadas ni los eventos ya asignados a esa zona.
     */
    @Transactional
    public ZoneResponse updateZoneName(Long id, UpdateZoneRequest request) {
        Zone zone = zoneRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Zona no encontrada: " + id));

        String newName = request.name().trim();
        zoneRepository.findByNameIgnoreCase(newName).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalStateException("Ya existe una ubicación con ese nombre: " + newName);
            }
        });

        zone.setName(newName);
        Zone saved = zoneRepository.save(zone);
        log.info("Ubicación #{} renombrada a '{}'", id, newName);
        return toZoneResponse(saved);
    }

    /**
     * Re-emparejar eventos sin zona contra el caption ya guardado —
     * necesario después de reemplazar/reconstruir el catálogo de zonas
     * (ej. V12__rebuild_zones_from_docx.sql), ya que el DELETE de las zonas
     * anteriores deja zone_id en null en cualquier evento que las usara
     * (ON DELETE SET NULL). No vuelve a llamar a Instagram — solo reprocesa
     * el texto que ya está en la base de datos contra las zonas actuales.
     */
    @Transactional
    public ZoneRematchResponse rematchZones() {
        List<Event> candidates = eventRepository.findAll().stream()
            .filter(e -> e.getZone() == null && e.getCaption() != null && !e.getCaption().isBlank())
            .toList();

        int matched = 0;
        for (Event event : candidates) {
            var zone = zoneMatchingService.findBestMatch(event.getCaption());
            if (zone.isPresent()) {
                event.setZone(zone.get());
                if (event.getLocationText() == null || event.getLocationText().isBlank()) {
                    event.setLocationText(zone.get().getName());
                }
                eventRepository.save(event);
                matched++;
            }
        }

        log.info("Re-matching de zonas: {} de {} eventos sin zona ahora tienen una asignada", matched, candidates.size());
        return new ZoneRematchResponse(matched, candidates.size());
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
        recordAudit(saved, admin, AuditAction.APPROVED);
        log.info("Evento '{}' aprobado por {}", saved.getTitle(), adminEmail);

        // Forzar carga LAZY de zona/imágenes dentro de la transacción activa —
        // notifyEventApproved() corre en un hilo @Async sin sesión de Hibernate,
        // así que cualquier proxy sin inicializar explota con LazyInitializationException.
        if (saved.getZone() != null) saved.getZone().getName();
        saved.getImages().size();

        // Notificación en segundo plano — un fallo de envío no debe revertir la
        // aprobación, que ya quedó confirmada en las líneas anteriores.
        eventApprovalNotifier.notifyEventApproved(saved);

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
        recordAudit(saved, admin, AuditAction.REJECTED);
        log.info("Evento '{}' rechazado por {}", saved.getTitle(), adminEmail);
        return eventMapper.toResponse(saved);
    }

    /** Registra en audit_logs el timestamp, la acción, el supervisor/admin y el evento. */
    private void recordAudit(Event event, AppUser supervisor, AuditAction action) {
        auditLogRepository.save(
            AuditLog.builder()
                .event(event)
                .eventTitleSnapshot(event.getTitle())  // preserva el título si el evento es borrado después
                .supervisor(supervisor)
                .action(action)
                .build()
        );
    }

    // ── Endpoints del supervisor (gestión de eventos) ───────────────────

    /**
     * Lista paginada de eventos para revisión del supervisor.
     * Si status es null, devuelve TODOS los estados (necesario para que el
     * supervisor vea los PENDING y pueda aprobarlos/rechazarlos); si viene
     * un status, filtra igual que el panel de admin.
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getAllEventsForSupervisor(EventStatus status, Pageable pageable) {
        Page<Event> page = (status != null)
            ? eventRepository.findByStatus(status, pageable)
            : eventRepository.findAll(pageable);
        return page.map(eventMapper::toResponse);
    }

    /** Lista paginada de eventos APROBADOS (publicados) para gestión del supervisor. */
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
     * Registra auditoría EDITED y notifica a usuarios con recordatorio activo.
     */
    @Transactional
    public EventResponse updateEvent(UUID eventId, UpdateEventRequest request, String supervisorEmail) {
        Event event = findEvent(eventId);
        AppUser supervisor = userRepository.findByEmail(supervisorEmail)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + supervisorEmail));

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
        recordAudit(saved, supervisor, AuditAction.EDITED);
        log.info("Evento '{}' editado por {}", saved.getTitle(), supervisorEmail);

        // Forzar carga LAZY antes del contexto @Async
        if (saved.getZone() != null) saved.getZone().getName();
        saved.getImages().size();

        // Notificar a usuarios con recordatorio activo (asíncrono — no bloquea la respuesta)
        List<Reminder> reminders = reminderRepository.findByEventId(eventId);
        for (Reminder r : reminders) {
            emailService.sendEventEditedNotification(r.getUser(), saved);
        }
        log.info("Notificados {} usuarios del evento editado '{}'", reminders.size(), saved.getTitle());

        return eventMapper.toResponse(saved);
    }

    /**
     * Elimina permanentemente un evento publicado y su carrusel de imágenes.
     * Recoge usuarios con recordatorio ANTES de borrar (cascade limpia los reminders).
     * Registra auditoría DELETED y notifica a los usuarios afectados.
     */
    @Transactional
    public void deleteEvent(UUID eventId, String supervisorEmail) {
        Event event = findEvent(eventId);
        AppUser supervisor = userRepository.findByEmail(supervisorEmail)
            .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado: " + supervisorEmail));

        // Recoger usuarios ANTES del delete — el cascade borrará los reminders
        List<Reminder> reminders = reminderRepository.findByEventId(eventId);
        String title = event.getTitle();

        recordAudit(event, supervisor, AuditAction.DELETED);
        eventRepository.delete(event);
        log.info("Evento '{}' eliminado por {}", title, supervisorEmail);

        // Notificar a los usuarios afectados (asíncrono)
        for (Reminder r : reminders) {
            emailService.sendEventDeletedNotification(r.getUser(), title);
        }
        log.info("Notificados {} usuarios del evento eliminado '{}'", reminders.size(), title);
    }

    /** Número de recordatorios activos de un evento (Feature 3 — supervisor). */
    @Transactional(readOnly = true)
    public long getReminderCount(UUID eventId) {
        return reminderRepository.countByEventId(eventId);
    }

    /**
     * Historial paginado de ediciones y eliminaciones — solo para ADMIN (Feature 2).
     * Devuelve registros con acción EDITED o DELETED, ordenados del más reciente al más antiguo.
     */
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> getEditAuditLogs(Pageable pageable) {
        Page<AuditLogResponse> page = auditLogRepository
            .findByActionInOrderByCreatedAtDesc(
                List.of(AuditAction.EDITED, AuditAction.DELETED),
                pageable
            )
            .map(AuditLogResponse::from);
        return PageResponse.of(page);
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
