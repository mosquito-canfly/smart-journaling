package main.java.com.journalapp.util;

import main.java.com.journalapp.model.Entry;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Handles entry read and write operations.
 */
public class UserEntries {
    private static final String ENTRY_FOLDER = "./data/entries/";
    private static final String HEADER = "id,date,content,mood,weather\n";

    private final String path;
    private final ArrayList<Entry> entries = new ArrayList<>();

    public UserEntries(String id) {
        this.path = ENTRY_FOLDER + id + ".csv";

        if (!Files.exists(Paths.get(path))) {
            createEntriesFile();
        }
        else {
            loadEntriesFromFile();
        }
    }

    private void createEntriesFile() {
        new File(path).getParentFile().mkdirs();
        try (FileWriter file = new FileWriter(path)) {
            file.write(HEADER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveEntriesToFile() {
        try (FileWriter writer = new FileWriter(path, false)) {
            writer.write(HEADER);
            for (Entry entry : entries) {
                writer.write(entry.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadEntriesFromFile() {
        Pattern pattern = Pattern.compile("\"([^\"]*)\",\"([^\"]*)\",\"([^\"]*)\",\"([^\"]*)\",\"([^\"]*)\"");

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            reader.readLine();  // discard header
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);

                // handle multiline content
                while (!matcher.matches()) {
                    line += "\n" + reader.readLine();
                    matcher = pattern.matcher(line);
                }

                Entry entry = new Entry(matcher.group(1),
                        matcher.group(2),
                        matcher.group(3),
                        matcher.group(4),
                        matcher.group(5));
                this.entries.add(entry);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Entry> listEntries() {
        return this.entries;
    }

    public void createEntry(LocalDate date, String content, String mood, String weather) {
        String id = UUID.randomUUID().toString();
        Entry entry = new Entry(id, date, content, mood, weather);
        entries.add(entry);
        saveEntriesToFile();
    }

    public void deleteEntry(String id) {
        for (Entry entry : entries) {
            if (entry.getId().equals(id)) {
                entries.remove(entry);
                saveEntriesToFile();
                return;
            }
        }
    }

    public void editEntry(String id, LocalDate date, String content, String mood, String weather) {
        for (Entry entry : entries) {
            if (entry.getId().equals(id)) {
                entry.setDate(date);
                entry.setContent(content);
                entry.setMood(mood);
                entry.setWeather(weather);
                saveEntriesToFile();
                return;
            }
        }
    }
}
