package com.goservi.booking.service;

import com.goservi.booking.dto.BookingDtos;

import java.util.List;

public interface BookingService {
    BookingDtos.BookingResponse create(Long clientId, BookingDtos.BookingRequest req);
    BookingDtos.BookingResponse getById(String id);
    List<BookingDtos.BookingResponse> getByClient(Long clientId);
    List<BookingDtos.BookingResponse> getByProfessional(Long professionalId);
    BookingDtos.BookingResponse accept(String id, Long professionalId);
    BookingDtos.BookingResponse reject(String id, Long professionalId);
    BookingDtos.BookingResponse verify(String id, BookingDtos.VerifyRequest req, Long professionalId);
    BookingDtos.BookingResponse complete(String id, Long professionalId);
    BookingDtos.BookingResponse cancel(String id, Long userId);

    /** Llamado por el profesional desde la app — valida que sea el asignado */
    BookingDtos.BookingResponse markPaid(String id, Long professionalId);

    /** Llamado internamente por el webhook de Wompi — sin validación de profesional */
    BookingDtos.BookingResponse markPaidInternal(String id);

    /** Profesional notifica que llegó al destino — envía notificación WS al cliente */
    BookingDtos.BookingResponse arrive(String id, Long professionalId);
}