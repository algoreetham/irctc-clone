package railway.repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import railway.model.ACTicket;
import railway.model.Booking;
import railway.model.BookingStatus;
import railway.model.GeneralTicket;
import railway.model.Passenger;
import railway.model.SeniorPassenger;
import railway.model.SleeperTicket;
import railway.model.Ticket;
import railway.model.TicketClass;
import railway.model.Train;
import railway.service.TrainService;

/**
 * File-backed repository using CSV format with BufferedReader and BufferedWriter.
 * Demonstrates File I/O, Character Stream classes (CO-4, PrO-12, LUO-4e).
 */
public class FileBookingRepository implements BookingRepository {
    private static final String CSV_HEADER = "PNR,PassengerName,PassengerAge,IsSenior,TrainNumber,SeatCount,TicketClass,Fare,BookingTime,Status,WaitingListNumber";
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String filePath;
    private final TrainService trainService;
    private final Map<String, Booking> bookingMap = new LinkedHashMap<>();

    public FileBookingRepository(String filePath, TrainService trainService) {
        this.filePath = filePath;
        this.trainService = trainService;
        ensureDataFileExists();
        loadBookingsFromFile();
    }

    private void ensureDataFileExists() {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    writer.write(CSV_HEADER);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("[Repository Warning] Unable to initialize storage file: " + e.getMessage());
        }
    }

    private void loadBookingsFromFile() {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                Booking booking = parseCsvLine(line);
                if (booking != null) {
                    bookingMap.put(booking.getPnr(), booking);
                    // If the booking is confirmed, synchronize the train's available seats
                    if (booking.isConfirmed() && booking.getTrain() != null) {
                        try {
                            booking.getTrain().reserveSeats(booking.getSeatCount());
                        } catch (Exception ignored) {
                            // Best-effort seat sync on cold startup
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Repository Warning] Failed to read bookings file: " + e.getMessage());
        }
    }

    private Booking parseCsvLine(String line) {
        try {
            String[] parts = line.split(",", -1);
            if (parts.length < 10) {
                return null;
            }

            String pnr = parts[0].trim();
            String passengerName = parts[1].trim();
            int passengerAge = Integer.parseInt(parts[2].trim());
            boolean isSenior = Boolean.parseBoolean(parts[3].trim());
            String trainNumber = parts[4].trim();
            int seatCount = Integer.parseInt(parts[5].trim());
            TicketClass ticketClass = TicketClass.valueOf(parts[6].trim());
            double fare = Double.parseDouble(parts[7].trim());
            LocalDateTime bookingTime = LocalDateTime.parse(parts[8].trim(), ISO_FORMATTER);
            BookingStatus status = BookingStatus.valueOf(parts[9].trim());
            int wlNumber = (parts.length >= 11 && !parts[10].trim().isEmpty()) ? Integer.parseInt(parts[10].trim()) : 0;

            Passenger passenger;
            if (isSenior && passengerAge >= SeniorPassenger.MIN_SENIOR_AGE) {
                passenger = new SeniorPassenger(passengerName, passengerAge);
            } else {
                passenger = new Passenger(passengerName, passengerAge);
            }

            Train train = trainService.getTrainByNumber(trainNumber);
            if (train == null) {
                train = new Train(trainNumber, "Historical Train", "Source", "Destination", "00:00", "00:00", 100, 200.0);
            }

            Ticket ticket;
            switch (ticketClass) {
                case SLEEPER:
                    ticket = new SleeperTicket(pnr, passenger, train, seatCount);
                    break;
                case AC:
                    ticket = new ACTicket(pnr, passenger, train, seatCount);
                    break;
                case GENERAL:
                default:
                    ticket = new GeneralTicket(pnr, passenger, train, seatCount);
                    break;
            }

            return new Booking(pnr, passenger, train, seatCount, ticketClass, ticket, fare, bookingTime, status, wlNumber);
        } catch (Exception ex) {
            System.err.println("[Repository Warning] Skipping malformed record: " + line + " (" + ex.getMessage() + ")");
            return null;
        }
    }

    @Override
    public synchronized void save(Booking booking) {
        bookingMap.put(booking.getPnr(), booking);
        appendToFile(booking);
    }

    @Override
    public synchronized Booking findByPnr(String pnr) {
        if (pnr == null) {
            return null;
        }
        return bookingMap.get(pnr.trim().toUpperCase());
    }

    @Override
    public synchronized List<Booking> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(bookingMap.values()));
    }

    @Override
    public synchronized void update(Booking booking) {
        bookingMap.put(booking.getPnr(), booking);
        rewriteAllToFile();
    }

    private void appendToFile(Booking b) {
        File file = new File(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(formatToCsv(b));
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[Repository Error] Failed to write booking to disk: " + e.getMessage());
        }
    }

    private void rewriteAllToFile() {
        File file = new File(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(CSV_HEADER);
            writer.newLine();
            for (Booking b : bookingMap.values()) {
                writer.write(formatToCsv(b));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("[Repository Error] Failed to update bookings file on disk: " + e.getMessage());
        }
    }

    private String formatToCsv(Booking b) {
        boolean isSenior = b.getPassenger() instanceof SeniorPassenger;
        return String.join(",",
                b.getPnr(),
                b.getPassenger().getName(),
                String.valueOf(b.getPassenger().getAge()),
                String.valueOf(isSenior),
                b.getTrain().getNumber(),
                String.valueOf(b.getSeatCount()),
                b.getTicketClass().name(),
                String.format("%.2f", b.getFare()),
                b.getBookingTime().format(ISO_FORMATTER),
                b.getStatus().name(),
                String.valueOf(b.getWaitingListNumber())
        );
    }
}
