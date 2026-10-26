package railway.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import railway.exception.RailwayException;
import railway.model.Booking;
import railway.model.Train;
import railway.service.BookingService;
import railway.service.TrainService;

/**
 * Manages the Local Autonomous AI Passenger Swarm.
 * Simulates high-concurrency 10:00 AM / 11:00 AM IRCTC Tatkal Rush.
 *
 * Specific Concurrency Spec:
 * - Minimum at a time: >15 AI passengers (at least 16 bots)
 * - Dynamic formula: Math.max(16, ceil(trainSeats / 2.0)) -> e.g. 40 seats = 20 bots!
 * - Exclusively books on TATKAL Quota with authentic surcharges and TQWL queueing.
 */
public class AiSwarmManager {
    private static final int MAX_HISTORY_EVENTS = 120;
    private static final Random RNG = new Random();

    private final TrainService trainService;
    private final BookingService bookingService;
    private final ConcurrentLinkedDeque<AiEvent> eventLog = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<String> botPnrPool = new ConcurrentLinkedDeque<>();
    private final AtomicLong eventSequence = new AtomicLong(1);

    // 48 worker threads to handle simultaneous bursts of 20-30+ concurrent bots without queuing
    private final ExecutorService workerPool = Executors.newFixedThreadPool(48);
    private ScheduledExecutorService scheduler;
    private final AtomicBoolean isSwarmActive = new AtomicBoolean(false);
    private String swarmSpeedMode = "NORMAL"; // OFF, NORMAL, RUSH

    public AiSwarmManager(TrainService trainService, BookingService bookingService) {
        this.trainService = trainService;
        this.bookingService = bookingService;

        // Seed initial greeting event
        recordEvent(new AiEvent(
            eventSequence.getAndIncrement(),
            "SYSTEM_INFO",
            "IRCTC AI Swarm Engine",
            0,
            "ALL",
            "National Train Grid",
            "SYSTEM",
            "GENERAL",
            0,
            "",
            "Autonomous AI Passenger Swarm initialized. Tatkal Rush concurrency engine armed (>15 bots, 50% seat quota)."
        ));
    }

    /**
     * Calculates the realistic Tatkal rush bot count for a given train:
     * - Minimum at a time: strictly > 15 bots (at least 16).
     * - Proportional: exactly half (50%) of train seats (e.g. 40 seats -> 20 bots; 50 seats -> 25 bots).
     */
    public int calculateTatkalBotCount(String trainNumber) {
        Train t = trainService.getTrainByNumber(trainNumber);
        if (t == null) return 16;
        int seats = t.getAvailableSeats() > 0 ? t.getAvailableSeats() : t.getTotalSeats();
        int halfSeats = (int) Math.ceil(seats / 2.0);
        return Math.max(16, halfSeats);
    }

    /**
     * Starts continuous background simulation of passenger bookings and cancellations.
     */
    public synchronized void startContinuousSwarm(String speedMode) {
        stopContinuousSwarm();

        if ("OFF".equalsIgnoreCase(speedMode)) {
            this.swarmSpeedMode = "OFF";
            this.isSwarmActive.set(false);
            return;
        }

        this.swarmSpeedMode = speedMode.toUpperCase();
        this.isSwarmActive.set(true);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        long periodMs = "RUSH".equalsIgnoreCase(speedMode) ? 1200 : 3500;

        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (isSwarmActive.get()) {
                    performAutonomousStep();
                }
            } catch (Throwable t) {
                System.err.println("[AI Swarm Error] " + t.getMessage());
            }
        }, 1000, periodMs, TimeUnit.MILLISECONDS);

        recordEvent(new AiEvent(
            eventSequence.getAndIncrement(),
            "SYSTEM_INFO",
            "IRCTC AI Swarm Engine",
            0,
            "ALL",
            "National Grid",
            "INFO",
            "GENERAL",
            0,
            "",
            "Swarm mode set to " + this.swarmSpeedMode + ". Simulating real passengers at ~" + (periodMs / 1000.0) + "s interval."
        ));
    }

    public synchronized void stopContinuousSwarm() {
        isSwarmActive.set(false);
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    /**
     * Executes one autonomous background action (booking or cancellation).
     */
    private void performAutonomousStep() {
        // 15% chance of cancellation if we have existing bot PNRs
        if (RNG.nextDouble() < 0.15 && !botPnrPool.isEmpty()) {
            String pnrToCancel = botPnrPool.poll();
            if (pnrToCancel != null) {
                simulateAiCancellation(pnrToCancel);
                return;
            }
        }

        List<Train> trains = trainService.getAllTrains();
        if (trains.isEmpty()) return;

        Train target = trains.get(RNG.nextInt(trains.size()));
        // 40% chance of background bot booking Tatkal quota
        boolean tatkal = RNG.nextDouble() < 0.40;
        AiPassengerBot bot = new AiPassengerBot(tatkal);
        workerPool.submit(() -> executeBookingTask(bot, target.getNumber(), bot.getQuota()));
    }

    /**
     * Triggers a realistic high-concurrency Tatkal Rush:
     * - Minimum at a time: >15 AI passengers (at least 16 bots)
     * - Proportional to capacity: If 40 seats, sends half (20 bots); if 50 seats, sends 25 bots
     * - Exclusively books on TATKAL Quota with authentic Tatkal pricing and TQWL queues
     */
    public void triggerTatkalRush(String targetTrainNumber, int requestedCount) {
        String trainNum = targetTrainNumber;
        if (trainNum == null || trainNum.trim().isEmpty()) {
            List<Train> trains = trainService.getAllTrains();
            if (trains.isEmpty()) return;
            trainNum = trains.get(0).getNumber();
        }

        final String finalTrainNum = trainNum.trim().toUpperCase();
        Train train = trainService.getTrainByNumber(finalTrainNum);
        String trainName = train != null ? train.getName() : "Express";

        // Calculate dynamic bot count: minimum >15 bots, and half of seats (e.g. 40 seats -> 20 bots)
        int calculatedCount = calculateTatkalBotCount(finalTrainNum);
        int finalBotCount = (requestedCount > 15) ? Math.max(requestedCount, calculatedCount) : calculatedCount;

        recordEvent(new AiEvent(
            eventSequence.getAndIncrement(),
            "TATKAL_RUSH_START",
            "Tatkal Controller (10:00 AM Rush)",
            0,
            finalTrainNum,
            trainName,
            "TATKAL",
            "TATKAL",
            finalBotCount,
            "",
            String.format("⚡ 10:00 AM TATKAL RUSH LIVE! Dispatching %d AI passengers (50%% of capacity, min >15) exclusively on TATKAL quota for train %s!",
                    finalBotCount, finalTrainNum)
        ));

        // Submit all 16 to 25+ bots to thread pool concurrently with micro-jitter
        for (int i = 0; i < finalBotCount; i++) {
            AiPassengerBot bot = new AiPassengerBot(true); // Exclusively Tatkal quota
            workerPool.submit(() -> {
                // Realistic human click latency jitter: 15ms to 450ms spread
                try {
                    Thread.sleep(15 + RNG.nextInt(435));
                } catch (InterruptedException ignored) {}
                executeBookingTask(bot, finalTrainNum, "TATKAL");
            });
        }
    }

    private void executeBookingTask(AiPassengerBot bot, String trainNumber, String quota) {
        Train train = trainService.getTrainByNumber(trainNumber);
        String trainName = train != null ? train.getName() : "Train " + trainNumber;
        boolean isTatkal = "TATKAL".equalsIgnoreCase(quota);

        try {
            Booking booking = bookingService.bookTicket(
                trainNumber,
                bot.getName(),
                bot.getAge(),
                bot.getSeatsToBook(),
                bot.getPreferredClass(),
                quota
            );

            if (booking.isWaitingList()) {
                String wlRank = isTatkal ? ("TQWL-" + booking.getWaitingListNumber()) : ("WL-" + booking.getWaitingListNumber());
                recordEvent(new AiEvent(
                    eventSequence.getAndIncrement(),
                    "WAITING_LIST",
                    bot.getName(),
                    bot.getAge(),
                    trainNumber,
                    trainName,
                    bot.getPreferredClass().name(),
                    quota,
                    bot.getSeatsToBook(),
                    booking.getPnr(),
                    isTatkal
                        ? ("⚡ [TATKAL FULL] All Tatkal berths booked! Queued on Tatkal Waiting List: " + wlRank)
                        : ("Train seats full! Assigned Waiting List rank: " + wlRank)
                ));
            } else {
                botPnrPool.offer(booking.getPnr());
                String detail = isTatkal
                    ? String.format("⚡ [TATKAL CONFIRMED] Booked %d Tatkal %s berth(s) (₹%.2f incl. Tatkal Surcharge)",
                            booking.getSeatCount(), bot.getPreferredClass().name(), booking.getFare())
                    : String.format("%sConfirmed %d seat(s) in %s (₹%.2f)",
                            (bot.isSenior() ? "Senior 40% discount applied. " : ""),
                            booking.getSeatCount(), bot.getPreferredClass().name(), booking.getFare());

                recordEvent(new AiEvent(
                    eventSequence.getAndIncrement(),
                    "BOOKING_CONFIRMED",
                    bot.getName(),
                    bot.getAge(),
                    trainNumber,
                    trainName,
                    bot.getPreferredClass().name(),
                    quota,
                    bot.getSeatsToBook(),
                    booking.getPnr(),
                    detail
                ));
            }
        } catch (RailwayException ex) {
            recordEvent(new AiEvent(
                eventSequence.getAndIncrement(),
                "BOOKING_FAILED",
                bot.getName(),
                bot.getAge(),
                trainNumber,
                trainName,
                bot.getPreferredClass().name(),
                quota,
                bot.getSeatsToBook(),
                "",
                "Booking declined: " + ex.getMessage()
            ));
        } catch (Exception ex) {
            System.err.println("[AI Booking Error] " + ex.getMessage());
        }
    }

    private void simulateAiCancellation(String pnr) {
        try {
            Booking cancelled = bookingService.cancelBooking(pnr);
            recordEvent(new AiEvent(
                eventSequence.getAndIncrement(),
                "CANCELLED",
                cancelled.getPassenger().getName(),
                cancelled.getPassenger().getAge(),
                cancelled.getTrain().getNumber(),
                cancelled.getTrain().getName(),
                cancelled.getTicket().getTicketClass().name(),
                cancelled.getQuota(),
                cancelled.getSeatCount(),
                cancelled.getPnr(),
                "Passenger cancelled " + cancelled.getQuota() + " ticket. Berth released! Waiting List auto-promotion evaluated."
            ));
        } catch (Exception ex) {
            // Ignore if already cancelled
        }
    }

    private void recordEvent(AiEvent event) {
        eventLog.addFirst(event);
        while (eventLog.size() > MAX_HISTORY_EVENTS) {
            eventLog.pollLast();
        }
    }

    public List<AiEvent> getRecentEvents(int limit) {
        List<AiEvent> result = new ArrayList<>();
        int count = 0;
        for (AiEvent e : eventLog) {
            result.add(e);
            count++;
            if (count >= limit) break;
        }
        return Collections.unmodifiableList(result);
    }

    public String getFeedJson(int limit) {
        List<AiEvent> events = getRecentEvents(limit);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < events.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(events.get(i).toJson());
        }
        sb.append("]");
        return sb.toString();
    }

    public boolean isSwarmActive() { return isSwarmActive.get(); }
    public String getSwarmSpeedMode() { return swarmSpeedMode; }

    public void shutdown() {
        stopContinuousSwarm();
        workerPool.shutdown();
    }
}
