package main.java.com.journalapp.model;

import main.java.com.journalapp.util.Cipher;
import main.java.com.journalapp.util.UserEntries;

/**
 *  Model representing a single user.
 */
public class User {
    private final String id;
    private final String username;
    private final String email;
    private final String password;
    private final UserEntries entries;

    public User(String id, String username, String email, String password, UserEntries entries) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.entries = entries != null ? entries : new UserEntries(id);
    }

    /* Getter methods */
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public UserEntries getEntries() {
        return entries;
    }

    /* Override comparison methods to compare only id */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        User user = (User) o;
        return this.id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.toCSVRow();
    }

    private String toCSVRow() {
        final String quote = "\"";
        final String comma = ",";
        return quote + getId() + quote + comma +
               quote + getUsername() + quote + comma +
               quote + getEmail() + quote + comma +
               quote + Cipher.encode(getPassword()) + quote;
    }
}
