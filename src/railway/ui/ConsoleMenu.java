package railway.ui;

import java.util.List;
import java.util.Scanner;
import railway.exception.BookingNotFoundException;
import railway.exception.RailwayException;
import railway.model.Booking;
import railway.model.SeniorPassenger;
import railway.model.TicketClass;
import railway.model.Train;
import railway.service.BookingService;
import railway.service.TrainService;

/**
 * Console user interface for interacting with the Railway Reservation System.
 * Handles input validation, menu display, and graceful error reporting.
 */
public class ConsoleMenu {
    private final TrainService trainService;
    private final BookingService bookingService;
    private final Scanner scanner;

    public ConsoleMenu(TrainService trainService, BookingService bookingService, Scanner scanner) {
        this.trainService = trainService;
        this.bookingService = bookingService;
        this.scanner = scanner;
    }

    public void start() {
        boolean running = true;
        while (running) {
            printMainMenu();
            System.out.print("Enter your choice: ");
            String input = scanner.nextLine().trim();

            int choice;
            try {
                choice = Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("\n[Error] Invalid input. Please enter a valid number (1-7).\n");
                continue;
            }

            System.out.println();
            switch (choice) {
                case 1:
                    handleSearchTrains();
                    break;
                case 2:
                    handleCheckAvailability();
                    break;
                case 3:
                    handleBookTicket();
                    break;
                case 4:
                    handleViewBooking();
                    break;
                case 5:
                    handleCancelBooking();
                    break;
                case 6:
                    handleBookingHistory();
                    break;
                case 7:
                    System.out.println("Thank you for using the Demo Railway Reservation System. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("[Error] Choice out of range. Please choose a number between 1 and 7.\n");
                    break;
            }
        }
    }

    private void printMainMenu() {
        System.out.println("==========================================");
        System.out.println("     RAILWAY RESERVATION SYSTEM (DEMO)    ");
        System.out.println("       Educational project — not IRCTC    ");
        System.out.println("==========================================");
        System.out.println("1. Search Trains");
        System.out.println("2. Check Seat Availability");
        System.out.println("3. Book Ticket");
        System.out.println("4. View Booking by PNR");
        System.out.println("5. Cancel Booking");
        System.out.println("6. View Booking History");
        System.out.println("7. Exit");
    }

    private void handleSearchTrains() {
        System.out.println("--- SEARCH TRAINS ---");
        System.out.print("Enter Source Station: ");
        String source = scanner.nextLine().trim();

        System.out.print("Enter Destination Station: ");
        String destination = scanner.nextLine().trim();

        try {
            List<Train> matchingTrains = trainService.searchTrains(source, destination);
            if (matchingTrains.isEmpty()) {
                System.out.println("\nNo demo trains found for route: " + source + " -> " + destination);
                System.out.println("Available demo routes include: Bengaluru <-> Chennai, Bengaluru <-> Mysuru, Chennai <-> Hyderabad.\n");
            } else {
                System.out.printf("%nFound %d matching demo train(s):%n", matchingTrains.size());
                printTrainTable(matchingTrains);
            }
        } catch (RailwayException ex) {
            System.out.println("\n[Error] " + ex.getMessage() + "\n");
        }
    }

    private void handleCheckAvailability() {
        System.out.println("--- CHECK SEAT AVAILABILITY ---");
        List<Train> allTrains = trainService.getAllTrains();
        printTrainTable(allTrains);

        System.out.print("Enter Train Number (or press Enter to return): ");
        String trainNum = scanner.nextLine().trim();
        if (trainNum.isEmpty()) {
            return;
        }

        Train train = trainService.getTrainByNumber(trainNum);
        if (train == null) {
            System.out.println("\n[Error] Train number not found: " + trainNum + "\n");
            return;
        }

        System.out.println("\nTrain Details:");
        System.out.println("  Number          : " + train.getNumber());
        System.out.println("  Name            : " + train.getName());
        System.out.println("  Route           : " + train.getSource() + " -> " + train.getDestination());
        System.out.println("  Schedule        : Departs " + train.getDepartureTime() + " | Arrives " + train.getArrivalTime());
        System.out.println("  Available Seats : " + train.getAvailableSeats() + " / " + train.getTotalSeats());
        System.out.printf("  Base Fare       : ₹%.2f per seat%n%n", train.getBaseFare());
    }

    private void handleBookTicket() {
        System.out.println("--- BOOK TICKET (DEMO) ---");
        List<Train> allTrains = trainService.getAllTrains();
        printTrainTable(allTrains);

        System.out.print("Enter Train Number: ");
        String trainNum = scanner.nextLine().trim();
        Train train = trainService.getTrainByNumber(trainNum);
        if (train == null) {
            System.out.println("\n[Error] Invalid train number. Booking aborted.\n");
            return;
        }

        System.out.print("Enter Passenger Name: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) {
            System.out.println("\n[Error] Passenger name cannot be blank.\n");
            return;
        }

        System.out.print("Enter Passenger Age: ");
        int age;
        try {
            age = Integer.parseInt(scanner.nextLine().trim());
            if (age <= 0 || age > 120) {
                System.out.println("\n[Error] Age must be between 1 and 120.\n");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("\n[Error] Invalid age format. Please enter a valid number.\n");
            return;
        }

        if (age >= SeniorPassenger.MIN_SENIOR_AGE) {
            System.out.printf(">> Senior Citizen detected! 40%% concession will be applied.%n");
        }

        System.out.println("\nSelect Travel Class:");
        System.out.println("  1. General (2S) - Base fare multiplier: 1.0x");
        System.out.println("  2. Sleeper (SL) - Base fare multiplier: 1.5x");
        System.out.println("  3. AC Tier (3A) - Base fare multiplier: 2.5x");
        System.out.print("Enter Class choice (1-3): ");
        TicketClass selectedClass;
        String classChoice = scanner.nextLine().trim();
        switch (classChoice) {
            case "1":
                selectedClass = TicketClass.GENERAL;
                break;
            case "2":
                selectedClass = TicketClass.SLEEPER;
                break;
            case "3":
                selectedClass = TicketClass.AC;
                break;
            default:
                System.out.println("\n[Error] Invalid class selection. Booking aborted.\n");
                return;
        }

        System.out.print("Enter Number of Seats: ");
        int seats;
        try {
            seats = Integer.parseInt(scanner.nextLine().trim());
            if (seats <= 0) {
                System.out.println("\n[Error] Seat count must be greater than zero.\n");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("\n[Error] Invalid seat number format.\n");
            return;
        }

        boolean willBeWaitingList = (seats > train.getAvailableSeats());
        if (willBeWaitingList) {
            System.out.printf("%n>> NOTICE: Only %d seats available! Your booking will be placed on the WAITING LIST.%n", train.getAvailableSeats());
        }

        // Preview estimated fare
        double discountFactor = (age >= SeniorPassenger.MIN_SENIOR_AGE) ? 0.6 : 1.0;
        double estimatedFare = train.getBaseFare() * selectedClass.getFareMultiplier() * discountFactor * seats;
        System.out.printf("%nEstimated Demo Fare: ₹%.2f (for %d seats in %s)%n", estimatedFare, seats, selectedClass.getDisplayName());
        System.out.print("Proceed to simulated payment and confirmation? (Y/N): ");
        String confirm = scanner.nextLine().trim();
        if (!confirm.equalsIgnoreCase("Y")) {
            System.out.println("Booking cancelled by user.\n");
            return;
        }

        try {
            Booking booking = bookingService.bookTicket(trainNum, name, age, seats, selectedClass);
            if (booking.isWaitingList()) {
                System.out.printf("%n*** BOOKING CONFIRMED ON WAITING LIST (Rank: WL-%d) ***%n", booking.getWaitingListNumber());
            } else {
                System.out.println("\n*** BOOKING CONFIRMED! ***");
            }
            booking.displayDetails();
            System.out.println();
        } catch (RailwayException ex) {
            System.out.println("\n[Error] Booking failed: " + ex.getMessage() + "\n");
        }
    }

    private void handleViewBooking() {
        System.out.println("--- VIEW BOOKING BY PNR ---");
        System.out.print("Enter PNR Number (e.g., PNR-1001): ");
        String pnr = scanner.nextLine().trim();

        try {
            Booking booking = bookingService.getBooking(pnr);
            System.out.println();
            booking.displayDetails();
            System.out.println();
        } catch (BookingNotFoundException ex) {
            System.out.println("\n[Error] " + ex.getMessage() + "\n");
        } catch (RailwayException ex) {
            System.out.println("\n[Error] " + ex.getMessage() + "\n");
        }
    }

    private void handleCancelBooking() {
        System.out.println("--- CANCEL BOOKING ---");
        System.out.print("Enter PNR Number to cancel: ");
        String pnr = scanner.nextLine().trim();

        try {
            Booking booking = bookingService.getBooking(pnr);
            if (booking.isCancelled()) {
                System.out.println("\n[Error] Booking " + pnr.toUpperCase() + " is already cancelled.\n");
                return;
            }

            System.out.println("\nBooking to cancel:");
            booking.displayDetails();
            System.out.print("Are you sure you want to cancel this booking? (Y/N): ");
            String confirm = scanner.nextLine().trim();
            if (!confirm.equalsIgnoreCase("Y")) {
                System.out.println("Cancellation aborted by user.\n");
                return;
            }

            Booking cancelledBooking = bookingService.cancelBooking(pnr);
            System.out.printf("%n[Success] Booking %s has been CANCELLED.%n", cancelledBooking.getPnr());
            System.out.printf("Restored %d seat(s) to train %s (%s). Current available: %d.%n%n",
                    cancelledBooking.getSeatCount(),
                    cancelledBooking.getTrain().getNumber(),
                    cancelledBooking.getTrain().getName(),
                    cancelledBooking.getTrain().getAvailableSeats());
        } catch (BookingNotFoundException ex) {
            System.out.println("\n[Error] " + ex.getMessage() + "\n");
        } catch (RailwayException ex) {
            System.out.println("\n[Error] Cancellation failed: " + ex.getMessage() + "\n");
        }
    }

    private void handleBookingHistory() {
        System.out.println("--- BOOKING HISTORY ---");
        List<Booking> bookings = bookingService.getBookingHistory();
        if (bookings.isEmpty()) {
            System.out.println("No booking records found.\n");
            return;
        }

        System.out.printf("Total records found: %d%n", bookings.size());
        System.out.println("-------------------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-10s | %-18s | %-12s | %-12s | %-14s | %-5s | %-10s | %-10s%n",
                "Status", "PNR", "Passenger", "From", "To", "Class", "Seats", "Fare", "Train");
        System.out.println("-------------------------------------------------------------------------------------------------------------------------");
        for (Booking b : bookings) {
            String statusStr = b.isWaitingList() ? ("WL-" + b.getWaitingListNumber()) : b.getStatus().name();
            System.out.printf("%-10s | %-10s | %-18s | %-12s | %-12s | %-14s | %-5d | ₹%-9.2f | %-10s%n",
                    statusStr,
                    b.getPnr(),
                    truncate(b.getPassenger().getName(), 18),
                    truncate(b.getTrain().getSource(), 12),
                    truncate(b.getTrain().getDestination(), 12),
                    truncate(b.getTicketClass().getDisplayName(), 14),
                    b.getSeatCount(),
                    b.getFare(),
                    b.getTrain().getNumber());
        }
        System.out.println("-------------------------------------------------------------------------------------------------------------------------\n");
    }

    private void printTrainTable(List<Train> trains) {
        System.out.println("---------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-20s | %-12s | %-12s | %-10s | %-10s | %-10s | %-10s%n",
                "Number", "Train Name", "Source", "Destination", "Departure", "Arrival", "Seats Left", "Base Fare");
        System.out.println("---------------------------------------------------------------------------------------------------------");
        for (Train t : trains) {
            System.out.printf("%-10s | %-20s | %-12s | %-12s | %-10s | %-10s | %-10d | ₹%-9.2f%n",
                    t.getNumber(),
                    truncate(t.getName(), 20),
                    truncate(t.getSource(), 12),
                    truncate(t.getDestination(), 12),
                    t.getDepartureTime(),
                    t.getArrivalTime(),
                    t.getAvailableSeats(),
                    t.getBaseFare());
        }
        System.out.println("---------------------------------------------------------------------------------------------------------\n");
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 2) + "..";
    }
}
