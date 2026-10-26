package railway.model;

/**
 * Concrete ticket for General class travel.
 * Demonstrates inheritance and method overriding.
 */
public class GeneralTicket extends Ticket {

    public GeneralTicket(String pnr, Passenger passenger, Train train, int seatCount) {
        super(pnr, passenger, train, seatCount, TicketClass.GENERAL);
    }

    @Override
    public double calculateFare() {
        double base = getTrain().getBaseFare();
        double multiplier = getTicketClass().getFareMultiplier();
        double discount = getPassenger().getFareDiscountFactor();
        return base * multiplier * discount * getSeatCount();
    }
}
