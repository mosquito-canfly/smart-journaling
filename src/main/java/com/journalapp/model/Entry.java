package main.java.com.journalapp.model;

import java.time.LocalDate;

/**
 *  Model representing a single entry
 */
public class Entry {
    private final String id;  // holds unique id for each entry
    private LocalDate date;
    private String content;
    private String mood;  // either "positive" or "negative"
    private String weather;

    public Entry(String id, LocalDate date, String content, String mood, String weather) {
        this.id = id;
        this.date = date;
        this.content = content;
        this.mood = mood;
        this.weather = weather;
    }

    public Entry(String id, String date, String content, String mood, String weather) {
        this(id, LocalDate.parse(date), content, mood, weather);
    }

    /* getters and setters... or should I just make the variables public? */
    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    @Override
    public String toString() {
        return this.toCSVRow();
    }

    private String toCSVRow() {
        final String quote = "\"";
        final String comma = ",";
        return quote + getId() + quote + comma +
               quote + getDate() + quote + comma +
               quote + getContent() + quote + comma +
               quote + getMood() + quote + comma +
               quote + getWeather() + quote;
    }
}
