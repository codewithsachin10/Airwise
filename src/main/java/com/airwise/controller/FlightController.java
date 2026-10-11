package com.airwise.controller;

import com.airwise.entity.Flight;
import com.airwise.entity.FlightPrediction;
import com.airwise.entity.WeatherRecord;
import com.airwise.service.AirportService;
import com.airwise.service.FlightDelayPredictionService;
import com.airwise.service.FlightService;
import com.airwise.service.WeatherDisruptionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class FlightController {

    private final FlightService flightService;
    private final AirportService airportService;
    private final FlightDelayPredictionService flightDelayPredictionService;
    private final WeatherDisruptionService weatherDisruptionService;

    public FlightController(FlightService flightService,
                            AirportService airportService,
                            FlightDelayPredictionService flightDelayPredictionService,
                            WeatherDisruptionService weatherDisruptionService) {
        this.flightService = flightService;
        this.airportService = airportService;
        this.flightDelayPredictionService = flightDelayPredictionService;
        this.weatherDisruptionService = weatherDisruptionService;
    }

    @GetMapping("/flights/search")
    public String searchFlights(
            @RequestParam(value = "flightNumber", required = false) String flightNumber,
            @RequestParam(value = "airline", required = false) String airline,
            @RequestParam(value = "depAirportId", required = false) Long depAirportId,
            @RequestParam(value = "arrAirportId", required = false) Long arrAirportId,
            @RequestParam(value = "travelDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate travelDate,
            Model model) {

        List<Flight> flights = flightService.searchFlights(flightNumber, airline, depAirportId, arrAirportId, travelDate);

        model.addAttribute("flights", flights);
        model.addAttribute("airports", airportService.getAllAirports());
        model.addAttribute("paramFlightNumber", flightNumber);
        model.addAttribute("paramAirline", airline);
        model.addAttribute("paramDepAirportId", depAirportId);
        model.addAttribute("paramArrAirportId", arrAirportId);
        model.addAttribute("paramTravelDate", travelDate);

        return "flight-search";
    }

    @GetMapping("/flights/{id}")
    public String flightDetails(@PathVariable("id") Long id, Model model) {
        Flight flight = flightService.getFlightById(id)
                .orElseThrow(() -> new IllegalArgumentException("Flight not found with ID: " + id));

        FlightPrediction prediction = flightDelayPredictionService.predictForFlight(flight);

        Optional<WeatherRecord> depWeather = weatherDisruptionService.getLatestForAirport(flight.getDepartureAirport());
        Optional<WeatherRecord> arrWeather = weatherDisruptionService.getLatestForAirport(flight.getArrivalAirport());

        model.addAttribute("flight", flight);
        model.addAttribute("prediction", prediction);
        model.addAttribute("depWeather", depWeather.orElse(null));
        model.addAttribute("arrWeather", arrWeather.orElse(null));

        return "flight-details";
    }

    @GetMapping("/weather")
    public String weatherDashboard(Model model) {
        model.addAttribute("weatherRecords", weatherDisruptionService.getAllWeatherRecords());
        model.addAttribute("airports", airportService.getAllAirports());
        return "weather-dashboard";
    }
}
