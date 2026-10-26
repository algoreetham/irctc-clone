package railway.repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
 * Genuine JDBC Database Repository implementation.
 * Satisfies Alliance University Course Outcomes:
 * - CO-5 / PrO-13: Java Database Connectivity (JDBC)
 * - LUO-5a: Database connectivity setup
 * - LUO-5b: Querying tables using JDBC Statements, PreparedStatements, and ResultSets
 * - Transaction handling with setAutoCommit, commit, and rollback.
 */
public class JdbcBookingRepository implements BookingRepository {
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String dbUrl;
    private final TrainService trainService;

    public JdbcBookingRepository(String dbUrl, TrainService trainService) {
        this.dbUrl = dbUrl;
        this.trainService = trainService;
        initializeDatabase();
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Creates table schema using JDBC Statement (LUO-5b).
     */
    private void initializeDatabase() {
        String sql = "CREATE TABLE IF NOT EXISTS bookings (" +
                "pnr TEXT PRIMARY KEY, " +
                "passenger_name TEXT NOT NULL, " +
                "passenger_age INTEGER NOT NULL, " +
                "is_senior INTEGER NOT NULL, " +
                "train_number TEXT NOT NULL, " +
                "seat_count INTEGER NOT NULL, " +
                "ticket_class TEXT NOT NULL, " +
                "fare REAL NOT NULL, " +
                "booking_time TEXT NOT NULL, " +
                "status TEXT NOT NULL, " +
                "waiting_list_number INTEGER DEFAULT 0" +
                ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("[JDBC Success] Connected to database and verified table schema: " + dbUrl);
        } catch (SQLException e) {
            System.err.println("[JDBC Error] Failed to initialize database: " + e.getMessage());
        }
    }

    @Override
    public synchronized void save(Booking b) {
        String insertSql = "INSERT OR REPLACE INTO bookings " +
                "(pnr, passenger_name, passenger_age, is_senior, train_number, seat_count, ticket_class, fare, booking_time, status, waiting_list_number) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false); // Transaction boundary

            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, b.getPnr());
                pstmt.setString(2, b.getPassenger().getName());
                pstmt.setInt(3, b.getPassenger().getAge());
                pstmt.setInt(4, (b.getPassenger() instanceof SeniorPassenger) ? 1 : 0);
                pstmt.setString(5, b.getTrain().getNumber());
                pstmt.setInt(6, b.getSeatCount());
                pstmt.setString(7, b.getTicketClass().name());
                pstmt.setDouble(8, b.getFare());
                pstmt.setString(9, b.getBookingTime().format(ISO_FORMATTER));
                pstmt.setString(10, b.getStatus().name());
                pstmt.setInt(11, b.getWaitingListNumber());

                pstmt.executeUpdate();
            }

            conn.commit(); // Commit transaction
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    System.err.println("[JDBC Rollback Error] " + ex.getMessage());
                }
            }
            System.err.println("[JDBC Error] Failed to save booking " + b.getPnr() + ": " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public synchronized Booking findByPnr(String pnr) {
        if (pnr == null) return null;
        String query = "SELECT * FROM bookings WHERE UPPER(pnr) = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, pnr.trim().toUpperCase());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToBooking(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JDBC Error] Failed to query booking by PNR: " + e.getMessage());
        }
        return null;
    }

    @Override
    public synchronized List<Booking> findAll() {
        List<Booking> list = new ArrayList<>();
        String query = "SELECT * FROM bookings ORDER BY booking_time DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                Booking b = mapRowToBooking(rs);
                if (b != null) {
                    list.add(b);
                }
            }
        } catch (SQLException e) {
            System.err.println("[JDBC Error] Failed to query all bookings: " + e.getMessage());
        }
        return list;
    }

    @Override
    public synchronized void update(Booking b) {
        String updateSql = "UPDATE bookings SET status = ?, waiting_list_number = ? WHERE pnr = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, b.getStatus().name());
            pstmt.setInt(2, b.getWaitingListNumber());
            pstmt.setString(3, b.getPnr());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[JDBC Error] Failed to update booking: " + e.getMessage());
        }
    }

    private Booking mapRowToBooking(ResultSet rs) throws SQLException {
        String pnr = rs.getString("pnr");
        String name = rs.getString("passenger_name");
        int age = rs.getInt("passenger_age");
        boolean isSenior = rs.getInt("is_senior") == 1;
        String trainNum = rs.getString("train_number");
        int seatCount = rs.getInt("seat_count");
        TicketClass ticketClass = TicketClass.valueOf(rs.getString("ticket_class"));
        double fare = rs.getDouble("fare");
        LocalDateTime bookingTime = LocalDateTime.parse(rs.getString("booking_time"), ISO_FORMATTER);
        BookingStatus status = BookingStatus.valueOf(rs.getString("status"));
        int wlNumber = rs.getInt("waiting_list_number");

        Passenger passenger = (isSenior && age >= SeniorPassenger.MIN_SENIOR_AGE)
                ? new SeniorPassenger(name, age)
                : new Passenger(name, age);

        Train train = trainService.getTrainByNumber(trainNum);
        if (train == null) {
            train = new Train(trainNum, "Historical Train", "Source", "Destination", "00:00", "00:00", 100, 200.0);
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
    }
}
