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
     * Kör huvudflödet för applikationen, inklusive inloggning och enkel meny för att lista månfärder.
     * <p>
     * Vad som har lagts till i denna version:
     * <p>
     * Skapar en Scanner för användarinput.
     * Frågar användaren efter användarnamn och lösenord och kontrollerar dessa mot databasen.
     * Om inloggning lyckas skrivs "Login successful!" ut, annars avslutas programmet med felmeddelande.
     * Efter lyckad inloggning visas en enkel meny med två alternativ:
     * <p>
     * Lista alla månfärder (namn på rymdfarkoster).
     * Avsluta programmet.
     * <p>
     * Valet "Lista månfärder" hämtar alla rymdfarkoster från tabellen "moon_mission" och skriver ut dem i ordning eftermission_id.
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
                System.out.println("0) Exit");

                String choice = scanner.nextLine().trim();
                if ("0".equals(choice)) {
                    break;
                } else if ("1".equals(choice)) {
                    try (PreparedStatement ps = connection.prepareStatement(
                            "SELECT spacecraft FROM moon_mission ORDER BY mission_id"
                    )) {
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) {
                            System.out.println(rs.getString("spacecraft"));
                        }
                    }
                } else {
                    System.out.println("Invalid option");
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
