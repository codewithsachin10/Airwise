package com.airwise.controller;

import com.airwise.service.FlightService;
import com.airwise.service.WeatherDisruptionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final FlightService flightService;
    private final WeatherDisruptionService weatherDisruptionService;

    public HomeController(FlightService flightService, WeatherDisruptionService weatherDisruptionService) {
        this.flightService = flightService;
        this.weatherDisruptionService = weatherDisruptionService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("recentFlights", flightService.getRecentFlights().stream().limit(6).toList());
        model.addAttribute("weatherRecords", weatherDisruptionService.getAllWeatherRecords().stream().limit(4).toList());
        model.addAttribute("totalFlights", flightService.countTotalFlights());
        model.addAttribute("delayedFlights", flightService.countDelayedFlights());
        return "index";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
