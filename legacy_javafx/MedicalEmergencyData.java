import java.time.LocalDateTime;

public record MedicalEmergencyData(
        String emergencyId, String location, LocalDateTime timestamp, String emergencyType,
        int responseMinutes, boolean medicalTeamAvailable, boolean ambulanceAvailable, String outcome) {
}
