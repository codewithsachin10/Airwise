import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class ComplaintNLPAnalyzer {
    public record Analysis(String category, String sentiment, String priority, String explanation) {
    }

    private static final Map<String, Set<String>> CATEGORY_TERMS = Map.of(
            "Flight Delay", Set.of("delay", "late", "cancel", "reschedule", "missed flight"),
            "Baggage", Set.of("baggage", "bag", "luggage", "suitcase", "lost"),
            "Security", Set.of("security", "screening", "checkpoint", "queue", "line"),
            "Cleanliness", Set.of("dirty", "clean", "restroom", "toilet", "hygiene"),
            "Staff Behavior", Set.of("staff", "rude", "employee", "service agent", "behavior"),
            "Food", Set.of("food", "meal", "restaurant", "drink", "coffee"),
            "Parking", Set.of("parking", "parked", "car park", "garage"),
            "Transportation", Set.of("taxi", "transport", "shuttle", "bus", "ride"));
    private static final Set<String> NEGATIVE = Set.of("not", "never", "hasn't", "isn't", "didn't", "lost", "late", "rude", "bad",
            "poor", "angry", "unhappy", "missing", "dirty", "wait", "delay", "terrible", "wrong");
    private static final Set<String> POSITIVE = Set.of("thank", "thanks", "good", "great", "helpful",
            "excellent", "clean", "friendly", "quick", "appreciate", "resolved");
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9']+");

    public Analysis analyze(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String category = "Other";
        int categoryScore = 0;
        for (Map.Entry<String, Set<String>> entry : CATEGORY_TERMS.entrySet()) {
            int score = score(normalized, entry.getValue());
            if (score > categoryScore) {
                category = entry.getKey();
                categoryScore = score;
            }
        }
        String[] tokens = TOKEN_SPLIT.split(normalized);
        int sentimentScore = 0;
        for (String token : tokens) {
            if (NEGATIVE.contains(token)) sentimentScore--;
            if (POSITIVE.contains(token)) sentimentScore++;
        }
        String sentiment = sentimentScore < 0 ? "Negative" : sentimentScore > 0 ? "Positive" : "Neutral";
        int urgency = (normalized.contains("urgent") || normalized.contains("emergency") ? 2 : 0)
                + (normalized.contains("two hours") || normalized.contains("hours") ? 1 : 0)
                + (sentiment.equals("Negative") ? 1 : 0);
        String priority = urgency >= 3 ? "Critical" : urgency >= 2 ? "High" : urgency == 1 ? "Medium" : "Low";
        return new Analysis(category, sentiment, priority,
                "Keyword matching and token scoring (demonstration rules; not a trained AI model).");
    }

    private static int score(String text, Set<String> terms) {
        return (int) terms.stream().filter(text::contains).count();
    }
}
