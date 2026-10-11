package com.airwise.controller;

import com.airwise.entity.*;
import com.airwise.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DashboardService dashboardService;
    private final FlightService flightService;
    private final ComplaintService complaintService;
    private final TaxiDemandPredictionService taxiDemandPredictionService;
    private final TaxiBookingService taxiBookingService;
    private final WeatherDisruptionService weatherDisruptionService;
    private final AlertService alertService;
    private final AirportService airportService;

    public AdminController(DashboardService dashboardService,
                           FlightService flightService,
                           ComplaintService complaintService,
                           TaxiDemandPredictionService taxiDemandPredictionService,
                           TaxiBookingService taxiBookingService,
                           WeatherDisruptionService weatherDisruptionService,
                           AlertService alertService,
                           AirportService airportService) {
        this.dashboardService = dashboardService;
        this.flightService = flightService;
        this.complaintService = complaintService;
        this.taxiDemandPredictionService = taxiDemandPredictionService;
        this.taxiBookingService = taxiBookingService;
        this.weatherDisruptionService = weatherDisruptionService;
        this.alertService = alertService;
        this.airportService = airportService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> stats = dashboardService.getAdminDashboardStats();
        List<Flight> recentFlights = flightService.getRecentFlights().stream().limit(5).toList();
        List<Complaint> recentComplaints = complaintService.getAllComplaints().stream().limit(5).toList();
        List<Alert> alerts = alertService.getActiveAlerts();

        model.addAttribute("stats", stats);
        model.addAttribute("recentFlights", recentFlights);
        model.addAttribute("recentComplaints", recentComplaints);
        model.addAttribute("alerts", alerts);

        return "admin-dashboard";
    }

    @GetMapping("/flights")
    public String flightManagement(Model model) {
        model.addAttribute("flights", flightService.getAllFlights());
        model.addAttribute("airports", airportService.getAllAirports());
        model.addAttribute("statuses", FlightStatus.values());
        return "admin-flights";
    }

    @PostMapping("/flights/{id}/status")
    public String updateFlightStatus(@PathVariable("id") Long id,
                                     @RequestParam("status") String statusStr,
                                     @RequestParam(value = "delayMinutes", defaultValue = "0") int delayMinutes,
                                     @RequestParam(value = "confirmedReason", required = false) String confirmedReason,
                                     RedirectAttributes redirectAttributes) {
        try {
            FlightStatus status = FlightStatus.valueOf(statusStr);
            Flight updated = flightService.updateFlightStatus(id, status, delayMinutes, confirmedReason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Flight " + updated.getFlightNumber() + " updated to " + status + " (" + delayMinutes + " min delay). Connected taxi bookings updated!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating flight: " + e.getMessage());
        }
        return "redirect:/admin/flights";
    }

    @GetMapping("/complaints")
    public String complaintManagement(Model model) {
        model.addAttribute("complaints", complaintService.getAllComplaints());
        model.addAttribute("statuses", ComplaintStatus.values());
        return "admin-complaints";
    }

    @GetMapping("/complaints/{id}")
    public String reviewComplaint(@PathVariable("id") Long id, Model model) {
        Complaint complaint = complaintService.getComplaintById(id)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with ID: " + id));

        model.addAttribute("complaint", complaint);
        model.addAttribute("analysis", complaintService.getAnalysisForComplaint(complaint).orElse(null));
        model.addAttribute("statuses", ComplaintStatus.values());
        return "admin-complaint-detail";
    }

    @PostMapping("/complaints/{id}/status")
    public String updateComplaintStatus(@PathVariable("id") Long id,
                                        @RequestParam("status") String statusStr,
                                        @RequestParam(value = "department", required = false) String department,
                                        @RequestParam(value = "resolutionNotes", required = false) String resolutionNotes,
                                        RedirectAttributes redirectAttributes) {
        try {
            ComplaintStatus status = ComplaintStatus.valueOf(statusStr);
            Complaint updated = complaintService.updateComplaintStatus(id, status, department, resolutionNotes);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Complaint " + updated.getReferenceNo() + " status updated to " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating complaint: " + e.getMessage());
        }
        return "redirect:/admin/complaints/" + id;
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("totalFlights", flightService.countTotalFlights());
        model.addAttribute("delayedFlights", flightService.countDelayedFlights());
        model.addAttribute("onTimeFlights", flightService.countOnTimeFlights());
        model.addAttribute("cancelledFlights", flightService.countCancelledFlights());

        model.addAttribute("categoryStats", complaintService.getCategoryBreakdown());
        model.addAttribute("statusStats", complaintService.getStatusBreakdown());
        model.addAttribute("sentimentStats", complaintService.getSentimentBreakdown());
        model.addAttribute("departmentStats", complaintService.getDepartmentBreakdown());

        model.addAttribute("demandForecasts", taxiDemandPredictionService.getAllPredictions());
        model.addAttribute("totalBookings", taxiBookingService.countTotalBookings());
        model.addAttribute("activeBookings", taxiBookingService.countActiveBookings());

        return "admin-analytics";
    }

    @PostMapping("/alerts/{id}/dismiss")
    public String dismissAlert(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        alertService.dismissAlert(id);
        redirectAttributes.addFlashAttribute("successMessage", "Alert dismissed.");
        return "redirect:/admin/dashboard";
    }
}
