package com.airwise.service;

import com.airwise.entity.*;
import com.airwise.repository.*;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {

    private final FlightRepository flightRepository;
    private final TaxiBookingRepository taxiBookingRepository;
    private final ComplaintRepository complaintRepository;
    private final AlertRepository alertRepository;
    private final TaxiRepository taxiRepository;
    private final WeatherRecordRepository weatherRecordRepository;
    private final PassengerFlowRecordRepository passengerFlowRecordRepository;

    public DashboardService(FlightRepository flightRepository,
                            TaxiBookingRepository taxiBookingRepository,
                            ComplaintRepository complaintRepository,
                            AlertRepository alertRepository,
                            TaxiRepository taxiRepository,
                            WeatherRecordRepository weatherRecordRepository,
                            PassengerFlowRecordRepository passengerFlowRecordRepository) {
        this.flightRepository = flightRepository;
        this.taxiBookingRepository = taxiBookingRepository;
        this.complaintRepository = complaintRepository;
        this.alertRepository = alertRepository;
        this.taxiRepository = taxiRepository;
        this.weatherRecordRepository = weatherRecordRepository;
        this.passengerFlowRecordRepository = passengerFlowRecordRepository;
    }

    public Map<String, Object> getAdminDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalFlights = flightRepository.count();
        long delayedFlights = flightRepository.countByStatus(FlightStatus.DELAYED);
        long onTimeFlights = flightRepository.countByStatus(FlightStatus.ON_TIME);
        long cancelledFlights = flightRepository.countByStatus(FlightStatus.CANCELLED);

        long totalBookings = taxiBookingRepository.count();
        long activeBookings = taxiBookingRepository.countByBookingStatus(BookingStatus.CONFIRMED) +
                              taxiBookingRepository.countByBookingStatus(BookingStatus.DRIVER_ASSIGNED);
        long availableTaxis = taxiRepository.countByStatus(TaxiStatus.AVAILABLE);

        long totalComplaints = complaintRepository.count();
        long openComplaints = complaintRepository.countByStatus(ComplaintStatus.SUBMITTED) +
                              complaintRepository.countByStatus(ComplaintStatus.UNDER_REVIEW) +
                              complaintRepository.countByStatus(ComplaintStatus.ASSIGNED) +
                              complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);
        long urgentComplaints = complaintRepository.countByPriority(ComplaintPriority.URGENT);

        long activeAlerts = alertRepository.countByActiveTrue();
        long criticalAlerts = alertRepository.countByActiveTrueAndSeverity(AlertSeverity.CRITICAL);

        stats.put("totalFlights", totalFlights);
        stats.put("delayedFlights", delayedFlights);
        stats.put("onTimeFlights", onTimeFlights);
        stats.put("cancelledFlights", cancelledFlights);
        stats.put("totalBookings", totalBookings);
        stats.put("activeBookings", activeBookings);
        stats.put("availableTaxis", availableTaxis);
        stats.put("totalComplaints", totalComplaints);
        stats.put("openComplaints", openComplaints);
        stats.put("urgentComplaints", urgentComplaints);
        stats.put("activeAlerts", activeAlerts);
        stats.put("criticalAlerts", criticalAlerts);

        return stats;
    }

    public Map<String, Object> getPassengerDashboardStats(User user) {
        Map<String, Object> stats = new HashMap<>();

        List<TaxiBooking> bookings = taxiBookingRepository.findByUserOrderByPickupTimeDesc(user);
        List<Complaint> complaints = complaintRepository.findByUserOrderByCreatedAtDesc(user);

        long activeTaxiBookings = bookings.stream()
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED ||
                             b.getBookingStatus() == BookingStatus.DRIVER_ASSIGNED ||
                             b.getBookingStatus() == BookingStatus.RESCHEDULE_REQUIRED)
                .count();

        long openComplaints = complaints.stream()
                .filter(c -> c.getStatus() != ComplaintStatus.RESOLVED && c.getStatus() != ComplaintStatus.CLOSED)
                .count();

        stats.put("bookingsCount", bookings.size());
        stats.put("activeBookingsCount", activeTaxiBookings);
        stats.put("complaintsCount", complaints.size());
        stats.put("openComplaintsCount", openComplaints);
        stats.put("recentBookings", bookings.stream().limit(3).toList());
        stats.put("recentComplaints", complaints.stream().limit(3).toList());

        return stats;
    }
}
