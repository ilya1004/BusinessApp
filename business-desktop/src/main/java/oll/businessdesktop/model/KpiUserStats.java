package oll.businessdesktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KpiUserStats(
    Long userId,
    String username,
    Double rating,
    Integer weeklyCompleted,
    Double loadPercent,
    List<RatingHistoryPoint> ratingHistory
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RatingHistoryPoint(
        String date,
        Double rating
    ) {}
}
