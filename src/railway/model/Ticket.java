package railway.model;

/**
 * Abstract Ticket class defining common ticket attributes and the calculateFare contract.
 * Demonstrates Abstraction in OOP.
 */
public abstract class Ticket {
    private final String pnr;
    private final Passenger passenger;
    private final Train train;
    private final int seatCount;
    private final TicketClass ticketClass;

    protected Ticket(String pnr, Passenger passenger, Train train, int seatCount, TicketClass ticketClass) {
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

        this.pnr = pnr.trim();
        this.passenger = passenger;
        this.train = train;
        this.seatCount = seatCount;
        this.ticketClass = ticketClass;
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

    /**
     * Abstract method implemented by concrete ticket subclasses to calculate total fare.
     * Demonstrates Polymorphism and Abstraction.
     */
    public abstract double calculateFare();

    public void printSummary() {
        System.out.println("PNR: " + pnr);
        System.out.println("Passenger: " + passenger.getName() + " (" + passenger.getCategory() + ")");
        System.out.println("Train: " + train.getName() + " (" + train.getNumber() + ")");
        System.out.println("Route: " + train.getSource() + " -> " + train.getDestination());
        System.out.println("Class: " + ticketClass.getDisplayName());
        System.out.println("Seats: " + seatCount);
        System.out.printf("Total Demo Fare: ₹%.2f%n", calculateFare());
    }
}
