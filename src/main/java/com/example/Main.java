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
     * Kör huvudflödet för applikationen, inklusive inloggning och enkel meny för att lista månfärder,
     * skapa nya konton och ta bort konton.
     * <p>
     * Skapar en Scanner för användarinput.
     * Frågar användaren efter användarnamn och lösenord och kontrollerar dessa mot databasen.
     * Om inloggning lyckas skrivs "Login successful!" ut, annars avslutas programmet med felmeddelande.
     * Efter lyckad inloggning visas en enkel meny med följande alternativ:
     * <p>
     * 1) Lista alla månfärder (namn på rymdfarkoster)
     * Hämtar alla rymdfarkoster från tabellen "moon_mission" och skriver ut dem i ordning
     * efter mission_id. Användaren får en snabb översikt över vilka månfärder som finns
     * registrerade i databasen.
     * <p>
     * 2) Hämta månfärd efter mission_id
     * Frågar efter ett specifikt mission_id och hämtar information om månfärden med det ID:t.
     * Om månfärden finns skrivs namnet på rymdfarkosten ut, annars meddelas att månfärden inte
     * hittades.
     * <p>
     * 3) Räkna månfärder för ett visst år
     * Frågar användaren efter ett år och räknar antalet månfärder som har launch_date inom
     * det året. Resultatet visar hur många månfärder som genomfördes under det valda året.
     * <p>
     * 4) Skapa ett nytt konto (förnamn, efternamn, SSN, lösenord)
     * Frågar efter användarens förnamn, efternamn, SSN och lösenord och skapar ett nytt konto
     * i tabellen "account". Detta gör det möjligt för nya användare att registrera sig.
     * <p>
     * 5) Uppdatera lösenord för ett konto via user_id
     * Frågar efter user_id för kontot som ska uppdateras samt det nya lösenordet. Uppdaterar
     * kontots lösenord i databasen om kontot finns, annars meddelas att inget konto med
     * angivet ID hittades.
     * <p>
     * 6) Ta bort ett konto via user_id
     * Frågar efter user_id för kontot som ska tas bort och tar bort kontot från tabellen
     * "account". Om kontot inte finns meddelas användaren om att inget konto hittades.
     * <p>
     * 0) Avsluta programmet
     * Bryter menyn och avslutar metoden.
     * <p>
     * Ogiltiga val hanteras med felmeddelanden.
     */
    public void run() {
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
                System.out.println("6) Delete account");
                System.out.println("0) Exit");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "0" -> {
                        return;
                    }
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
                        int id;
                        try {
                            id = Integer.parseInt(scanner.nextLine().trim());
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid mission ID");
                            break;
                        }
                        try (PreparedStatement ps = connection.prepareStatement(
                                "SELECT spacecraft FROM moon_mission WHERE mission_id = ?"
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
                        int year;
                        try {
                            year = Integer.parseInt(scanner.nextLine().trim());
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid year");
                            break;
                        }
                        try (PreparedStatement ps = connection.prepareStatement(
                                "SELECT COUNT(*) FROM moon_mission WHERE YEAR(launch_date) = ?"
                        )) {
                            ps.setInt(1, year);
                            ResultSet rs = ps.executeQuery();
                            rs.next();
                            int count = rs.getInt(1);
                            System.out.println(year + " missions: " + count);
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

                    case "6" -> {
                        System.out.println("User ID of account to delete:");
                        long userId = Long.parseLong(scanner.nextLine().trim());

                        try (PreparedStatement ps = connection.prepareStatement(
                                "DELETE FROM account WHERE user_id = ?"
                        )) {
                            ps.setLong(1, userId);
                            int rows = ps.executeUpdate();
                            if (rows > 0) {
                                System.out.println("Account deleted");
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
        if (Boolean.getBoolean("devMode"))
            return true;
        if ("true".equalsIgnoreCase(System.getenv("DEV_MODE")))
            return true;
        return Arrays.asList(args).contains("--dev");
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
