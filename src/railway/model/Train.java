package railway.model;

import railway.exception.InvalidInputException;
import railway.exception.SeatUnavailableException;

/**
 * Represents a train in the railway network.
 * Applies encapsulation to ensure seat reservation and release are atomic and consistent.
 */
public class Train {
    private final String number;
    private final String name;
    private final String source;
    private final String destination;
    private final String departureTime;
    private final String arrivalTime;
    private final int totalSeats;
    private int availableSeats;
    private final double baseFare;

    public Train(String number, String name, String source, String destination,
                 String departureTime, String arrivalTime, int totalSeats, double baseFare) {
        if (number == null || number.trim().isEmpty()) {
            throw new IllegalArgumentException("Train number cannot be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Train name cannot be blank.");
        }
        if (source == null || source.trim().isEmpty()) {
            throw new IllegalArgumentException("Source station cannot be blank.");
        }
        if (destination == null || destination.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination station cannot be blank.");
        }
        if (source.trim().equalsIgnoreCase(destination.trim())) {
            throw new IllegalArgumentException("Source and destination cannot be identical.");
        }
        if (totalSeats <= 0) {
            throw new IllegalArgumentException("Total seats must be greater than zero.");
        }
        if (baseFare <= 0) {
            throw new IllegalArgumentException("Base fare must be positive.");
        }

        this.number = number.trim().toUpperCase();
        this.name = name.trim();
        this.source = source.trim();
        this.destination = destination.trim();
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.totalSeats = totalSeats;
        this.availableSeats = totalSeats;
        this.baseFare = baseFare;
    }

    public String getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public synchronized int getAvailableSeats() {
        return availableSeats;
    }

    public double getBaseFare() {
        return baseFare;
    }

    /**
     * Checks if the train matches the query route (case-insensitive).
     */
    public boolean matchesRoute(String querySource, String queryDestination) {
        if (querySource == null || queryDestination == null) {
            return false;
        }
        return this.source.equalsIgnoreCase(querySource.trim()) &&
               this.destination.equalsIgnoreCase(queryDestination.trim());
    }

    public synchronized boolean hasAvailableSeats(int requestedSeats) {
        return requestedSeats > 0 && requestedSeats <= this.availableSeats;
    }

    /**
     * Safely reserves seats, ensuring seat count does not drop below zero.
     */
    public synchronized void reserveSeats(int requestedSeats) throws SeatUnavailableException, InvalidInputException {
        if (requestedSeats <= 0) {
            throw new InvalidInputException("Requested seat count must be positive.");
        }
        if (requestedSeats > this.availableSeats) {
            throw new SeatUnavailableException("Not enough seats available! Requested: " +
                    requestedSeats + ", Available: " + this.availableSeats);
        }
        this.availableSeats -= requestedSeats;
    }

    /**
     * Safely releases seats back to the inventory, ensuring available seats do not exceed totalSeats.
     */
    public synchronized void releaseSeats(int count) throws InvalidInputException {
        if (count <= 0) {
            throw new InvalidInputException("Seats to release must be positive.");
        }
        if (this.availableSeats + count > this.totalSeats) {
            throw new InvalidInputException("Cannot release more seats than the train's total capacity.");
        }
        this.availableSeats += count;
    }

    @Override
    public String toString() {
        return String.format("[%s] %-20s | %s -> %s | Dept: %s | Arrv: %s | Seats: %d/%d | Base Fare: ₹%.2f",
                number, name, source, destination, departureTime, arrivalTime, availableSeats, totalSeats, baseFare);
    }
}
