package railway.model;

/**
 * Concrete ticket for AC class travel.
 * Demonstrates inheritance and method overriding.
 */
public class ACTicket extends Ticket {

    public ACTicket(String pnr, Passenger passenger, Train train, int seatCount) {
        super(pnr, passenger, train, seatCount, TicketClass.AC);
    }

    @Override
    public double calculateFare() {
        double base = getTrain().getBaseFare();
        double multiplier = getTicketClass().getFareMultiplier();
        double discount = getPassenger().getFareDiscountFactor();
        return base * multiplier * discount * getSeatCount();
    }
}
