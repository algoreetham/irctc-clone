package railway.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;
import railway.exception.InvalidInputException;
import railway.model.TicketClass;
import railway.model.Train;

/**
 * Service managing train searches, route matching, and availability inquiries.
 * Demonstrates:
 * - Hashtable for thread-safe key-value indexing (CO-4, PrO-11)
 * - Java 8+ Streams and Lambda Expressions (CO-4, LUO-4c)
 * - Method Overloading (CO-2, PrO-4)
 */
public class TrainService {
    private final List<Train> trainList = new ArrayList<>();
    // Demonstrates Hashtable as required by PrO-11 / LUO-4b
    private final Hashtable<String, Train> trainHashtable = new Hashtable<>();

    public TrainService() {
        seedSampleData();
    }

    private void seedSampleData() {
        addTrain(new Train("DEMO101", "Campus Express", "Bengaluru", "Chennai",
                "06:00 AM", "11:30 AM", 50, 250.0));
        addTrain(new Train("DEMO202", "Student Superfast", "Bengaluru", "Mysuru",
                "07:15 AM", "10:00 AM", 40, 150.0));
        addTrain(new Train("DEMO303", "Project Intercity", "Chennai", "Hyderabad",
                "05:30 AM", "02:00 PM", 35, 450.0));
        addTrain(new Train("DEMO404", "Learning Express", "Mysuru", "Bengaluru",
                "04:00 PM", "06:45 PM", 45, 150.0));
    }

    public synchronized void addTrain(Train train) {
        if (train != null) {
            trainList.add(train);
            trainHashtable.put(train.getNumber().toUpperCase(), train);
        }
    }

    public List<Train> getAllTrains() {
        // Lambda sorting by train number
        return trainList.stream()
                .sorted(Comparator.comparing(Train::getNumber))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
    }

    /**
     * Finds a train in O(1) time using Hashtable.
     * Demonstrates PrO-11 (Managing Hash table for storing and accessing data).
     */
    public Train getTrainByNumber(String trainNumber) {
        if (trainNumber == null) {
            return null;
        }
        return trainHashtable.get(trainNumber.trim().toUpperCase());
    }

    /**
     * Searches for trains matching source and destination.
     * Demonstrates Lambda Expressions, Streams (LUO-4c), and Method Overloading (PrO-4).
     */
    public List<Train> searchTrains(String source, String destination) throws InvalidInputException {
        validateRoute(source, destination);

        // Demonstrates Java Lambda Expressions & Stream filtering
        return trainList.stream()
                .filter(train -> train.matchesRoute(source, destination))
                .sorted(Comparator.comparingDouble(Train::getBaseFare))
                .collect(Collectors.toList());
    }

    /**
     * Searches for trains matching source and destination with a specific ticket class preference.
     * Demonstrates Method Overloading (Signature 2).
     */
    public List<Train> searchTrains(String source, String destination, TicketClass ticketClass) throws InvalidInputException {
        List<Train> baseMatches = searchTrains(source, destination);
        if (ticketClass == null) {
            return baseMatches;
        }

        // Demonstrates Lambda filter for seat availability
        return baseMatches.stream()
                .filter(t -> t.getAvailableSeats() > 0)
                .collect(Collectors.toList());
    }

    private void validateRoute(String source, String destination) throws InvalidInputException {
        if (source == null || source.trim().isEmpty()) {
            throw new InvalidInputException("Source station cannot be empty or blank.");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new InvalidInputException("Destination station cannot be empty or blank.");
        }
        if (source.trim().equalsIgnoreCase(destination.trim())) {
            throw new InvalidInputException("Source and destination stations cannot be identical.");
        }
    }
}
