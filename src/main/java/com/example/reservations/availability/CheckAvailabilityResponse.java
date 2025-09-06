package com.example.reservations.availability;

public record CheckAvailabilityResponse(
        String message,
        AvailabilityStatus status
) {

}
