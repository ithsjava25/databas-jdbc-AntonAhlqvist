package com.example;

import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    static void main(String[] args) {
        if (isDevMode(args)) {
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    /**
     * Kör huvudflödet för applikationen, inklusive inloggning och enkel meny för att lista månfärder
     * och skapa nya konton.
     * <p>
     * Skapar en Scanner för användarinput.
     * Frågar användaren efter användarnamn och lösenord och kontrollerar dessa mot databasen.
     * Om inloggning lyckas skrivs "Login successful!" ut, annars avslutas programmet med felmeddelande.
     * Efter lyckad inloggning visas en enkel meny med följande alternativ:
     * <p>
     * 1) Lista alla månfärder (namn på rymdfarkoster)
     * 2) Hämta månfärd efter mission_id
     * 3) Räkna månfärder för ett visst år
     * 4) Skapa ett nytt konto (förnamn, efternamn, SSN, lösenord)
     * 5) Uppdatera lösenord för ett konto via user_id
     * 0) Avsluta programmet
     * <p>
     * Valet "Lista månfärder" hämtar alla rymdfarkoster från tabellen "moon_mission" och skriver ut dem i ordning efter mission_id.
     * Valet "Hämta månfärd efter mission_id" skriver ut information om den specifika månfärden om den finns.
     * Valet "Räkna månfärder för ett visst år" skriver ut antalet månfärder för det angivna året.
     * Valet "Skapa nytt konto" frågar efter namn, SSN och lösenord och sparar informationen i tabellen "account".
     * Valet "Uppdatera lösenord" frågar efter user_id och nytt lösenord och uppdaterar kontots lösenord i tabellen "account".
     * Valet "Avsluta" bryter menyn och avslutar metoden.
     * Ogiltiga val hanteras med ett felmeddelande.
     */
    public void run() {
        // Resolve DB settings with precedence: System properties -> Environment variables
        String jdbcUrl = resolveConfig("APP_JDBC_URL", "APP_JDBC_URL");
        String dbUser = resolveConfig("APP_DB_USER", "APP_DB_USER");
        String dbPass = resolveConfig("APP_DB_PASS", "APP_DB_PASS");

        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                    "Missing DB configuration. Provide APP_JDBC_URL, APP_DB_USER, APP_DB_PASS " +
                            "as system properties (-Dkey=value) or environment variables.");
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {

            Scanner scanner = new Scanner(System.in);

            System.out.println("Username:");
            String username = scanner.nextLine().trim();

            System.out.println("Password:");
            String password = scanner.nextLine().trim();

            boolean authenticated = false;
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM account WHERE name = ? AND password = ?"
            )) {
                ps.setString(1, username);
                ps.setString(2, password);

                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    authenticated = true;
                }
            }

            if (!authenticated) {
                System.out.println("Invalid username or password");
                return;
            }

            System.out.println("Login successful!");

            while (true) {
                System.out.println("1) List moon missions");
                System.out.println("2) Get mission by ID");
                System.out.println("3) Count missions for year");
                System.out.println("4) Create new account");
                System.out.println("5) Update account password");
                System.out.println("0) Exit");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "0" -> { break; }
                    case "1" -> {
                        try (PreparedStatement ps = connection.prepareStatement(
                                "SELECT spacecraft FROM moon_mission ORDER BY mission_id"
                        )) {
                            ResultSet rs = ps.executeQuery();
                            while (rs.next()) {
                                System.out.println(rs.getString("spacecraft"));
                            }
                        }
                    }
                    case "2" -> {
                        System.out.println("Mission ID:");
                        int id = Integer.parseInt(scanner.nextLine().trim());

                        try (PreparedStatement ps = connection.prepareStatement(
                                "SELECT * FROM moon_mission WHERE mission_id = ?"
                        )) {
                            ps.setInt(1, id);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {

                                System.out.println(rs.getString("spacecraft"));
                            } else {
                                System.out.println("Mission not found");
                            }
                        }
                    }

                    case "3" -> {
                        System.out.println("Year:");
                        String yearStr = scanner.nextLine().trim();
                        System.out.println(yearStr);
                        int year = Integer.parseInt(scanner.nextLine().trim());

                        try (PreparedStatement ps = connection.prepareStatement(
                                "SELECT COUNT(*) FROM moon_mission WHERE year = ?"
                        )) {
                            ps.setInt(1, year);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) {
                                System.out.println(rs.getInt(1));
                            }
                        }
                    }

                    case "4" -> {
                        System.out.println("First name:");
                        String firstName = scanner.nextLine().trim();
                        System.out.println("Last name:");
                        String lastName = scanner.nextLine().trim();
                        System.out.println("SSN:");
                        String ssn = scanner.nextLine().trim();
                        System.out.println("Password:");
                        String accountPassword = scanner.nextLine().trim();

                        try (PreparedStatement ps = connection.prepareStatement(
                                "INSERT INTO account(password, first_name, last_name, ssn) VALUES (?, ?, ?, ?)"
                        )) {
                            ps.setString(1, accountPassword);
                            ps.setString(2, firstName);
                            ps.setString(3, lastName);
                            ps.setString(4, ssn);
                            ps.executeUpdate();
                        }

                        System.out.println("Account created");
                    }

                    case "5" -> {
                        System.out.println("User ID of account to update:");
                        long userId = Long.parseLong(scanner.nextLine().trim());
                        System.out.println("New password:");
                        String newPassword = scanner.nextLine().trim();

                        try (PreparedStatement ps = connection.prepareStatement(
                                "UPDATE account SET password = ? WHERE user_id = ?"
                        )) {
                            ps.setString(1, newPassword);
                            ps.setLong(2, userId);
                            int rows = ps.executeUpdate();
                            if (rows > 0) {
                                System.out.println("Password updated");
                            } else {
                                System.out.println("No account found with that ID");
                            }
                        }
                    }

                    default -> System.out.println("Invalid option");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Determines if the application is running in development mode based on system properties,
     * environment variables, or command-line arguments.
     *
     * @param args an array of command-line arguments
     * @return {@code true} if the application is in development mode; {@code false} otherwise
     */
    private static boolean isDevMode(String[] args) {
        if (Boolean.getBoolean("devMode"))  //Add VM option -DdevMode=true
            return true;
        if ("true".equalsIgnoreCase(System.getenv("DEV_MODE")))  //Environment variable DEV_MODE=true
            return true;
        return Arrays.asList(args).contains("--dev"); //Argument --dev
    }

    /**
     * Reads configuration with precedence: Java system property first, then environment variable.
     * Returns trimmed value or null if neither source provides a non-empty value.
     */
    private static String resolveConfig(String propertyKey, String envKey) {
        String v = System.getProperty(propertyKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(envKey);
        }
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}
