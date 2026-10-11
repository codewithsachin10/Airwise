import java.util.ArrayList;
import java.util.List;

public class PredictionEngine {
    public record Prediction(double probability, int expectedMinutes, String risk, List<String> factors) {
        public Prediction {
            factors = List.copyOf(factors);
        }
    }

    public record WeatherPrediction(double probability, String risk, String impact) {
    }

    public record CrowdPrediction(int expectedPassengers, String level, int securityQueueMinutes,
                                  int checkInQueueMinutes, int extraLanes) {
    }

    public record TaxiPrediction(int demand, int shortage, int waitMinutes, String risk) {
    }

    public record MedicalPrediction(String risk, int requiredStaff, int requiredAmbulances) {
    }

    public Prediction predictFlight(int previousDelayMinutes, int turnaroundMinutes, int passengerCount,
                                    double weatherRisk, boolean peakHour) {
        List<String> factors = new ArrayList<>();
        double score = 0.08;
        if (previousDelayMinutes > 0) {
            score += Math.min(previousDelayMinutes / 180.0, 0.28);
            factors.add("Previous delay: +" + previousDelayMinutes + " min");
        }
        if (turnaroundMinutes > 50) {
            score += Math.min((turnaroundMinutes - 50) / 180.0, 0.20);
            factors.add("Long aircraft turnaround");
        }
        if (passengerCount > 220) {
            score += Math.min((passengerCount - 220) / 1800.0, 0.12);
            factors.add("High passenger load");
        }
        double boundedWeather = clamp(weatherRisk, 0, 1);
        score += boundedWeather * 0.30;
        if (boundedWeather >= 0.35) factors.add("Weather risk elevated");
        if (peakHour) {
            score += 0.12;
            factors.add("Peak-hour departure");
        }
        double probability = clamp(score, 0.03, 0.97);
        return new Prediction(probability, (int) Math.round(8 + probability * 52),
                riskFor(probability), factors.isEmpty() ? List.of("No major risk factors identified") : factors);
    }

    public WeatherPrediction predictWeather(double rainfallMm, double windSpeedKph, double visibilityKm,
                                            boolean thunderstorm) {
        return predictWeather(rainfallMm, windSpeedKph, visibilityKm, 22, thunderstorm);
    }

    public WeatherPrediction predictWeather(double rainfallMm, double windSpeedKph, double visibilityKm,
                                            double temperatureC, boolean thunderstorm) {
        double score = Math.min(Math.max(rainfallMm, 0) / 40.0, 0.28)
                + Math.min(Math.max(windSpeedKph, 0) / 150.0, 0.25)
                + Math.max(0, (8 - visibilityKm) / 8.0) * 0.32
                + (temperatureC < 0 || temperatureC > 40 ? 0.06 : 0)
                + (thunderstorm ? 0.30 : 0);
        double probability = clamp(score, 0.02, 0.98);
        String impact = probability >= 0.7 ? "Flight delays and possible cancellations"
                : probability >= 0.4 ? "Schedule padding and ground-operation monitoring"
                : "Limited operational impact expected";
        return new WeatherPrediction(probability, riskFor(probability), impact);
    }

    public CrowdPrediction predictCrowd(int passengerCount, int arrivals, int departures, boolean peakHour,
                                        boolean holiday, int activeCounters, int securityLanes) {
        int expected = Math.max(0, passengerCount + Math.max(0, arrivals + departures) * 18
                + (peakHour ? 420 : 0) + (holiday ? 300 : 0));
        double load = expected / (double) Math.max(1, activeCounters * 75 + securityLanes * 120);
        String level = load >= 4.5 ? "CRITICAL" : load >= 3 ? "HIGH" : load >= 1.8 ? "MODERATE" : "LOW";
        int securityQueue = Math.max(3, (int) Math.round(expected / (double) Math.max(1, securityLanes * 150)));
        int checkInQueue = Math.max(2, (int) Math.round(expected / (double) Math.max(1, activeCounters * 115)));
        int extraLanes = Math.max(0, (securityQueue - 20 + 9) / 10);
        return new CrowdPrediction(expected, level, securityQueue, checkInQueue, extraLanes);
    }

    public TaxiPrediction predictTaxi(int arrivals, int passengers, boolean peakHour, boolean holiday,
                                      String weatherCondition, int availableTaxis) {
        double weatherFactor = "Rain".equalsIgnoreCase(weatherCondition) ? 1.15
                : "Storm".equalsIgnoreCase(weatherCondition) ? 1.25 : 1.0;
        int demand = (int) Math.ceil(Math.max(0, (arrivals * 12 + passengers * 0.42
                + (peakHour ? 120 : 0) + (holiday ? 90 : 0)) * weatherFactor));
        int shortage = Math.max(0, demand - Math.max(0, availableTaxis));
        int waitMinutes = shortage == 0 ? 5 : 5 + (int) Math.ceil(shortage / 30.0);
        return new TaxiPrediction(demand, shortage, waitMinutes,
                shortage > 250 ? "HIGH" : shortage > 0 ? "MODERATE" : "LOW");
    }

    public MedicalPrediction predictMedical(int recentIncidents, boolean peakHour, boolean teamAvailable,
                                            boolean ambulanceAvailable) {
        int requiredStaff = Math.max(2, (int) Math.ceil(recentIncidents / 8.0) + (peakHour ? 1 : 0));
        int requiredAmbulances = Math.max(1, (int) Math.ceil(recentIncidents / 15.0));
        String risk = (!teamAvailable || !ambulanceAvailable || recentIncidents >= 12) ? "HIGH"
                : recentIncidents >= 6 ? "MODERATE" : "LOW";
        return new MedicalPrediction(risk, requiredStaff, requiredAmbulances);
    }

    public static String riskFor(double probability) {
        return probability >= 0.75 ? "HIGH" : probability >= 0.45 ? "MODERATE" : "LOW";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
