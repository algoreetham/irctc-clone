package railway.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import railway.model.Booking;
import railway.model.Train;
import railway.service.BookingService;
import railway.service.TrainService;

/**
 * Local Natural Language Processing Chatbot Service for "Ask DISHA 2.0".
 * 100% self-contained local AI assistant (no external API keys or cloud dependencies).
 * Provides conversational assistance for train searches, seat availability, PNR inquiries,
 * concession rules, and AI Swarm / Tatkal simulation triggers.
 */
public class DishaChatbotService {
    private final TrainService trainService;
    private final BookingService bookingService;
    private final AiSwarmManager swarmManager;

    public DishaChatbotService(TrainService trainService, BookingService bookingService, AiSwarmManager swarmManager) {
        this.trainService = trainService;
        this.bookingService = bookingService;
        this.swarmManager = swarmManager;
    }

    public static class ChatResponse {
        public final String reply;
        public final String intent;
        public final String actionType;
        public final String actionPayload;
        public final List<String> suggestions;

        public ChatResponse(String reply, String intent, String actionType, String actionPayload, List<String> suggestions) {
            this.reply = reply;
            this.intent = intent;
            this.actionType = actionType;
            this.actionPayload = actionPayload;
            this.suggestions = suggestions;
        }

        public String toJson() {
            StringBuilder sb = new StringBuilder("{");
            sb.append("\"reply\":\"").append(escape(reply)).append("\",");
            sb.append("\"intent\":\"").append(escape(intent)).append("\",");
            sb.append("\"actionType\":\"").append(escape(actionType != null ? actionType : "")).append("\",");
            sb.append("\"actionPayload\":").append(actionPayload != null && !actionPayload.isEmpty() ? actionPayload : "null").append(",");
            sb.append("\"suggestions\":[");
            for (int i = 0; i < suggestions.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escape(suggestions.get(i))).append("\"");
            }
            sb.append("]}");
            return sb.toString();
        }

        private static String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");
        }
    }

    /**
     * Processes a user chat message and returns an intelligent response with interactive suggestions.
     */
    public ChatResponse processMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return new ChatResponse(
                "Namaste! I am Ask DISHA 2.0, your Indian Railways virtual assistant. How may I assist your journey today?",
                "GREETING",
                null,
                null,
                Arrays.asList("Bengaluru to Chennai trains", "Check PNR Status", "Start Tatkal Rush", "Senior Citizen Discount")
            );
        }

        String msg = rawMessage.toLowerCase().trim();

        // 1. GREETINGS
        if (msg.matches("^(hi|hello|hey|namaste|greetings|good (morning|afternoon|evening)|disha).*")) {
            return new ChatResponse(
                "Namaste! 🙏 Welcome to IRCTC Next Generation e-Ticketing System. I am **Ask DISHA 2.0**, your AI assistant powered by local intelligent processing.\n\nYou can ask me to search trains, check real-time seat availability, inspect PNR status, or unleash the **AI Passenger Swarm** to test concurrent Tatkal rush!",
                "GREETING",
                null,
                null,
                Arrays.asList("Bengaluru to Chennai trains", "Check PNR Status", "Trigger Tatkal Rush on DEMO101", "Senior Citizen Concession")
            );
        }

        // 2. PNR STATUS LOOKUP
        Pattern pnrPattern = Pattern.compile("(pnr[- ]?\\d{4,6})", Pattern.CASE_INSENSITIVE);
        Matcher pnrMatcher = pnrPattern.matcher(msg);
        if (pnrMatcher.find() || msg.contains("pnr") || msg.contains("booking status")) {
            if (pnrMatcher.find(0)) {
                String rawPnr = pnrMatcher.group(1).toUpperCase().replace(" ", "-");
                if (!rawPnr.startsWith("PNR-")) {
                    rawPnr = "PNR-" + rawPnr.replace("PNR", "").trim();
                }
                try {
                    Booking b = bookingService.getBooking(rawPnr);
                    String statusText = b.isConfirmed() ? "✅ CONFIRMED" : (b.isWaitingList() ? "⏳ WAITING LIST (Rank: WL-" + b.getWaitingListNumber() + ")" : "❌ CANCELLED");
                    String reply = String.format(
                        "### 🎫 PNR Inquiry Result: `%s`\n\n" +
                        "- **Passenger:** %s (Age: %d)\n" +
                        "- **Train:** %s - %s\n" +
                        "- **Route:** %s ➔ %s\n" +
                        "- **Class:** %s | **Seats Booked:** %d\n" +
                        "- **Current Status:** %s\n" +
                        "- **Fare Paid:** ₹%.2f\n\n" +
                        "You can view or cancel this ticket using the Booking History tab.",
                        b.getPnr(), b.getPassenger().getName(), b.getPassenger().getAge(),
                        b.getTrain().getNumber(), b.getTrain().getName(),
                        b.getTrain().getSource(), b.getTrain().getDestination(),
                        b.getTicket().getTicketClass().name(), b.getSeatCount(),
                        statusText, b.getFare()
                    );
                    return new ChatResponse(reply, "PNR_RESULT", "SHOW_PNR", "{\"pnr\":\"" + b.getPnr() + "\"}",
                        Arrays.asList("Check another PNR", "Book another train", "Start Tatkal Rush"));
                } catch (Exception e) {
                    return new ChatResponse(
                        "🔍 I checked the railway reservation records, but no booking was found for `" + rawPnr + "`. Please verify your 10-digit/alpha PNR number.",
                        "PNR_NOT_FOUND",
                        null,
                        null,
                        Arrays.asList("Search all trains", "Check latest booking", "Bengaluru to Mysuru trains")
                    );
                }
            } else {
                return new ChatResponse(
                    "To check your reservation status, please tell me your PNR number (e.g., `PNR-1001` or `Check status of PNR-1002`).",
                    "PNR_PROMPT",
                    null,
                    null,
                    Arrays.asList("Status of PNR-1001", "Status of PNR-1002", "View My Bookings")
                );
            }
        }

        // 3. TATKAL RUSH / AI SWARM SIMULATION
        if (msg.contains("tatkal") || msg.contains("rush") || msg.contains("swarm") || msg.contains("bots") || msg.contains("simulate") || msg.contains("compete")) {
            // Find train number if specified
            String targetTrain = "DEMO101";
            for (Train t : trainService.getAllTrains()) {
                if (msg.contains(t.getNumber().toLowerCase())) {
                    targetTrain = t.getNumber();
                    break;
                }
            }

            int botCount = swarmManager.calculateTatkalBotCount(targetTrain);
            swarmManager.triggerTatkalRush(targetTrain, botCount);
            Train train = trainService.getTrainByNumber(targetTrain);
            String name = train != null ? train.getName() : "Express";

            String reply = String.format(
                "⚡ **10:00 AM IRCTC TATKAL RUSH SIMULATION TRIGGERED!**\n\n" +
                "I have dispatched **%d autonomous AI passengers** (proportional 50%% capacity, strictly >15 bots) hitting train **%s - %s** simultaneously on **TATKAL QUOTA**!\n\n" +
                "**Realistic Concurrency Highlights:**\n" +
                "- All %d bots are booking under official **TATKAL Quota** with authentic dynamic surcharges.\n" +
                "- Rapid berth decrement: available berths will drop instantly within 500ms.\n" +
                "- Excess passengers automatically queue into **Tatkal Waiting List (TQWL)**!\n\n" +
                "Watch the **Live AI Activity Feed** update in real-time!",
                botCount, targetTrain, name, botCount
            );

            return new ChatResponse(
                reply,
                "TATKAL_TRIGGERED",
                "REFRESH_FEED",
                "{\"trainNumber\":\"" + targetTrain + "\"}",
                Arrays.asList("Trigger another Tatkal rush on " + targetTrain, "Check " + targetTrain + " availability", "How does TQWL work?")
            );
        }

        // 4. ROUTE & TRAIN SEARCH
        String detectedSource = null;
        String detectedDest = null;

        String[] cities = {"bengaluru", "bangalore", "chennai", "mysuru", "mysore", "hyderabad", "delhi", "mumbai"};
        for (String c : cities) {
            if (msg.contains("from " + c) || (msg.contains(c) && detectedSource == null && (msg.contains("to") || msg.contains("->")))) {
                if (detectedSource == null) detectedSource = normalizeCity(c);
            } else if (msg.contains("to " + c) || (detectedSource != null && msg.contains(c))) {
                if (detectedDest == null && !normalizeCity(c).equalsIgnoreCase(detectedSource)) {
                    detectedDest = normalizeCity(c);
                }
            }
        }

        // Direct common routes shortcut
        if (detectedSource == null && detectedDest == null) {
            if (msg.contains("chennai")) {
                detectedSource = "Bengaluru";
                detectedDest = "Chennai";
            } else if (msg.contains("mysuru") || msg.contains("mysore")) {
                detectedSource = "Bengaluru";
                detectedDest = "Mysuru";
            } else if (msg.contains("hyderabad")) {
                detectedSource = "Chennai";
                detectedDest = "Hyderabad";
            }
        }

        if (detectedSource != null && detectedDest != null) {
            try {
                List<Train> results = trainService.searchTrains(detectedSource, detectedDest);
                if (results.isEmpty()) {
                    return new ChatResponse(
                        String.format("I found no direct trains connecting **%s** to **%s** in our current schedule. Please try Bengaluru ➔ Chennai or Bengaluru ➔ Mysuru.", detectedSource, detectedDest),
                        "SEARCH_NO_RESULTS",
                        null,
                        null,
                        Arrays.asList("Bengaluru to Chennai trains", "Bengaluru to Mysuru trains", "Show all trains")
                    );
                }

                StringBuilder sb = new StringBuilder();
                sb.append(String.format("🚆 **Found %d train(s) from %s to %s:**\n\n", results.size(), detectedSource, detectedDest));
                for (Train t : results) {
                    sb.append(String.format("- **%s | %s**\n  Depart: `%s` ➔ Arrive: `%s`\n  Available Seats: **%d** | Base Fare: ₹%.2f\n\n",
                        t.getNumber(), t.getName(), t.getDepartureTime(), t.getArrivalTime(), t.getAvailableSeats(), t.getBaseFare()));
                }
                sb.append("Click below to auto-fill your booking search!");

                String payload = String.format("{\"source\":\"%s\",\"destination\":\"%s\"}", detectedSource, detectedDest);
                return new ChatResponse(sb.toString(), "TRAIN_SEARCH", "APPLY_SEARCH", payload,
                    Arrays.asList("Trigger Tatkal Rush on " + results.get(0).getNumber(), "Check waiting list for " + results.get(0).getNumber(), "Senior Citizen Discount"));
            } catch (Exception e) {
                // fall through
            }
        }

        // 5. SEAT AVAILABILITY CHECK
        for (Train t : trainService.getAllTrains()) {
            if (msg.contains(t.getNumber().toLowerCase()) || msg.contains(t.getName().toLowerCase())) {
                List<Booking> wl = bookingService.getWaitingList(t.getNumber());
                String availText = t.getAvailableSeats() > 0
                    ? "🟢 **AVAILABLE - " + String.format("%04d", t.getAvailableSeats()) + " Seats**"
                    : "🔴 **SEATS FULL** (Waiting List Queue: " + wl.size() + " passengers)";

                String reply = String.format(
                    "📊 **Live Status for %s | %s**\n\n" +
                    "- **Route:** %s to %s\n" +
                    "- **Departure:** %s | **Arrival:** %s\n" +
                    "- **Current Status:** %s\n" +
                    "- **Base Fare:** ₹%.2f\n" +
                    "- **Waiting List Depth:** %d / 100 max\n\n" +
                    "Would you like to book a seat or simulate concurrent demand?",
                    t.getNumber(), t.getName(), t.getSource(), t.getDestination(),
                    t.getDepartureTime(), t.getArrivalTime(), availText, t.getBaseFare(), wl.size()
                );

                return new ChatResponse(reply, "TRAIN_STATUS", "SELECT_TRAIN", "{\"trainNumber\":\"" + t.getNumber() + "\"}",
                    Arrays.asList("Book on " + t.getNumber(), "⚡ Tatkal Rush on " + t.getNumber(), "Check trains to Chennai"));
            }
        }

        // 6. SENIOR CITIZEN & CONCESSIONS
        if (msg.contains("senior") || msg.contains("discount") || msg.contains("concession") || msg.contains("age") || msg.contains("elderly")) {
            return new ChatResponse(
                "👵 **Senior Citizen Concession Rules (Alliance University CDAP - CO-2):**\n\n" +
                "1. **Eligibility:** Passengers aged **60 years and above** automatically qualify for Senior Citizen concession.\n" +
                "2. **Discount Rate:** **40% concession** is deducted from the base fare across all ticket classes (General, Sleeper, and AC).\n" +
                "3. **Implementation:** Powered by Object-Oriented Polymorphism via Java inheritance (`SeniorPassenger extends Passenger`).\n" +
                "4. Simply enter your age as 60 or above during booking to see the discount applied automatically!",
                "CONCESSION_INFO",
                null,
                null,
                Arrays.asList("Bengaluru to Chennai trains", "Simulate 5 AI bookings", "Check PNR Status")
            );
        }

        // 7. WAITING LIST & AUTO-PROMOTION EXPLANATION
        if (msg.contains("waiting list") || msg.contains("wl") || msg.contains("rac") || msg.contains("promotion") || msg.contains("cancellation")) {
            return new ChatResponse(
                "📋 **Waiting List & Auto-Promotion Engine (CO-4 & PrO-8/9):**\n\n" +
                "- When all train seats are reserved, new bookings are assigned FIFO Waiting List positions (`WL-1`, `WL-2`, etc.) up to a capacity of **100**.\n" +
                "- **Automatic Promotion:** When any confirmed passenger cancels their booking, our background daemon (`WaitingListWorker`) uses `wait()` and `notifyAll()` synchronization locks to instantly promote the highest-priority waiting passenger to **CONFIRMED**!\n" +
                "- Try cancelling any confirmed ticket in the Booking History tab to witness auto-promotion live!",
                "WL_INFO",
                null,
                null,
                Arrays.asList("Trigger Tatkal Rush on DEMO101", "View Booking History", "Senior Citizen Concession")
            );
        }

        // 8. LIST ALL TRAINS
        if (msg.contains("all trains") || msg.contains("list trains") || msg.contains("show trains") || msg.contains("schedule")) {
            List<Train> trains = trainService.getAllTrains();
            StringBuilder sb = new StringBuilder("🚆 **Indian Railways Grid Schedule:**\n\n");
            for (Train t : trains) {
                sb.append(String.format("- **%s | %s**: %s ➔ %s (%s to %s) | **%d seats** @ ₹%.2f\n",
                    t.getNumber(), t.getName(), t.getSource(), t.getDestination(),
                    t.getDepartureTime(), t.getArrivalTime(), t.getAvailableSeats(), t.getBaseFare()));
            }
            return new ChatResponse(sb.toString(), "ALL_TRAINS", null, null,
                Arrays.asList("Bengaluru to Chennai trains", "⚡ Tatkal Rush on DEMO101", "Senior Citizen Discount"));
        }

        // DEFAULT FALLBACK
        return new ChatResponse(
            "I'm here to help with your train journey! You can search for trains (e.g. *\"Trains from Bengaluru to Chennai\"*), check seat availability (e.g. *\"Availability on DEMO101\"*), look up a PNR (e.g. *\"PNR-1001\"*), or test our concurrent AI Swarm with *\"Start Tatkal Rush\"*.",
            "UNKNOWN",
            null,
            null,
            Arrays.asList("Bengaluru to Chennai trains", "Check PNR-1001", "Start Tatkal Rush", "Senior Citizen Discount")
        );
    }

    private String normalizeCity(String c) {
        if (c.equals("bangalore") || c.equals("bengaluru")) return "Bengaluru";
        if (c.equals("mysore") || c.equals("mysuru")) return "Mysuru";
        if (c.equals("chennai")) return "Chennai";
        if (c.equals("hyderabad")) return "Hyderabad";
        if (c.equals("delhi")) return "New Delhi";
        if (c.equals("mumbai")) return "Mumbai";
        return c.substring(0, 1).toUpperCase() + c.substring(1);
    }
}
