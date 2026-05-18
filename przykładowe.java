package TASK2_Kowalski_123456;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class App {

    // Dane do połączenia z bazą danych (dostosuj do swojej konfiguracji SQL Server)
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=DB__Kowalski_123456;encrypt=true;trustServerCertificate=true;";
    private static final String USER = "sa";
    private static final String PASSWORD = "TwojeHaslo123";

    private Connection connection;
    private final Random random = new Random();

    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    public void run() {
        try {
            System.out.println("Nawiązywanie połączenia z bazą danych...");
            // Połączenie typu "gruby klient" (bez puli połączeń)
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Połączenie ustanowione pomyślnie!\n");

            // Krok 2.a: Usuwa wszystkie rekordy z tabeli
            System.out.println("--- KROK 2.a: Czyszczenie tabeli ---");
            wyczyscTabele();
            wyswietlTabele();

            // Krok 2.b: Dodaje trzy konkretne zamówienia na sztywno
            System.out.println("\n--- KROK 2.b: Dodawanie 3 konkretnych zamówień ---");
            dodajZamowienieNaSztywno("Fikus", 45.50, 2);
            dodajZamowienieNaSztywno("Monstera", 80.00, 1);
            dodajZamowienieNaSztywno("Kaktus", 15.00, 5);
            wyswietlTabele();

            // Krok 2.c: Modyfikacja (podwojenie cen) + jedno nowe zamówienie za 500 PLN
            System.out.println("\n--- KROK 2.c: Podwojenie cen i dodanie zamówienia za 500 PLN ---");
            podwojCeny();
            dodajZamowienieZaKwote("Bonsai", 500.00);
            wyswietlTabele();

            // Krok 2.d: W pętli tworzy 10 losowych zamówień, a potem losowo zmienia 3 z nich
            System.out.println("\n--- KROK 2.d: Generowanie 10 losowych zamówień i losowa zmiana 3 z nich ---");
            generujLosoweZamowienia(10);
            modyfikujLosoweRekordy(3);
            wyswietlTabele();

        } catch (SQLException e) {
            System.err.println("Błąd bazy danych: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    System.out.println("\nPołączenie z bazą danych zostało zamknięte.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // --- METODY REALIZUJĄCE TRANSAKCJE BAZODANOWE ---

    // 2.a
    private void wyczyscTabele() throws SQLException {
        String sql = "DELETE FROM ZAMOWIENIA_Kowalski_123456";
        try (Statement stmt = connection.createStatement()) {
            int rows = stmt.executeUpdate(sql);
            System.out.println("Usunięto rekordów: " + rows);
        }
    }

    // 2.b
    private void dodajZamowienieNaSztywno(String nazwa, double cenaSztuki, int ilosc) throws SQLException {
        double cenaCalosciowa = cenaSztuki * ilosc;
        String sql = "INSERT INTO ZAMOWIENIA_Kowalski_123456 (Nazwa_rosliny, Cena_sztuki, Ilosc, Cena_calosciowa) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nazwa);
            pstmt.setDouble(2, cenaSztuki);
            pstmt.setInt(3, ilosc);
            pstmt.setDouble(4, cenaCalosciowa);
            pstmt.executeUpdate();
        }
    }

    // 2.c - część 1
    private void podwojCeny() throws SQLException {
        String sql = "UPDATE ZAMOWIENIA_Kowalski_123456 SET Cena_sztuki = Cena_sztuki * 2, Cena_calosciowa = (Cena_sztuki * 2) * Ilosc";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    // 2.c - część 2
    private void dodajZamowienieZaKwote(String nazwa, double cenaCalosciowa) throws SQLException {
        // Skoro cena_calosciowa ma wynosić dokładnie 500 PLN, przyjmujemy 1 sztukę
        String sql = "INSERT INTO ZAMOWIENIA_Kowalski_123456 (Nazwa_rosliny, Cena_sztuki, Ilosc, Cena_calosciowa) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, nazwa);
            pstmt.setDouble(2, cenaCalosciowa);
            pstmt.setInt(3, 1);
            pstmt.setDouble(4, cenaCalosciowa);
            pstmt.executeUpdate();
        }
    }

    // 2.d - część 1
    private void generujLosoweZamowienia(int ile) throws SQLException {
        String[] rosliny = {"Storczyk", "Paproć", "Sukkulent", "Dracena", "Bluszcz", "Palma", "Aloes"};
        String sql = "INSERT INTO ZAMOWIENIA_Kowalski_123456 (Nazwa_rosliny, Cena_sztuki, Ilosc, Cena_calosciowa) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < ile; i++) {
                String nazwa = rosliny[random.nextInt(rosliny.length)];
                double cenaSztuki = 10 + (100 - 10) * random.nextDouble(); // losowa cena od 10 do 100
                int ilosc = random.nextInt(5) + 1; // losowa ilość od 1 do 5
                double cenaCalosciowa = cenaSztuki * ilosc;

                // Zaokrąglenie ceny do 2 miejsc po przecinku
                cenaSztuki = Math.round(cenaSztuki * 100.0) / 100.0;
                cenaCalosciowa = Math.round(cenaCalosciowa * 100.0) / 100.0;

                pstmt.setString(1, nazwa);
                pstmt.setDouble(2, cenaSztuki);
                pstmt.setInt(3, ilosc);
                pstmt.setDouble(4, cenaCalosciowa);
                pstmt.executeUpdate();
            }
        }
    }

    // 2.d - część 2
    private void modyfikujLosoweRekordy(int ileDoModyfikacji) throws SQLException {
        // Pobieramy ID wszystkich aktualnych rekordów
        List<Integer> identyfikatory = new ArrayList<>();
        String sqlSelect = "SELECT Id_zamowienia FROM ZAMOWIENIA_Kowalski_123456";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sqlSelect)) {
            while (rs.next()) {
                identyfikatory.add(rs.getInt("Id_zamowienia"));
            }
        }

        if (identyfikatory.isEmpty()) return;

        // Losowo wybieramy i modyfikujemy 'ileDoModyfikacji' rekordów
        String sqlUpdate = "UPDATE ZAMOWIENIA_Kowalski_123456 SET Nazwa_rosliny = ?, Cena_sztuki = ?, Cena_calosciowa = ? * Ilosc WHERE Id_zamowienia = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlUpdate)) {
            for (int i = 0; i < Math.min(ileDoModyfikacji, identyfikatory.size()); i++) {
                // Wybór losowego ID z listy i usunięcie go, żeby nie modyfikować dwa razy tego samego
                int losowyIndeks = random.nextInt(identyfikatory.size());
                int idZamowienia = identyfikatory.remove(losowyIndeks);

                double nowaCena = 20 + random.nextDouble() * 50;
                nowaCena = Math.round(nowaCena * 100.0) / 100.0;

                pstmt.setString(1, "Zmodyfikowana Roślina " + (i + 1));
                pstmt.setDouble(2, nowaCena);
                pstmt.setDouble(3, nowaCena);
                pstmt.setInt(4, idZamowienia);
                pstmt.executeUpdate();
            }
        }
    }

    // Uwagi b: Pobieranie i wyświetlanie całej tabeli w konsoli za pomocą print()
    private void wyswietlTabele() throws SQLException {
        String sql = "SELECT * FROM ZAMOWIENIA_Kowalski_123456";
        System.out.println("\n--- AKTUALNA ZAWARTOŚĆ TABELI ---");
        System.out.printf("%-5s | %-25s | %-12s | %-6s | %-15s\n", "ID", "Nazwa rośliny", "Cena szt.", "Ilość", "Cena całkowita");
        System.out.println("-----------------------------------------------------------------------------");
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.printf("%-5d | %-25s | %-12.2f | %-6d | %-15.2f\n",
                        rs.getInt("Id_zamowienia"),
                        rs.getString("Nazwa_rosliny"),
                        rs.getDouble("Cena_sztuki"),
                        rs.getInt("Ilosc"),
                        rs.getDouble("Cena_calosciowa")
                );
            }
        }
        System.out.println("-----------------------------------------------------------------------------\n");
    }
}