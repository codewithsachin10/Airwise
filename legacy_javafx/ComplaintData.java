import java.time.LocalDateTime;

public record ComplaintData(String complaintId, String text, LocalDateTime timestamp) {
}
