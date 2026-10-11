package com.airwise.controller;

import com.airwise.entity.*;
import com.airwise.service.TaxiBookingService;
import com.airwise.service.TaxiDemandPredictionService;
import com.airwise.service.TaxiOperatorService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/operator")
public class TaxiOperatorController {

    private final TaxiOperatorService taxiOperatorService;
    private final TaxiBookingService taxiBookingService;
    private final TaxiDemandPredictionService taxiDemandPredictionService;

    public TaxiOperatorController(TaxiOperatorService taxiOperatorService,
                                  TaxiBookingService taxiBookingService,
                                  TaxiDemandPredictionService taxiDemandPredictionService) {
        this.taxiOperatorService = taxiOperatorService;
        this.taxiBookingService = taxiBookingService;
        this.taxiDemandPredictionService = taxiDemandPredictionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@ModelAttribute("currentUser") User currentUser, Model model) {
        Optional<TaxiOperator> operatorOpt = taxiOperatorService.getOperatorForUser(currentUser);
        TaxiOperator operator = operatorOpt.orElse(null);

        List<Taxi> taxis = operator != null ? taxiOperatorService.getTaxisForOperator(operator) : taxiOperatorService.getAllTaxis();
        List<TaxiBooking> bookings = operator != null ? taxiBookingService.getOperatorBookings(operator) : taxiBookingService.getAllBookings();
        List<TaxiDemandPrediction> demand = taxiDemandPredictionService.getAllPredictions();

        long availableCount = taxis.stream().filter(t -> t.getStatus() == TaxiStatus.AVAILABLE).count();
        long busyCount = taxis.stream().filter(t -> t.getStatus() == TaxiStatus.BUSY).count();

        model.addAttribute("operator", operator);
        model.addAttribute("taxis", taxis);
        model.addAttribute("bookings", bookings.stream().limit(8).toList());
        model.addAttribute("demandPredictions", demand);
        model.addAttribute("availableCount", availableCount);
        model.addAttribute("busyCount", busyCount);

        return "operator-dashboard";
    }

    @GetMapping("/taxis")
    public String taxisPage(@ModelAttribute("currentUser") User currentUser, Model model) {
        Optional<TaxiOperator> operatorOpt = taxiOperatorService.getOperatorForUser(currentUser);
        TaxiOperator operator = operatorOpt.orElse(null);
        List<Taxi> taxis = operator != null ? taxiOperatorService.getTaxisForOperator(operator) : taxiOperatorService.getAllTaxis();

        model.addAttribute("taxis", taxis);
        model.addAttribute("taxiTypes", TaxiType.values());
        model.addAttribute("taxiStatuses", TaxiStatus.values());
        return "operator-taxis";
    }

    @PostMapping("/taxis/add")
    public String addTaxi(@RequestParam("vehicleNumber") String vehicleNumber,
                          @RequestParam("model") String modelName,
                          @RequestParam("taxiType") String typeStr,
                          @RequestParam(value = "passengers", defaultValue = "4") int passengers,
                          @RequestParam(value = "luggage", defaultValue = "3") int luggage,
                          @RequestParam(value = "farePerKm", defaultValue = "25.0") double farePerKm,
                          @RequestParam(value = "terminal", defaultValue = "Terminal 2") String terminal,
                          @ModelAttribute("currentUser") User currentUser,
                          RedirectAttributes redirectAttributes) {
        try {
            TaxiOperator operator = taxiOperatorService.getOperatorForUser(currentUser)
                    .orElseThrow(() -> new IllegalStateException("Operator profile not configured"));

            TaxiType type = TaxiType.valueOf(typeStr);
            taxiOperatorService.registerTaxi(operator, vehicleNumber, modelName, type, passengers, luggage, farePerKm, terminal);
            redirectAttributes.addFlashAttribute("successMessage", "Vehicle " + vehicleNumber + " registered to fleet!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error registering vehicle: " + e.getMessage());
        }
        return "redirect:/operator/taxis";
    }

    @PostMapping("/taxis/{id}/status")
    public String updateTaxiStatus(@PathVariable("id") Long id,
                                   @RequestParam("status") String statusStr,
                                   @RequestParam(value = "terminal", required = false) String terminal,
                                   RedirectAttributes redirectAttributes) {
        try {
            TaxiStatus status = TaxiStatus.valueOf(statusStr);
            taxiOperatorService.updateTaxiStatus(id, status, terminal);
            redirectAttributes.addFlashAttribute("successMessage", "Taxi status updated to " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/operator/taxis";
    }

    @GetMapping("/bookings")
    public String bookingsPage(@ModelAttribute("currentUser") User currentUser, Model model) {
        Optional<TaxiOperator> operatorOpt = taxiOperatorService.getOperatorForUser(currentUser);
        TaxiOperator operator = operatorOpt.orElse(null);
        List<TaxiBooking> bookings = operator != null ? taxiBookingService.getOperatorBookings(operator) : taxiBookingService.getAllBookings();

        model.addAttribute("bookings", bookings);
        model.addAttribute("bookingStatuses", BookingStatus.values());
        return "operator-bookings";
    }

    @PostMapping("/bookings/{id}/status")
    public String updateBookingStatus(@PathVariable("id") Long id,
                                      @RequestParam("status") String statusStr,
                                      RedirectAttributes redirectAttributes) {
        try {
            BookingStatus status = BookingStatus.valueOf(statusStr);
            taxiBookingService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Booking status updated to " + status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating status: " + e.getMessage());
        }
        return "redirect:/operator/bookings";
    }
}
