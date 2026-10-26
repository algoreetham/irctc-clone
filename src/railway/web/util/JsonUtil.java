package railway.web.util;

import java.util.List;
import railway.model.Booking;
import railway.model.BookingStatus;
import railway.model.SeniorPassenger;
import railway.model.Train;

/**
 * Lightweight JSON serializer/deserializer with zero external dependencies.
 * Updated to support Waiting List ranks (CO-4 & Project Spec).
 */
public class JsonUtil {

    public static String toJson(Train t) {
        if (t == null) return "null";
        return String.format(
                "{\"number\":\"%s\",\"name\":\"%s\",\"source\":\"%s\",\"destination\":\"%s\"," +
                "\"departureTime\":\"%s\",\"arrivalTime\":\"%s\",\"totalSeats\":%d,\"availableSeats\":%d,\"baseFare\":%.2f}",
                escape(t.getNumber()),
                escape(t.getName()),
                escape(t.getSource()),
                escape(t.getDestination()),
                escape(t.getDepartureTime()),
                escape(t.getArrivalTime()),
                t.getTotalSeats(),
                t.getAvailableSeats(),
                t.getBaseFare()
        );
    }

    public static String trainsToJson(List<Train> trains) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < trains.size(); i++) {
            sb.append(toJson(trains.get(i)));
            if (i < trains.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toJson(Booking b) {
        if (b == null) return "null";
        boolean isSenior = b.getPassenger() instanceof SeniorPassenger;
        String statusLabel = b.getStatusLabel();

        return String.format(
                "{\"pnr\":\"%s\",\"status\":\"%s\",\"statusLabel\":\"%s\",\"waitingListNumber\":%d,\"quota\":\"%s\",\"isTatkal\":%b," +
                "\"fare\":%.2f,\"seatCount\":%d,\"ticketClass\":\"%s\",\"classDisplayName\":\"%s\",\"bookingTime\":\"%s\"," +
                "\"passenger\":{\"name\":\"%s\",\"age\":%d,\"category\":\"%s\",\"isSenior\":%b}," +
                "\"train\":{\"number\":\"%s\",\"name\":\"%s\",\"source\":\"%s\",\"destination\":\"%s\"," +
                "\"departureTime\":\"%s\",\"arrivalTime\":\"%s\",\"availableSeats\":%d}}",
                escape(b.getPnr()),
                escape(b.getStatus().name()),
                escape(statusLabel),
                b.getWaitingListNumber(),
                escape(b.getQuota()),
                b.isTatkal(),
                b.getFare(),
                b.getSeatCount(),
                escape(b.getTicketClass().name()),
                escape(b.getTicketClass().getDisplayName()),
                escape(b.getFormattedBookingTime()),
                escape(b.getPassenger().getName()),
                b.getPassenger().getAge(),
                escape(b.getPassenger().getCategory()),
                isSenior,
                escape(b.getTrain().getNumber()),
                escape(b.getTrain().getName()),
                escape(b.getTrain().getSource()),
                escape(b.getTrain().getDestination()),
                escape(b.getTrain().getDepartureTime()),
                escape(b.getTrain().getArrivalTime()),
                b.getTrain().getAvailableSeats()
        );
    }

    public static String bookingsToJson(List<Booking> bookings) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bookings.size(); i++) {
            sb.append(toJson(bookings.get(i)));
            if (i < bookings.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String toApiResponseJson(boolean success, String message, String dataJson) {
        return String.format("{\"success\":%b,\"message\":\"%s\",\"data\":%s}",
                success, escape(message), (dataJson != null ? dataJson : "null"));
    }

    public static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public static int extractJsonInt(String json, String key, int defaultValue) {
        String pattern = "\"" + key + "\"\\s*:\\s*([0-9]+)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1).trim());
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public static boolean extractJsonBool(String json, String key, boolean defaultValue) {
        String pattern = "\"" + key + "\"\\s*:\\s*(true|false)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return Boolean.parseBoolean(m.group(1).trim());
        }
        return defaultValue;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
