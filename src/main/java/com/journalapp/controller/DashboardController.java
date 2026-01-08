package main.java.com.journalapp.controller;

import main.java.com.journalapp.model.Entry;
import main.java.com.journalapp.util.Session;
import main.java.com.journalapp.util.Time;
import main.java.com.journalapp.util.Weather;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Tooltip;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

public class DashboardController {

    private Runnable onWriteNow;
    // Data Variables
    private int currentStreak = 0;
    private int totalEntriesCount = 0;
    private String weeklyMoodText = "No Data";
    private String weeklyMoodEmoji = "😐";

    // Maps to store valid data from CSV
    private Map<LocalDate, Boolean> last7DaysActivity = new HashMap<>();
    private Map<LocalDate, String> last7DaysWeather = new HashMap<>();

    private List<Entry> recentEntries = new ArrayList<>();
    private Map<LocalDate, String> moodHistory = new HashMap<>();
    private Set<Integer> availableYears = new HashSet<>();
    private int selectedYear = LocalDate.now().getYear();

    private GridPane graphGrid;
    private ComboBox<Integer> yearSelector;

    public void setOnWriteNow(Runnable action) {
        this.onWriteNow = action;
    }

    public VBox getView() {
        calculateRealData();

        VBox mainLayout = new VBox(30);
        mainLayout.setPadding(new Insets(40, 50, 40, 50));
        mainLayout.setAlignment(Pos.TOP_LEFT);
        mainLayout.setStyle("-fx-background-color: transparent;");

        HBox headerRow = new HBox();
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox greetingBox = new VBox(5);
        String currentUsername = (Session.getUsername() != null) ? Session.getUsername() : "Friend";
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM"));

        Label dateLabel = new Label(dateStr.toUpperCase());
        dateLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        dateLabel.setTextFill(Color.web("#7f8c8d"));

        String timePeriod = Time.getPeriodOfDay();
        String greetingText = "Good " + timePeriod + ", " + currentUsername + ".";
        Label greetingLabel = new Label(greetingText);
        greetingLabel.setFont(Font.font("Georgia", FontWeight.NORMAL, 32));
        greetingLabel.setTextFill(Color.web("#2d3436"));

        greetingBox.getChildren().addAll(dateLabel, greetingLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button writeNowBtn = new Button("+ Write New Entry");
        styleButton(writeNowBtn); // Helper method for clean code

        writeNowBtn.setOnAction(e -> {
            if (onWriteNow != null) onWriteNow.run();
        });

        headerRow.getChildren().addAll(greetingBox, spacer, writeNowBtn);

        HBox statsContainer = new HBox(20);
        statsContainer.setAlignment(Pos.CENTER_LEFT);
        statsContainer.getChildren().addAll(
                createStreakCard(),
                createMoodCard(),
                createTotalCard(),
                createWeatherCard()
        );

        VBox graphSection = new VBox(15);
        HBox graphHeader = new HBox(15);
        graphHeader.setAlignment(Pos.CENTER_LEFT);
        Label graphTitle = new Label("Mood Activity");
        graphTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        graphTitle.setTextFill(Color.web("#2d3436"));
        Region graphSpacer = new Region();
        HBox.setHgrow(graphSpacer, Priority.ALWAYS);

        yearSelector = new ComboBox<>();
        yearSelector.setStyle("-fx-font-size: 14px; -fx-background-color: white; -fx-border-color: #dfe6e9; -fx-border-radius: 5;");
        updateYearSelectorItems();
        yearSelector.setValue(selectedYear);

        yearSelector.setOnAction(e -> {
            if (yearSelector.getValue() != null) {
                selectedYear = yearSelector.getValue();
                drawCalendarGraph(selectedYear);
            }
        });

        graphHeader.getChildren().addAll(graphTitle, graphSpacer, new Label("Year: "), yearSelector);
        VBox moodGraphContainer = createMoodGraph();
        drawCalendarGraph(selectedYear);

        graphSection.getChildren().addAll(graphHeader, moodGraphContainer);

        mainLayout.getChildren().addAll(headerRow, statsContainer, graphSection);

        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox root = new VBox(scrollPane);
        root.setStyle("-fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        return root;
    }

    private void calculateRealData() {
        // Reset
        currentStreak = 0;
        totalEntriesCount = 0;
        weeklyMoodText = "No Data";
        weeklyMoodEmoji = "😐";
        last7DaysActivity.clear();
        recentEntries.clear();
        last7DaysWeather.clear();
        moodHistory.clear();
        availableYears.clear();
        availableYears.add(LocalDate.now().getYear());

        if (!Session.hasActiveUser()) return;
        ArrayList<Entry> entries = Session.listEntries();
        if (entries == null || entries.isEmpty()) return;

        entries.sort((e1, e2) -> e2.getDate().compareTo(e1.getDate()));
        totalEntriesCount = entries.size();
        recentEntries = entries.stream().limit(3).collect(Collectors.toList());

        List<LocalDate> allDates = new ArrayList<>();
        List<String> recentMoods = new ArrayList<>();

        for (Entry e : entries) {
            LocalDate d = e.getDate();
            allDates.add(d);
            moodHistory.put(d, e.getMood());
            availableYears.add(d.getYear());

            last7DaysActivity.put(d, true);

            if (e.getWeather() != null && !e.getWeather().trim().isEmpty()) {
                last7DaysWeather.put(d, e.getWeather());
            }

            if (d.isAfter(LocalDate.now().minusDays(7))) {
                recentMoods.add(e.getMood());
            }
        }
        if (yearSelector != null) updateYearSelectorItems();
        calculateStreak(allDates);
        analyzeMoods(recentMoods);
    }

    private void calculateStreak(List<LocalDate> dates) {
        if (dates.isEmpty()) {
            currentStreak = 0; return;
        }
        List<LocalDate> uniqueDates = dates.stream()
                .distinct()
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        currentStreak = 0;
        LocalDate checkDate = LocalDate.now();

        if (uniqueDates.contains(checkDate)) {
        }
        else if (uniqueDates.contains(checkDate.minusDays(1))) {
            checkDate = checkDate.minusDays(1);
        }
        else {
            currentStreak = 0; return;
        }
        // Count backwards
        while (uniqueDates.contains(checkDate)) {
            currentStreak++;
            checkDate = checkDate.minusDays(1);
        }
    }

    private void analyzeMoods(List<String> moods) {
        if (moods.isEmpty()) {
            weeklyMoodText = "Neutral"; weeklyMoodEmoji = "😐"; return;
        }
        Map<String, Integer> freq = new HashMap<>();
        for (String m : moods) freq.put(m, freq.getOrDefault(m, 0) + 1);
        String mostCommon = Collections.max(freq.entrySet(), Map.Entry.comparingByValue()).getKey();

        weeklyMoodText = mostCommon;
        String lower = mostCommon.toLowerCase();
        if(lower.contains("very positive")) weeklyMoodEmoji = "😁";
        else if(lower.contains("positive")) weeklyMoodEmoji = "😊";
        else if(lower.contains("negative")) weeklyMoodEmoji = "😔";
        else weeklyMoodEmoji = "😐";
    }

    // UI
    private VBox createStreakCard() {
        VBox card = createBaseCard();
        card.setPrefWidth(260);

        HBox header = new HBox();
        Label title = new Label("Writing Streak");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#b2bec3"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label count = new Label(currentStreak + " Days");
        count.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        count.setTextFill(Color.web("#e67e22"));
        header.getChildren().addAll(title, spacer, count);

        Label icon = new Label("🔥");
        icon.setStyle("-fx-font-family: 'Segoe UI Emoji'; -fx-font-size: 24px;");
        HBox iconRow = new HBox(icon);
        iconRow.setAlignment(Pos.CENTER);

        HBox dotsBox = new HBox(12);
        dotsBox.setAlignment(Pos.CENTER);

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        for (int i = 0; i < 7; i++) {
            LocalDate dateToCheck = startOfWeek.plusDays(i);

            VBox dayCol = new VBox(5);
            dayCol.setAlignment(Pos.CENTER);
            Circle dot = new Circle(6);

            if (dateToCheck.isAfter(today)) {
                dot.setFill(Color.TRANSPARENT);
                dot.setStroke(Color.web("#dfe6e9"));
            }
            else if (last7DaysActivity.containsKey(dateToCheck)) {
                dot.setFill(Color.web("#2ecc71")); // Green
            }
            else {
                dot.setFill(Color.web("#dfe6e9")); // Grey
            }

            String dayLetter = dateToCheck.format(DateTimeFormatter.ofPattern("E")).substring(0, 1);
            Label letterLbl = new Label(dayLetter);
            letterLbl.setFont(Font.font("Segoe UI", 10));
            letterLbl.setTextFill(Color.web("#b2bec3"));
            dayCol.getChildren().addAll(dot, letterLbl);
            dotsBox.getChildren().add(dayCol);
        }
        card.getChildren().addAll(header, iconRow, dotsBox);
        return card;
    }

    private VBox createWeatherCard() {
        VBox card = createBaseCard();
        card.setPrefWidth(480);

        Label title = new Label("Weather Summary");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#b2bec3"));

        HBox daysContainer = new HBox(5);
        daysContainer.setAlignment(Pos.CENTER);
        daysContainer.setPadding(new Insets(15, 0, 0, 0));

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        for (int i = 0; i < 7; i++) {
            LocalDate dateToCheck = startOfWeek.plusDays(i);
            String weather = "";

            if (dateToCheck.isAfter(today)) {
                weather = "";
            } else if (last7DaysWeather.containsKey(dateToCheck)) {
                weather = last7DaysWeather.get(dateToCheck);
            } else if (dateToCheck.equals(today)) {
                weather = Weather.getCurrentWeather();
            }

            VBox dayCol = new VBox(0);
            dayCol.setAlignment(Pos.CENTER);
            dayCol.setMinWidth(60);
            dayCol.setMaxWidth(60);

            // Icon Setup
            String iconChar = "";
            if (weather != null && !weather.isEmpty()) {
                iconChar = getWeatherIcon(weather);
            }
            Label iconLbl = new Label(iconChar);

            iconLbl.setTextOverrun(javafx.scene.control.OverrunStyle.CLIP);
            iconLbl.setMaxWidth(Double.MAX_VALUE);
            iconLbl.setAlignment(Pos.CENTER);
            iconLbl.setMinHeight(35);
            iconLbl.setMaxHeight(35);
            iconLbl.setStyle("-fx-font-family: 'Segoe UI Emoji', 'Arial'; -fx-font-size: 24px; -fx-text-fill: black;");

            String dayName = dateToCheck.format(DateTimeFormatter.ofPattern("E"));
            Label dayLbl = new Label(dayName);
            dayLbl.setMaxWidth(Double.MAX_VALUE);
            dayLbl.setAlignment(Pos.CENTER);
            dayLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 10));
            dayLbl.setTextFill(Color.web("#636e72"));

            dayCol.getChildren().addAll(iconLbl, dayLbl);
            if (!iconChar.isEmpty() && !iconChar.equals("❓")) {
                Tooltip.install(dayCol, new Tooltip(dateToCheck.toString() + ": " + weather));
            }
            daysContainer.getChildren().add(dayCol);
        }
        card.getChildren().addAll(title, daysContainer);
        return card;
    }

    private String getWeatherIcon(String weather) {
        if (weather == null || weather.trim().isEmpty()) return "❓";
        String w = weather.trim().toLowerCase();
        if (w.contains("sunny")) return "☀️";
        if (w.contains("hazy")) return "🌫️";
        if (w.contains("rain")) return "🌧️";
        if (w.contains("thunderstorms")) return "⛈️";
        if (w.contains("cloud")) return "☁️";
        return "❓";
    }

    private VBox createMoodCard() {
        VBox card = createBaseCard();
        card.setPrefWidth(200);

        Label title = new Label("Weekly Vibe");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#b2bec3"));

        Label icon = new Label(weeklyMoodEmoji);
        icon.setStyle("-fx-font-family: 'Segoe UI Emoji', 'Arial'; -fx-font-size: 48px; -fx-text-fill: black;");
        icon.setPadding(new Insets(10,0,10,0));
        icon.setAlignment(Pos.CENTER);

        Label text = new Label(weeklyMoodText);
        text.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));

        String lowerMood = weeklyMoodText.toLowerCase();
        if (lowerMood.contains("positive")) text.setTextFill(Color.web("#27ae60"));
        else if (lowerMood.contains("negative")) text.setTextFill(Color.web("#e74c3c"));
        else text.setTextFill(Color.web("#f39c12"));

        card.getChildren().addAll(title, icon, text);
        return card;
    }

    private VBox createTotalCard() {
        VBox card = createBaseCard();
        card.setPrefWidth(200);

        Label title = new Label("Total Entries");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        title.setTextFill(Color.web("#b2bec3"));

        Label count = new Label(String.valueOf(totalEntriesCount));
        count.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36));
        count.setTextFill(Color.web("#0984e3"));

        Label sub = new Label("Lifetime memories");
        sub.setFont(Font.font("Segoe UI", 10));
        sub.setTextFill(Color.GRAY);

        card.getChildren().addAll(title, count, sub);
        return card;
    }

    private VBox createMoodGraph() {
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        container.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5);");

        container.setMinHeight(250);
        HBox graphWrapper = new HBox(10);
        graphWrapper.setAlignment(Pos.TOP_LEFT);

        RowConstraints headerRowConstraint = new RowConstraints(15);
        headerRowConstraint.setValignment(javafx.geometry.VPos.BOTTOM);
        RowConstraints dayRowConstraint = new RowConstraints(12);
        dayRowConstraint.setValignment(javafx.geometry.VPos.CENTER);

        GridPane labelsGrid = new GridPane();
        labelsGrid.setVgap(7);
        labelsGrid.setPadding(new Insets(0, 5, 0, 0));
        labelsGrid.setMinWidth(30);

        labelsGrid.getRowConstraints().add(headerRowConstraint);
        for (int i = 0; i < 7; i++) labelsGrid.getRowConstraints().add(dayRowConstraint);

        // Spacer
        Label spacer = new Label("Jan");
        spacer.setVisible(false);
        labelsGrid.add(spacer, 0, 0);

        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(days[i]);
            lbl.setFont(Font.font("Segoe UI", 10));
            lbl.setTextFill(Color.web("#b2bec3"));
            lbl.setMaxHeight(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER_LEFT);
            labelsGrid.add(lbl, 0, i + 1);
        }

        graphGrid = new GridPane();
        graphGrid.setHgap(7);
        graphGrid.setVgap(7);
        graphGrid.setAlignment(Pos.TOP_LEFT);
        graphGrid.setPadding(new Insets(0, 0, 20, 0));

        graphGrid.getRowConstraints().add(headerRowConstraint);
        for (int i = 0; i < 7; i++) graphGrid.getRowConstraints().add(dayRowConstraint);

        ScrollPane gridScrollPane = new ScrollPane(graphGrid);
        gridScrollPane.setFitToHeight(true);
        gridScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gridScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        gridScrollPane.setMinHeight(160);
        gridScrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        HBox.setHgrow(labelsGrid, Priority.NEVER);
        HBox.setHgrow(gridScrollPane, Priority.ALWAYS);

        graphWrapper.getChildren().addAll(labelsGrid, gridScrollPane);
        container.getChildren().addAll(graphWrapper, createLegend());
        return container;
    }

    private void drawCalendarGraph(int year) {
        if (graphGrid == null) return;
        graphGrid.getChildren().clear();

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        int column = 0;
        LocalDate cursor = start;
        java.time.Month currentMonth = null;

        while (!cursor.isAfter(end)) {
            int dayOfWeek = cursor.getDayOfWeek().getValue();
            // Java DayOfWeek: Mon=1...Sun=7.
            // We want Sun=0, Mon=1...Sat=6 for this specific grid
            int row = (dayOfWeek == 7) ? 0 : dayOfWeek;

            if (cursor.getMonth() != currentMonth) {
                currentMonth = cursor.getMonth();
                if (column < 51) {
                    Label monthLabel = new Label(currentMonth.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH));
                    monthLabel.setFont(Font.font("Segoe UI", 10));
                    monthLabel.setTextFill(Color.GRAY);
                    graphGrid.add(monthLabel, column, 0);
                    GridPane.setColumnSpan(monthLabel, 2);
                }
            }

            Rectangle rect = new Rectangle(12, 12);
            rect.setArcWidth(3); rect.setArcHeight(3);

            String mood = moodHistory.getOrDefault(cursor, "No Data");
            rect.setFill(getColorForMood(mood));

            Tooltip.install(rect, new Tooltip(cursor.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + "\n" + mood));
            graphGrid.add(rect, column, row + 1);
            if (row == 6) { // If Saturday, new column
                column++;
            }
            cursor = cursor.plusDays(1);
        }
    }

    private VBox createBaseCard() {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefHeight(140);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15;");
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.1));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        card.setEffect(shadow);
        return card;
    }

    private void updateYearSelectorItems() {
        if (yearSelector == null) return;
        List<Integer> sortedYears = new ArrayList<>(availableYears);
        sortedYears.sort(Collections.reverseOrder());
        yearSelector.setItems(FXCollections.observableArrayList(sortedYears));
    }

    private Color getColorForMood(String mood) {
        if (mood == null || mood.equals("No Data")) return Color.web("#ebedf0");
        String lower = mood.toLowerCase();
        if (lower.contains("very positive")) return Color.web("#2ecc71");
        else if (lower.contains("very negative")) return Color.web("#e74c3c");
        else if (lower.contains("positive")) return Color.web("#3498db");
        else if (lower.contains("negative")) return Color.web("#fd79a8");
        else if (lower.contains("neutral")) return Color.web("#f1c40f");
        return Color.web("#ebedf0");
    }

    private HBox createLegend() {
        HBox legend = new HBox(15);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.setPadding(new Insets(10, 0, 0, 0));
        legend.getChildren().addAll(
                createLegendItem("Very +ve", "#2ecc71"),
                createLegendItem("Positive", "#3498db"),
                createLegendItem("Neutral", "#f1c40f"),
                createLegendItem("Negative", "#fd79a8"),
                createLegendItem("Very -ve", "#e74c3c"),
                createLegendItem("No Data", "#ebedf0")
        );
        return legend;
    }

    private HBox createLegendItem(String text, String colorHex) {
        HBox item = new HBox(5);
        item.setAlignment(Pos.CENTER);
        Rectangle box = new Rectangle(10, 10, Color.web(colorHex));
        box.setArcWidth(2); box.setArcHeight(2);
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", 10));
        lbl.setTextFill(Color.GRAY);
        item.getChildren().addAll(box, lbl);
        return item;
    }

    private void styleButton(Button btn) {
        String defaultStyle = "-fx-background-color: #2d3436; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20 10 20; -fx-background-radius: 30; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: #636e72; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 20 10 20; -fx-background-radius: 30; -fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 3);";
        btn.setStyle(defaultStyle);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(defaultStyle));
    }
}