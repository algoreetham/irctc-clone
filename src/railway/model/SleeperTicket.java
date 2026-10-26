package railway.model;

/**
 * Concrete ticket for Sleeper class travel.
 * Demonstrates inheritance and method overriding.
 */
public class SleeperTicket extends Ticket {

    public SleeperTicket(String pnr, Passenger passenger, Train train, int seatCount) {
        super(pnr, passenger, train, seatCount, TicketClass.SLEEPER);
    }

    @Override
    public double calculateFare() {
        double base = getTrain().getBaseFare();
        double multiplier = getTicketClass().getFareMultiplier();
        double discount = getPassenger().getFareDiscountFactor();
        return base * multiplier * discount * getSeatCount();
    }
}
