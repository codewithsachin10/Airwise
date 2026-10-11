package com.airwise.controller;

import com.airwise.entity.*;
import com.airwise.service.AirportService;
import com.airwise.service.FlightService;
import com.airwise.service.TaxiBookingService;
import com.airwise.service.TaxiDemandPredictionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class TaxiController {

    private final TaxiBookingService taxiBookingService;
    private final TaxiDemandPredictionService taxiDemandPredictionService;
    private final FlightService flightService;
    private final AirportService airportService;

    public TaxiController(TaxiBookingService taxiBookingService,
                          TaxiDemandPredictionService taxiDemandPredictionService,
                          FlightService flightService,
                          AirportService airportService) {
        this.taxiBookingService = taxiBookingService;
        this.taxiDemandPredictionService = taxiDemandPredictionService;
        this.flightService = flightService;
        this.airportService = airportService;
    }

    @GetMapping("/taxis/demand")
    public String demandDashboard(Model model) {
        List<TaxiDemandPrediction> predictions = taxiDemandPredictionService.getAllPredictions();
        model.addAttribute("predictions", predictions);
        return "taxi-demand";
    }

    @GetMapping("/taxis/book")
    public String bookTaxiPage(@RequestParam(value = "flightId", required = false) Long flightId,
                               Model model) {
        Flight flight = null;
        LocalDateTime suggestedPickup = LocalDateTime.now().plusHours(1);

        if (flightId != null) {
            Optional<Flight> fOpt = flightService.getFlightById(flightId);
            if (fOpt.isPresent()) {
                flight = fOpt.get();
                // Add 30-min baggage & deplaning buffer
                suggestedPickup = flight.getEstimatedArrival().plusMinutes(30);
            }
        }

        model.addAttribute("flight", flight);
        model.addAttribute("suggestedPickup", suggestedPickup);
        model.addAttribute("taxiTypes", TaxiType.values());
        model.addAttribute("airports", airportService.getAllAirports());
        return "taxi-book";
    }

    @PostMapping("/taxis/book")
    public String submitBooking(@RequestParam(value = "flightId", required = false) Long flightId,
                                @RequestParam("taxiType") String taxiTypeStr,
                                @RequestParam("terminal") String terminal,
                                @RequestParam("destination") String destination,
                                @RequestParam(value = "passengers", defaultValue = "1") int passengers,
                                @RequestParam(value = "luggage", defaultValue = "1") int luggage,
                                @RequestParam("pickupTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime pickupTime,
                                @RequestParam(value = "notes", required = false) String notes,
                                @ModelAttribute("currentUser") User currentUser,
                                RedirectAttributes redirectAttributes) {
        try {
            TaxiType taxiType = TaxiType.valueOf(taxiTypeStr);
            TaxiBooking booking = taxiBookingService.createBooking(
                    currentUser, flightId, taxiType, terminal, destination, passengers, luggage, pickupTime, notes
            );
            redirectAttributes.addFlashAttribute("successMessage",
                    "Taxi advance reservation confirmed! Reference: " + booking.getBookingReference());
            return "redirect:/taxis/bookings/" + booking.getBookingReference();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/taxis/book" + (flightId != null ? "?flightId=" + flightId : "");
        }
    }

    @GetMapping("/taxis/bookings")
    public String userBookings(@ModelAttribute("currentUser") User currentUser, Model model) {
        List<TaxiBooking> bookings = taxiBookingService.getUserBookings(currentUser);
        model.addAttribute("bookings", bookings);
        return "taxi-history";
    }

    @GetMapping("/taxis/bookings/{ref}")
    public String bookingConfirmation(@PathVariable("ref") String ref,
                                      @ModelAttribute("currentUser") User currentUser,
                                      Model model) {
        TaxiBooking booking = taxiBookingService.getBookingByReference(ref)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + ref));

        model.addAttribute("booking", booking);
        return "taxi-confirmation";
    }

    @PostMapping("/taxis/bookings/{id}/reschedule")
    public String rescheduleBooking(@PathVariable("id") Long id,
                                    @RequestParam("newPickupTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime newPickupTime,
                                    RedirectAttributes redirectAttributes) {
        try {
            TaxiBooking booking = taxiBookingService.rescheduleBooking(id, newPickupTime);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Pickup slot rescheduled successfully to " + newPickupTime + "!");
            return "redirect:/taxis/bookings/" + booking.getBookingReference();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/taxis/bookings";
        }
    }

    @PostMapping("/taxis/bookings/{id}/cancel")
    public String cancelBooking(@PathVariable("id") Long id,
                                @ModelAttribute("currentUser") User currentUser,
                                RedirectAttributes redirectAttributes) {
        try {
            taxiBookingService.cancelBooking(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Booking cancelled successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/taxis/bookings";
    }
}
