package main.java.com.journalapp.util;

import main.java.com.journalapp.model.User;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages user accounts and saves.
 */
public class UserList {
    private static final String USER_LIST_FILE = "./data/users.csv";
    private static final String HEADER = "id,username,email,password\n";

    private static void createUserListFile() {
        new File(USER_LIST_FILE).getParentFile().mkdirs();
        try (FileWriter file = new FileWriter(USER_LIST_FILE, false)) {
            file.write(HEADER);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void write(ArrayList<User> users) {
        try (FileWriter writer = new FileWriter(USER_LIST_FILE, false)) {
            writer.write(HEADER);
            for (User user : users) {
                writer.write(user + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<User> read() {
        if (!Paths.get(USER_LIST_FILE).toFile().exists()) {
            createUserListFile();
        }

        ArrayList<User> users = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"([^\"]*)\",\"([^\"]*)\",\"([^\"]*)\",\"([^\"]*)\"");
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_LIST_FILE))) {
            String line;
            reader.readLine();  // discard header
            while ((line = reader.readLine()) != null) {
                Matcher matcher = pattern.matcher(line);
                matcher.matches();
                User user = new User(matcher.group(1),
                                     matcher.group(2),
                                     matcher.group(3),
                                     Cipher.decode(matcher.group(4)),
                                     null);
                users.add(user);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }
}
