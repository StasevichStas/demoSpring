package com.example.reservations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {
    @Modifying
    @Query("""
            update ReservationEntity r
            set r.status = :status
            where r.id = :id
            """)
    void setStatus(@Param("id") Long id, @Param("status") ReservationStatus reservationStatus);

    @Query("""
            select r.id from ReservationEntity r
            where r.roomId = :roomId
            and :startDate < r.endDate
            and r.startDate < :endDate
            and r.status = :status
            """)
    List<Long> findConflictReservationIds(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") ReservationStatus status);

    @Query("""
            select r from ReservationEntity r
            where (:roomId is null or r.roomId = :roomId)
            and (:userId is null or r.userId = :userId)
            """)
    List<ReservationEntity> searchByFilter(
            @Param("roomId") Long roomId,
            @Param("userId") Long userId,
            Pageable pageable
    );
}
