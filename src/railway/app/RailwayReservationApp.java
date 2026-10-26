package railway.app;

import java.util.Scanner;
import railway.repository.BookingRepository;
import railway.repository.FileBookingRepository;
import railway.repository.JdbcBookingRepository;
import railway.service.BookingService;
import railway.service.DemoPaymentService;
import railway.service.PaymentService;
import railway.service.TrainService;
import railway.service.WaitingListWorker;
import railway.ui.ConsoleMenu;

/**
 * Main application entry point for the IRCTC-inspired Railway Reservation System.
 * Aligns with Alliance University Course Code E1CSA25306.
 * Bootstraps system components, injects dependencies, starts background worker threads,
 * and launches the console interface.
 */
public class RailwayReservationApp {
    private static final String DEFAULT_CSV_PATH = "data/bookings.csv";
    private static final String DEFAULT_JDBC_URL = "jdbc:sqlite:data/railway.db";

    public static void main(String[] args) {
        System.out.println("================================================================");
        System.out.println("   WELCOME TO IRCTC-INSPIRED RAILWAY RESERVATION SYSTEM (DEMO)  ");
        System.out.println("            Alliance University - B.Tech CSE (Sem III)          ");
        System.out.println("            Course Code: E1CSA25306 - Programming in Java       ");
        System.out.println("================================================================\n");

        TrainService trainService = new TrainService();
        DemoPaymentService paymentService = new DemoPaymentService();

        // Demonstrates JDBC database connectivity (CO-5, PrO-13) with CSV fallback (CO-4, PrO-12)
        BookingRepository repository;
        try {
            Class.forName("org.sqlite.JDBC");
            repository = new JdbcBookingRepository(DEFAULT_JDBC_URL, trainService);
        } catch (Throwable t) {
            System.out.println("[Notice] SQLite driver not found in classpath. Falling back to FileBookingRepository (CSV).");
            repository = new FileBookingRepository(DEFAULT_CSV_PATH, trainService);
        }

        BookingService bookingService = new BookingService(trainService, paymentService, repository);

        // Multithreading: Start background allocation worker (CO-3, PrO-8, PrO-9)
        WaitingListWorker worker = new WaitingListWorker(bookingService.getAllocationLock(), bookingService, repository);
        worker.start();

        try (Scanner scanner = new Scanner(System.in)) {
            ConsoleMenu menu = new ConsoleMenu(trainService, bookingService, scanner);
            menu.start();
        } finally {
            worker.shutdown();
        }
    }
}
