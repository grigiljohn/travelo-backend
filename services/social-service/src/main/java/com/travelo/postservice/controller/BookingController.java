package com.travelo.postservice.controller;

import com.travelo.postservice.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Controller for booking-related endpoints.
 * Returns mock data for development.
 * TODO: Replace with actual booking-service integration
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBookings(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(value = "status", required = false) String status) {
        logger.info("Getting bookings for user {} with status {}", userId, status);
        
        // Return mock data
        List<Map<String, Object>> bookings = getMockBookings(userId, status);
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", bookings));
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-User-Id") String userId) {
        logger.info("Getting booking {} for user {}", bookingId, userId);
        
        // Return mock data
        Map<String, Object> booking = getMockBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", booking));
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-User-Id") String userId) {
        logger.info("Cancelling booking {} for user {}", bookingId, userId);
        
        // TODO: Implement actual cancellation logic
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", null));
    }

    @PutMapping("/{bookingId}/modify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> modifyBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Object> request) {
        logger.info("Modifying booking {} for user {}", bookingId, userId);
        
        // TODO: Implement actual modification logic
        Map<String, Object> booking = getMockBooking(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Booking modified successfully", booking));
    }

    private List<Map<String, Object>> getMockBookings(String userId, String status) {
        List<Map<String, Object>> upcoming = new ArrayList<>(List.of(
            Map.of(
                "id", "booking-1",
                "destination", "Santorini Villa",
                "location", "Oia, Greece",
                "checkIn", "Dec 15, 2024",
                "checkOut", "Dec 20, 2024",
                "guests", "2 Adults",
                "total", "$1,495",
                "status", "upcoming"
            ),
            Map.of(
                "id", "booking-2",
                "destination", "Tokyo Hotel",
                "location", "Shibuya, Japan",
                "checkIn", "Jan 10, 2025",
                "checkOut", "Jan 17, 2025",
                "guests", "2 Adults, 1 Child",
                "total", "$2,100",
                "status", "upcoming"
            )
        ));

        List<Map<String, Object>> past = new ArrayList<>(List.of(
            Map.of(
                "id", "booking-3",
                "destination", "Paris Apartment",
                "location", "Montmartre, France",
                "checkIn", "Sep 5, 2024",
                "checkOut", "Sep 12, 2024",
                "guests", "2 Adults",
                "total", "$1,800",
                "status", "past"
            )
        ));

        List<Map<String, Object>> cancelled = new ArrayList<>();

        if (status == null || status.equals("all")) {
            return Stream.concat(
                    Stream.concat(upcoming.stream(), past.stream()),
                    cancelled.stream()
            ).collect(Collectors.toList());
        } else if (status.equals("upcoming")) {
            return upcoming;
        } else if (status.equals("past")) {
            return past;
        } else if (status.equals("cancelled")) {
            return cancelled;
        }

        return new ArrayList<>();
    }

    private Map<String, Object> getMockBooking(String bookingId) {
        return Map.of(
            "id", bookingId,
            "destination", "Santorini Villa",
            "location", "Oia, Greece",
            "checkIn", "Dec 15, 2024",
            "checkOut", "Dec 20, 2024",
            "guests", "2 Adults",
            "total", "$1,495",
            "status", "upcoming"
        );
    }
}

