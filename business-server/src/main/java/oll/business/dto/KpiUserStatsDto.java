package oll.business.dto;

import java.util.List;

public class KpiUserStatsDto {
    private Long userId;
    private String username;
    private Double rating;
    private Integer weeklyCompleted;
    private Double loadPercent;
    private List<RatingHistoryPoint> ratingHistory;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public Integer getWeeklyCompleted() { return weeklyCompleted; }
    public void setWeeklyCompleted(Integer weeklyCompleted) { this.weeklyCompleted = weeklyCompleted; }
    public Double getLoadPercent() { return loadPercent; }
    public void setLoadPercent(Double loadPercent) { this.loadPercent = loadPercent; }
    public List<RatingHistoryPoint> getRatingHistory() { return ratingHistory; }
    public void setRatingHistory(List<RatingHistoryPoint> ratingHistory) { this.ratingHistory = ratingHistory; }

    public static class RatingHistoryPoint {
        private String date;
        private Double rating;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }
    }
}
