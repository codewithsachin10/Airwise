package com.airwise.controller;

import com.airwise.entity.*;
import com.airwise.service.AirportService;
import com.airwise.service.ComplaintService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
public class ComplaintController {

    private final ComplaintService complaintService;
    private final AirportService airportService;

    public ComplaintController(ComplaintService complaintService, AirportService airportService) {
        this.complaintService = complaintService;
        this.airportService = airportService;
    }

    @GetMapping("/complaints/submit")
    public String submitForm(@RequestParam(value = "flightNumber", required = false) String flightNumber,
                             @RequestParam(value = "bookingRef", required = false) String bookingRef,
                             Model model) {
        model.addAttribute("airports", airportService.getAllAirports());
        model.addAttribute("paramFlightNumber", flightNumber);
        model.addAttribute("paramBookingRef", bookingRef);
        return "complaint-submit";
    }

    @PostMapping("/complaints/submit")
    public String processComplaint(@RequestParam("title") String title,
                                   @RequestParam("description") String description,
                                   @RequestParam(value = "category", required = false) String category,
                                   @RequestParam(value = "airportId", required = false) Long airportId,
                                   @RequestParam(value = "terminal", required = false) String terminal,
                                   @RequestParam(value = "flightNumber", required = false) String flightNumber,
                                   @RequestParam(value = "bookingReference", required = false) String bookingReference,
                                   @RequestParam(value = "preferredContact", defaultValue = "Email") String preferredContact,
                                   @RequestParam(value = "attachment", required = false) MultipartFile attachment,
                                   @ModelAttribute("currentUser") User currentUser,
                                   RedirectAttributes redirectAttributes) {
        try {
            Airport airport = null;
            if (airportId != null) {
                airport = airportService.getAirportById(airportId).orElse(null);
            }

            String attachmentName = (attachment != null && !attachment.isEmpty()) ?
                    attachment.getOriginalFilename() : null;

            Complaint complaint = complaintService.submitComplaint(
                    currentUser, title, description, category, airport, terminal,
                    flightNumber, bookingReference, preferredContact, attachmentName
            );

            redirectAttributes.addFlashAttribute("successMessage",
                    "Complaint submitted successfully! Reference: " + complaint.getReferenceNo());
            return "redirect:/complaints/view/" + complaint.getReferenceNo();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error submitting report: " + e.getMessage());
            return "redirect:/complaints/submit";
        }
    }

    @GetMapping("/complaints/track")
    public String trackComplaints(@RequestParam(value = "searchRef", required = false) String searchRef,
                                  @ModelAttribute("currentUser") User currentUser,
                                  Model model) {
        if (searchRef != null && !searchRef.isBlank()) {
            Optional<Complaint> found = complaintService.getComplaintByReference(searchRef.trim());
            if (found.isPresent()) {
                return "redirect:/complaints/view/" + found.get().getReferenceNo();
            } else {
                model.addAttribute("searchError", "No complaint record found for reference: " + searchRef);
            }
        }

        List<Complaint> complaints = complaintService.getUserComplaints(currentUser);
        model.addAttribute("complaints", complaints);
        return "complaint-track";
    }

    @GetMapping("/complaints/view/{ref}")
    public String viewComplaint(@PathVariable("ref") String ref,
                                @ModelAttribute("currentUser") User currentUser,
                                Model model) {
        Complaint complaint = complaintService.getComplaintByReference(ref)
                .orElseThrow(() -> new IllegalArgumentException("Complaint not found with reference: " + ref));

        // Security check: non-admin can only see own complaint
        if (currentUser.getRole() == Role.ROLE_PASSENGER && !complaint.getUser().getId().equals(currentUser.getId())) {
            return "redirect:/access-denied";
        }

        Optional<ComplaintAnalysis> analysis = complaintService.getAnalysisForComplaint(complaint);

        model.addAttribute("complaint", complaint);
        model.addAttribute("analysis", analysis.orElse(null));
        return "complaint-details";
    }
}
