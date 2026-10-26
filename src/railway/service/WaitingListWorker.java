package railway.service;

import railway.model.Booking;
import railway.model.Train;
import railway.repository.BookingRepository;

/**
 * Background worker thread demonstrating Multithreading, Thread Lifecycle,
 * Interthread Communication (wait/notify), and Synchronization (CO-3, PrO-8, PrO-9).
 * Monitors seat cancellations and promotes passengers from Waiting List queues.
 */
public class WaitingListWorker extends Thread {
    private final Object monitorLock;
    private final BookingService bookingService;
    private final BookingRepository bookingRepository;
    private volatile boolean running = true;

    public WaitingListWorker(Object monitorLock, BookingService bookingService, BookingRepository bookingRepository) {
        super("WaitingList-AllocationWorker");
        this.monitorLock = monitorLock;
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
        setDaemon(true); // Allows clean JVM shutdown
    }

    @Override
    public void run() {
        System.out.println("[Thread Lifecycle] " + getName() + " started in background (State: RUNNABLE).");
        while (running) {
            try {
                synchronized (monitorLock) {
                    // Interthread communication: wait until notified by a cancellation event
                    monitorLock.wait(5000); // 5-second timeout or wake on notify
                }

                if (!running) break;

                // Process pending promotions across waiting queues
                processPendingWaitingList();

            } catch (InterruptedException e) {
                System.out.println("[Thread Lifecycle] " + getName() + " interrupted, shutting down.");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                System.err.println("[Thread Warning] Error during waiting list worker cycle: " + ex.getMessage());
            }
        }
        System.out.println("[Thread Lifecycle] " + getName() + " terminated cleanly (State: TERMINATED).");
    }

    /**
     * Synchronized promotion logic checking waiting queues against current train availability.
     */
    private void processPendingWaitingList() {
        if (bookingService == null) return;

        for (Train train : bookingService.getTrainService().getAllTrains()) {
            synchronized (train) {
                Booking waitingPassenger = bookingService.peekWaitingList(train.getNumber());
                if (waitingPassenger != null && train.hasAvailableSeats(waitingPassenger.getSeatCount())) {
                    try {
                        Booking promoted = bookingService.pollWaitingList(train.getNumber());
                        if (promoted != null) {
                            train.reserveSeats(promoted.getSeatCount());
                            promoted.promoteToConfirmed();
                            bookingRepository.update(promoted);
                            System.out.printf("[Worker Thread Promotion] Auto-promoted PNR %s (%s) to CONFIRMED! Seats remaining on %s: %d%n",
                                    promoted.getPnr(), promoted.getPassenger().getName(),
                                    train.getNumber(), train.getAvailableSeats());
                        }
                    } catch (Exception ex) {
                        System.err.println("[Worker Error] Failed to auto-promote waiting passenger: " + ex.getMessage());
                    }
                }
            }
        }
    }

    public void shutdown() {
        this.running = false;
        synchronized (monitorLock) {
            monitorLock.notifyAll(); // Wake up thread if waiting
        }
    }
}
