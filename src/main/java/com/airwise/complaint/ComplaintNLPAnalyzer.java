package com.airwise.complaint;

import com.airwise.entity.ComplaintPriority;
import com.airwise.entity.Sentiment;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class ComplaintNLPAnalyzer {

    public record AnalysisOutput(
            String category,
            Sentiment sentiment,
            ComplaintPriority priority,
            String extractedKeywords,
            String summaryReason,
            String recommendedDepartment,
            boolean isUrgentSafety
    ) {}

    private static final Map<String, Set<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_DEPARTMENTS = new LinkedHashMap<>();

    private static final Set<String> SAFETY_CRITICAL_KEYWORDS = Set.of(
            "fire", "smoke", "injury", "injured", "medical", "blood", "assault", "hazard", "threat",
            "emergency", "danger", "dangerous", "unconscious", "collapse", "stolen", "weapon"
    );

    private static final Set<String> URGENCY_KEYWORDS = Set.of(
            "urgent", "immediately", "emergency", "asap", "critical", "hours waiting", "stranded",
            "missed connection", "stuck", "two hours", "three hours"
    );

    private static final Set<String> NEGATIVE_LEXICON = Set.of(
            "delay", "delayed", "late", "cancelled", "cancel", "lost", "broken", "damaged", "dirty",
            "disgusting", "rude", "horrible", "terrible", "worst", "unacceptable", "angry", "frustrated",
            "poor", "bad", "slow", "incompetent", "negligent", "smelly", "stolen", "missing", "unhelpful",
            "chaos", "crowded", "waste", "failed", "unhappy", "pain", "shouting", "complaint"
    );

    private static final Set<String> POSITIVE_LEXICON = Set.of(
            "thank", "thanks", "helpful", "good", "great", "excellent", "kind", "prompt", "appreciate",
            "resolved", "polite", "quick", "clean", "wonderful", "satisfied", "courteous", "smooth"
    );

    private static final Pattern TOKEN_REGEX = Pattern.compile("[^a-zA-Z0-9']+");

    static {
        CATEGORY_KEYWORDS.put("Safety Concerns", Set.of(
                "safety", "hazard", "danger", "dangerous", "fire", "smoke", "slip", "fall", "injury", "medical",
                "first aid", "security risk", "threat", "emergency", "spill", "wiring", "broken glass"
        ));
        CATEGORY_KEYWORDS.put("Accessibility", Set.of(
                "wheelchair", "disabled", "disability", "accessibility", "elderly", "special assistance",
                "ramp", "elevator", "lift", "blind", "deaf", "hearing", "mobility", "escort"
        ));
        CATEGORY_KEYWORDS.put("Flight Delay", Set.of(
                "flight", "delay", "delayed", "reschedule", "gate change", "boarding", "takeoff",
                "airline", "pilot", "missed flight", "connection", "cancelled", "overbooked"
        ));
        CATEGORY_KEYWORDS.put("Baggage Problems", Set.of(
                "bag", "bags", "baggage", "luggage", "suitcase", "carousel", "lost bag", "damaged bag",
                "missing luggage", "tag", "belt", "lost property", "trolley"
        ));
        CATEGORY_KEYWORDS.put("Taxi or Transportation", Set.of(
                "taxi", "cab", "uber", "driver", "transport", "shuttle", "bus", "fare", "overcharging",
                "pickup", "ride", "dropoff", "meter", "parking", "car park"
        ));
        CATEGORY_KEYWORDS.put("Security", Set.of(
                "security", "screening", "tsa", "checkpoint", "frisking", "metal detector", "scanner",
                "pat down", "immigration", "passport control", "customs", "queue line"
        ));
        CATEGORY_KEYWORDS.put("Cleanliness", Set.of(
                "clean", "dirty", "restroom", "washroom", "toilet", "hygiene", "trash", "garbage",
                "smell", "dust", "sanitizer", "foul", "filthy", "water on floor"
        ));
        CATEGORY_KEYWORDS.put("Staff Behavior", Set.of(
                "staff", "agent", "employee", "officer", "rude", "attitude", "shouting", "unprofessional",
                "behavior", "ignored", "refused", "counter staff", "ground staff"
        ));
        CATEGORY_KEYWORDS.put("Food Services", Set.of(
                "food", "restaurant", "cafe", "coffee", "meal", "water", "price", "expensive",
                "expired", "stale", "dining", "snack", "hygiene food", "lounge food"
        ));

        CATEGORY_DEPARTMENTS.put("Safety Concerns", "Airport Safety & Emergency Operations");
        CATEGORY_DEPARTMENTS.put("Accessibility", "Passenger Special Assistance Services");
        CATEGORY_DEPARTMENTS.put("Flight Delay", "Flight Operations & Terminal Dispatch");
        CATEGORY_DEPARTMENTS.put("Baggage Problems", "Baggage Handling & Lost Property");
        CATEGORY_DEPARTMENTS.put("Taxi or Transportation", "Landside Ground Transportation");
        CATEGORY_DEPARTMENTS.put("Security", "Aviation Security & Screening Division");
        CATEGORY_DEPARTMENTS.put("Cleanliness", "Terminal Facilities & Janitorial Services");
        CATEGORY_DEPARTMENTS.put("Staff Behavior", "Customer Experience & Staffing Relations");
        CATEGORY_DEPARTMENTS.put("Food Services", "Terminal Concessions & Food Services");
        CATEGORY_DEPARTMENTS.put("Other", "General Customer Relations");
    }

    public AnalysisOutput analyze(String title, String description, String userCategory) {
        String fullText = ((title != null ? title : "") + " " + (description != null ? description : "")).toLowerCase(Locale.ROOT);
        String[] tokens = TOKEN_REGEX.split(fullText);

        List<String> matchedKeywords = new ArrayList<>();
        boolean isSafety = false;
        boolean hasUrgency = false;

        // Check safety critical terms
        for (String word : SAFETY_CRITICAL_KEYWORDS) {
            if (fullText.contains(word)) {
                isSafety = true;
                matchedKeywords.add(word);
            }
        }

        // Check urgency terms
        for (String word : URGENCY_KEYWORDS) {
            if (fullText.contains(word)) {
                hasUrgency = true;
                if (!matchedKeywords.contains(word)) matchedKeywords.add(word);
            }
        }

        // Determine Category
        String detectedCategory = "Other";
        int bestCategoryScore = 0;

        for (Map.Entry<String, Set<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            for (String term : entry.getValue()) {
                if (fullText.contains(term)) {
                    score++;
                    if (!matchedKeywords.contains(term) && matchedKeywords.size() < 10) {
                        matchedKeywords.add(term);
                    }
                }
            }
            if (score > bestCategoryScore) {
                bestCategoryScore = score;
                detectedCategory = entry.getKey();
            }
        }

        // If user explicitly provided a category and detection score was weak, respect user's selection
        if (userCategory != null && !userCategory.isBlank() && !"Other".equalsIgnoreCase(userCategory) && bestCategoryScore < 2) {
            detectedCategory = userCategory;
        }

        // Calculate Sentiment
        int sentimentScore = 0;
        for (String token : tokens) {
            if (NEGATIVE_LEXICON.contains(token)) sentimentScore--;
            if (POSITIVE_LEXICON.contains(token)) sentimentScore++;
        }

        Sentiment sentiment;
        if (sentimentScore < 0) {
            sentiment = Sentiment.NEGATIVE;
        } else if (sentimentScore > 0) {
            sentiment = Sentiment.POSITIVE;
        } else {
            sentiment = Sentiment.NEUTRAL;
        }

        // Determine Priority
        ComplaintPriority priority;
        if (isSafety) {
            priority = ComplaintPriority.URGENT;
        } else if (hasUrgency || (sentiment == Sentiment.NEGATIVE && sentimentScore <= -3)) {
            priority = ComplaintPriority.HIGH;
        } else if (sentiment == Sentiment.NEGATIVE || hasUrgency) {
            priority = ComplaintPriority.MEDIUM;
        } else {
            priority = ComplaintPriority.LOW;
        }

        String recommendedDept = CATEGORY_DEPARTMENTS.getOrDefault(detectedCategory, "General Customer Relations");

        String summaryReason = String.format(
                "Classified under '%s' based on %d matched lexicon terms (keywords: %s). Sentiment evaluated as %s (score: %d). Priority set to %s.",
                detectedCategory,
                matchedKeywords.size(),
                String.join(", ", matchedKeywords),
                sentiment,
                sentimentScore,
                priority
        );

        String extractedKeywordStr = String.join(", ", matchedKeywords);
        if (extractedKeywordStr.isBlank()) {
            extractedKeywordStr = "general, feedback";
        }

        return new AnalysisOutput(
                detectedCategory,
                sentiment,
                priority,
                extractedKeywordStr,
                summaryReason,
                recommendedDept,
                isSafety
        );
    }
}
