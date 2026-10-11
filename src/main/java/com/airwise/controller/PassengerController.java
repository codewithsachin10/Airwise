package com.airwise.controller;

import com.airwise.entity.Flight;
import com.airwise.entity.User;
import com.airwise.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/passenger")
public class PassengerController {

    private final DashboardService dashboardService;
    private final FlightService flightService;
    private final NotificationService notificationService;
    private final AlertService alertService;

    public PassengerController(DashboardService dashboardService,
                               FlightService flightService,
                               NotificationService notificationService,
                               AlertService alertService) {
        this.dashboardService = dashboardService;
        this.flightService = flightService;
        this.notificationService = notificationService;
        this.alertService = alertService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@ModelAttribute("currentUser") User currentUser, Model model) {
        Map<String, Object> stats = dashboardService.getPassengerDashboardStats(currentUser);
        List<Flight> upcomingFlights = flightService.getRecentFlights().stream().limit(2).toList();

        model.addAttribute("stats", stats);
        model.addAttribute("upcomingFlights", upcomingFlights);
        model.addAttribute("notifications", notificationService.getUserNotifications(currentUser).stream().limit(5).toList());
        model.addAttribute("activeAlerts", alertService.getRecentActiveAlerts());

        return "passenger-dashboard";
    }
}
