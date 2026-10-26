package railway.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import railway.exception.BookingNotFoundException;
import railway.exception.InvalidInputException;
import railway.exception.RailwayException;
import railway.exception.SeatUnavailableException;
import railway.model.ACTicket;
import railway.model.Booking;
import railway.model.BookingStatus;
import railway.model.GeneralTicket;
import railway.model.Passenger;
import railway.model.SeniorPassenger;
import railway.model.SleeperTicket;
import railway.model.TatkalTicket;
import railway.model.Ticket;
import railway.model.TicketClass;
import railway.model.Train;
import railway.repository.BookingRepository;

/**
 * Service managing reservation lifecycle conforming to Alliance University Course Code E1CSA25306.
 * Implements:
 * - Waiting List (WL) Queue mechanism with max 100 waiting limit (Application Example 5)
 * - Stack for tracking recent actions/cancellations (CO-4, LUO-4a)
 * - Thread synchronization and interthread notification (CO-3, PrO-9)
 */
public class BookingService {
    public static final int MAX_WAITING_LIST_LIMIT = 100;
    private static final AtomicInteger PNR_COUNTER = new AtomicInteger(1000);

    private final TrainService trainService;
    private final PaymentService paymentService;
    private final BookingRepository bookingRepository;

    // Demonstrates FIFO Queue (CO-4 / Application Example 5: Waiting List)
    private final Map<String, Queue<Booking>> trainWaitingQueues = new HashMap<>();

    // Demonstrates Stack (CO-4, LUO-4a)
    private final Stack<Booking> recentActionsStack = new Stack<>();

    // Interthread lock for multithread synchronization (CO-3, PrO-9)
    private final Object allocationLock = new Object();

    public BookingService(TrainService trainService, PaymentService paymentService, BookingRepository bookingRepository) {
        this.trainService = trainService;
        this.paymentService = paymentService;
        this.bookingRepository = bookingRepository;
        syncWaitingListsFromRepository();
    }

    public Object getAllocationLock() {
        return allocationLock;
    }

    public TrainService getTrainService() {
        return trainService;
    }

    private synchronized void syncWaitingListsFromRepository() {
        for (Booking b : bookingRepository.findAll()) {
            if (b.isWaitingList() && b.getTrain() != null) {
                Queue<Booking> queue = trainWaitingQueues.computeIfAbsent(
                        b.getTrain().getNumber(), k -> new LinkedList<>()
                );
                queue.add(b);
            }
        }
    }

    private synchronized String generatePnr() {
        return "PNR-" + PNR_COUNTER.incrementAndGet();
    }

    /**
     * Books a ticket following university specifications:
     * - Confirms reservation if seats available
     * - Places on FIFO Waiting List (WL-1, WL-2...) if seats full (up to 100 limit)
     * - Disallows booking if waiting list exceeds limit
     */
    public Booking bookTicket(String trainNumber, String passengerName, int passengerAge,
                              int seatCount, TicketClass ticketClass) throws RailwayException {
        return bookTicket(trainNumber, passengerName, passengerAge, seatCount, ticketClass, "GENERAL");
    }

    /**
     * Books a ticket with specific Quota (e.g. GENERAL, TATKAL).
     * Demonstrates Method Overloading (CO-2, PrO-4) and Tatkal Polymorphism.
     */
    public Booking bookTicket(String trainNumber, String passengerName, int passengerAge,
                              int seatCount, TicketClass ticketClass, String quota) throws RailwayException {
        if (trainNumber == null || trainNumber.trim().isEmpty()) {
            throw new InvalidInputException("Train number cannot be blank.");
        }
        if (seatCount <= 0) {
            throw new InvalidInputException("Requested seat count must be greater than zero.");
        }
        if (ticketClass == null) {
            throw new InvalidInputException("Please select a valid ticket class.");
        }

        final String effectiveQuota = (quota != null && !quota.trim().isEmpty()) ? quota.trim().toUpperCase() : "GENERAL";
        boolean isTatkal = "TATKAL".equalsIgnoreCase(effectiveQuota);

        Train train = trainService.getTrainByNumber(trainNumber);
        if (train == null) {
            throw new InvalidInputException("No train found with number: " + trainNumber);
        }

        // Check seat availability vs Waiting List eligibility
        boolean seatsAvailable;
        int wlNumber = 0;
        synchronized (train) {
            seatsAvailable = train.hasAvailableSeats(seatCount);
            if (!seatsAvailable) {
                Queue<Booking> queue = trainWaitingQueues.computeIfAbsent(train.getNumber(), k -> new LinkedList<>());
                if (queue.size() >= MAX_WAITING_LIST_LIMIT) {
                    throw new SeatUnavailableException(String.format(
                            "Seats full and Waiting List capacity (%d) reached on train %s.",
                            MAX_WAITING_LIST_LIMIT, train.getNumber()));
                }
                wlNumber = queue.size() + 1;
            }
        }

        // Polymorphic passenger creation (CO-1 / CO-2)
        Passenger passenger;
        if (passengerAge >= SeniorPassenger.MIN_SENIOR_AGE) {
            passenger = new SeniorPassenger(passengerName, passengerAge);
        } else {
            passenger = new Passenger(passengerName, passengerAge);
        }

        String pnr = generatePnr();

        // Polymorphic ticket creation (CO-1 / CO-2)
        Ticket ticket;
        if (isTatkal) {
            ticket = new TatkalTicket(pnr, passenger, train, seatCount, ticketClass);
        } else {
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
        }

        double totalFare = ticket.calculateFare();

        // Payment Simulation via interface (CO-2, PrO-5)
        boolean paymentSuccess = paymentService.pay(totalFare);
        if (!paymentSuccess) {
            throw new RailwayException("Payment failed or was declined. Booking was NOT confirmed and seats were NOT reserved.");
        }

        Booking booking;
        if (seatsAvailable) {
            // Synchronized seat decrement
            train.reserveSeats(seatCount);
            booking = new Booking(pnr, passenger, train, seatCount, ticketClass, ticket, totalFare,
                    LocalDateTime.now(), BookingStatus.CONFIRMED, 0, effectiveQuota);
        } else {
            // Enqueue onto Waiting List Queue (CO-4, FIFO Queue)
            booking = new Booking(pnr, passenger, train, seatCount, ticketClass, ticket, totalFare,
                    LocalDateTime.now(), BookingStatus.WAITING_LIST, wlNumber, effectiveQuota);
            synchronized (allocationLock) {
                Queue<Booking> queue = trainWaitingQueues.computeIfAbsent(train.getNumber(), k -> new LinkedList<>());
                queue.add(booking);
            }
        }

        // Push to Stack (CO-4, LUO-4a)
        recentActionsStack.push(booking);

        // Save to persistence
        bookingRepository.save(booking);
        return booking;
    }

    public Booking getBooking(String pnr) throws BookingNotFoundException, InvalidInputException {
        if (pnr == null || pnr.trim().isEmpty()) {
            throw new InvalidInputException("PNR cannot be blank.");
        }
        Booking booking = bookingRepository.findByPnr(pnr.trim());
        if (booking == null) {
            throw new BookingNotFoundException("No booking record found for PNR: " + pnr.trim().toUpperCase());
        }
        return booking;
    }

    /**
     * Cancels a confirmed booking, restores train seats, and automatically promotes
     * any waiting list passenger who can be accommodated.
     */
    public Booking cancelBooking(String pnr) throws RailwayException {
        Booking booking = getBooking(pnr);

        if (booking.isCancelled()) {
            throw new InvalidInputException("Booking " + booking.getPnr() + " is already cancelled. Cannot cancel twice.");
        }

        boolean wasConfirmed = booking.isConfirmed();
        Train train = booking.getTrain();

        // Mark cancelled
        booking.cancel();
        recentActionsStack.push(booking);

        if (wasConfirmed) {
            // Release seats back to train
            train.releaseSeats(booking.getSeatCount());

            // Check Waiting List Queue for auto-promotion (CO-4, Queue FIFO processing)
            promoteEligibleWaitingListPassengers(train);
        } else {
            // Remove from waiting queue if it was in waiting list
            synchronized (allocationLock) {
                Queue<Booking> queue = trainWaitingQueues.get(train.getNumber());
                if (queue != null) {
                    queue.remove(booking);
                    // Re-index remaining waiting list ranks
                    int rank = 1;
                    for (Booking wb : queue) {
                        wb.setWaitingListNumber(rank++);
                        bookingRepository.update(wb);
                    }
                }
            }
        }

        bookingRepository.update(booking);

        // Interthread communication: notify worker threads of seat changes
        synchronized (allocationLock) {
            allocationLock.notifyAll();
        }

        return booking;
    }

    private void promoteEligibleWaitingListPassengers(Train train) {
        synchronized (allocationLock) {
            Queue<Booking> queue = trainWaitingQueues.get(train.getNumber());
            if (queue == null || queue.isEmpty()) {
                return;
            }

            Booking candidate = queue.peek();
            if (candidate != null && train.hasAvailableSeats(candidate.getSeatCount())) {
                try {
                    queue.poll(); // Dequeue from FIFO Waiting List
                    train.reserveSeats(candidate.getSeatCount());
                    candidate.promoteToConfirmed();
                    bookingRepository.update(candidate);

                    // Re-index remaining waiting list
                    int rank = 1;
                    for (Booking wb : queue) {
                        wb.setWaitingListNumber(rank++);
                        bookingRepository.update(wb);
                    }

                    System.out.printf("[Auto-Promotion] Waiting List PNR %s (%s) has been PROMOTED to CONFIRMED!%n",
                            candidate.getPnr(), candidate.getPassenger().getName());

                } catch (Exception ex) {
                    System.err.println("[Promotion Warning] Unable to auto-promote: " + ex.getMessage());
                }
            }
        }
    }

    public synchronized Booking peekWaitingList(String trainNumber) {
        Queue<Booking> queue = trainWaitingQueues.get(trainNumber);
        return (queue != null) ? queue.peek() : null;
    }

    public synchronized Booking pollWaitingList(String trainNumber) {
        Queue<Booking> queue = trainWaitingQueues.get(trainNumber);
        return (queue != null) ? queue.poll() : null;
    }

    public synchronized List<Booking> getWaitingList(String trainNumber) {
        Queue<Booking> queue = trainWaitingQueues.get(trainNumber);
        if (queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(queue);
    }

    public Stack<Booking> getRecentActionsStack() {
        return recentActionsStack;
    }

    public List<Booking> getBookingHistory() {
        return bookingRepository.findAll();
    }
}
