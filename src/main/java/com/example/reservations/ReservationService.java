package com.example.reservations;

import com.example.reservations.availability.ReservationAvailabilityService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
//import java.awt.print.Pageable;
import java.time.LocalDate;
import java.util.List;


@Service
public class ReservationService {
    private final ReservationRepository repository;
    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
    private final ReservationMapper mapper;
    private final ReservationAvailabilityService availabilityService;

    public ReservationService(ReservationRepository repository, ReservationMapper mapper, ReservationAvailabilityService availabilityService) {
        this.repository = repository;
        this.mapper = mapper;
        this.availabilityService = availabilityService;
    }

    public Reservation getReservationById(Long id) {
        ReservationEntity reservationEntity = repository.findById(id).orElseThrow(() -> new EntityNotFoundException("not found by id = " + id));
        return mapper.toDomain(reservationEntity);
    }

    public List<Reservation> searchAllByFilter(ReservationSearchFilter filter) {
        int pageSize = filter.pageSize() != null ? filter.pageSize() : 2;
        int pageNumber = filter.pageNumber() != null ? filter.pageNumber() : 0;
        Pageable pageable = Pageable.ofSize(pageSize).withPage(pageNumber);
        List<ReservationEntity> allEntities = repository.searchByFilter(
                filter.roomId(),
                filter.userId(),
                pageable);
        return allEntities.stream().map(
                id -> mapper.toDomain(id)
        ).toList();
    }

    public Reservation createReservation(Reservation reservationToCreate) {
        if (reservationToCreate.status() != null) {
            throw new IllegalStateException("status should be empty");
        }
        checkCorrectDate(reservationToCreate);
        ReservationEntity newReservation = mapper.toEntity(reservationToCreate);
        newReservation.setStatus(ReservationStatus.PENDING);
        repository.save(newReservation);
        return mapper.toDomain(newReservation);
    }

    public Reservation updateReservation(Long id, Reservation reservationToUpdate) {
        var reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("not found reservation by id = " + id));
        if (reservationEntity.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Incorrect reservation status" + reservationEntity.getStatus());
        }
        checkCorrectDate(reservationToUpdate);
        var updateReservation = mapper.toEntity(reservationToUpdate);
        updateReservation.setId(reservationEntity.getId());
        updateReservation.setStatus(ReservationStatus.PENDING);
        return mapper.toDomain(repository.save(updateReservation));
    }

    @Transactional
    public void cancelReservation(Long id) {
        var reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("not found reservation by id = " + id));
        if (!reservationEntity.getStatus().equals(ReservationStatus.PENDING)) {
            throw new IllegalStateException("You can't cancel it, it's already been reserved or canceled, contact our managers");
        }
        repository.setStatus(id, ReservationStatus.CANCELLED);
        log.info("Successfully cancelled status id={}", id);
    }

    public Reservation approvereservation(Long id) {
        var reservationEntity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("not found reservation by id = " + id));
        if (reservationEntity.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Incorrect reservation status" + reservationEntity.getStatus());
        }
        if (!availabilityService.isReservationAvailable(
                reservationEntity.getRoomId(),
                reservationEntity.getStartDate(),
                reservationEntity.getEndDate())) {
            throw new IllegalStateException("date conflict");
        }
        reservationEntity.setStatus(ReservationStatus.APPROVED);
        return mapper.toDomain(repository.save(reservationEntity));

    }


    private void checkCorrectDate(Reservation reservation) {
        if (!reservation.endDate().isAfter(reservation.startDate())) {
            throw new IllegalArgumentException("startDate must be 1 day earlier than endDate");
        }
    }
}

