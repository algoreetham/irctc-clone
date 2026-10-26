package railway.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import railway.exception.InvalidInputException;

/**
 * Represents a reservation record.
 * Demonstrates composition (has-a Passenger, has-a Train, has-a Ticket).
 * Supports Confirmed, Waiting List, and Cancelled statuses as required by
 * Alliance University Course Code E1CSA25306.
 */
public class Booking {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    private final String pnr;
    private final Passenger passenger;
    private final Train train;
    private final int seatCount;
    private final TicketClass ticketClass;
    private final Ticket ticket;
    private final double fare;
    private final LocalDateTime bookingTime;
    private BookingStatus status;
    private int waitingListNumber; // 0 if confirmed, > 0 if on waiting list (e.g. WL-1 or TQWL-1)
    private final String quota; // "GENERAL" or "TATKAL"

    public Booking(String pnr, Passenger passenger, Train train, int seatCount,
                   TicketClass ticketClass, Ticket ticket, double fare, LocalDateTime bookingTime,
                   BookingStatus status) {
        this(pnr, passenger, train, seatCount, ticketClass, ticket, fare, bookingTime, status, 0, "GENERAL");
    }

    public Booking(String pnr, Passenger passenger, Train train, int seatCount,
                   TicketClass ticketClass, Ticket ticket, double fare, LocalDateTime bookingTime,
                   BookingStatus status, int waitingListNumber) {
        this(pnr, passenger, train, seatCount, ticketClass, ticket, fare, bookingTime, status, waitingListNumber, "GENERAL");
    }

    public Booking(String pnr, Passenger passenger, Train train, int seatCount,
                   TicketClass ticketClass, Ticket ticket, double fare, LocalDateTime bookingTime,
                   BookingStatus status, int waitingListNumber, String quota) {
        if (pnr == null || pnr.trim().isEmpty()) {
            throw new IllegalArgumentException("PNR cannot be empty.");
        }
        if (passenger == null) {
            throw new IllegalArgumentException("Passenger cannot be null.");
        }
        if (train == null) {
            throw new IllegalArgumentException("Train cannot be null.");
        }
        if (seatCount <= 0) {
            throw new IllegalArgumentException("Seat count must be positive.");
        }
        if (ticketClass == null) {
            throw new IllegalArgumentException("Ticket class cannot be null.");
        }
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket cannot be null.");
        }
        if (fare <= 0) {
            throw new IllegalArgumentException("Fare must be positive.");
        }

        this.pnr = pnr.trim().toUpperCase();
        this.passenger = passenger;
        this.train = train;
        this.seatCount = seatCount;
        this.ticketClass = ticketClass;
        this.ticket = ticket;
        this.fare = fare;
        this.bookingTime = (bookingTime != null) ? bookingTime : LocalDateTime.now();
        this.status = (status != null) ? status : BookingStatus.CONFIRMED;
        this.waitingListNumber = waitingListNumber;
        this.quota = (quota != null && !quota.trim().isEmpty()) ? quota.trim().toUpperCase() : "GENERAL";
    }

    public String getQuota() {
        return quota;
    }

    public boolean isTatkal() {
        return "TATKAL".equalsIgnoreCase(quota);
    }

    public String getStatusLabel() {
        if (status == BookingStatus.WAITING_LIST) {
            return isTatkal() ? ("TQWL-" + waitingListNumber) : ("WL-" + waitingListNumber);
        }
        return isTatkal() ? "TATKAL CONFIRMED" : status.name();
    }

    public String getPnr() {
        return pnr;
    }

    public Passenger getPassenger() {
        return passenger;
    }

    public Train getTrain() {
        return train;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public TicketClass getTicketClass() {
        return ticketClass;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public double getFare() {
        return fare;
    }

    public LocalDateTime getBookingTime() {
        return bookingTime;
    }

    public String getFormattedBookingTime() {
        return bookingTime.format(FORMATTER);
    }

    public synchronized BookingStatus getStatus() {
        return status;
    }

    public synchronized int getWaitingListNumber() {
        return waitingListNumber;
    }

    public synchronized void setWaitingListNumber(int waitingListNumber) {
        this.waitingListNumber = waitingListNumber;
    }

    public synchronized boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public synchronized boolean isWaitingList() {
        return status == BookingStatus.WAITING_LIST;
    }

    public synchronized boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    /**
     * Promotes a waiting list ticket to Confirmed status upon seat availability.
     */
    public synchronized void promoteToConfirmed() {
        this.status = BookingStatus.CONFIRMED;
        this.waitingListNumber = 0;
    }

    /**
     * Marks the booking as cancelled.
     * Prevents duplicate cancellation.
     */
    public synchronized void cancel() throws InvalidInputException {
        if (this.status == BookingStatus.CANCELLED) {
            throw new InvalidInputException("Booking is already cancelled. Cannot cancel twice.");
        }
        this.status = BookingStatus.CANCELLED;
        this.waitingListNumber = 0;
    }

    public void displayDetails() {
        System.out.println("==================================================");
        System.out.println("             DEMO TICKET CONFIRMATION             ");
        System.out.println("          (Educational Simulation Only)           ");
        System.out.println("==================================================");
        System.out.printf(" PNR Number       : %s%n", pnr);
        System.out.printf(" Quota            : %s%n", quota);
        System.out.printf(" Status           : %s%n", getStatusLabel());
        System.out.printf(" Booking Time     : %s%n", getFormattedBookingTime());
        System.out.println("--------------------------------------------------");
        System.out.printf(" Train            : %s (%s)%n", train.getName(), train.getNumber());
        System.out.printf(" Route            : %s -> %s%n", train.getSource(), train.getDestination());
        System.out.printf(" Departure/Arrival: %s -> %s%n", train.getDepartureTime(), train.getArrivalTime());
        System.out.printf(" Travel Class     : %s%n", ticketClass.getDisplayName());
        System.out.printf(" Seats Booked     : %d seat(s)%n", seatCount);
        System.out.println("--------------------------------------------------");
        System.out.printf(" Passenger Name   : %s%n", passenger.getName());
        System.out.printf(" Passenger Age    : %d years (%s)%n", passenger.getAge(), passenger.getCategory());
        System.out.printf(" Total Fare Paid  : ₹%.2f (Demo simulated)%n", fare);
        System.out.println("==================================================");
    }

    @Override
    public String toString() {
        return String.format("[%s | %s] PNR: %s | %s (%s) | %s -> %s | %d seat(s) | Class: %s | ₹%.2f | %s",
                quota, getStatusLabel(), pnr, passenger.getName(), passenger.getCategory(),
                train.getSource(), train.getDestination(), seatCount, ticketClass, fare, getFormattedBookingTime());
    }
}
