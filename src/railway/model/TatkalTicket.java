package railway.model;

/**
 * Concrete Ticket implementation representing an IRCTC Tatkal Quota reservation.
 * Demonstrates Abstraction and Dynamic Polymorphism (CO-1, CO-2).
 *
 * Official Indian Railways / IRCTC Tatkal Rules:
 * 1. Fixed Tatkal surcharge applied per berth:
 *    - AC Class (3A): +₹300 per passenger
 *    - Sleeper Class (SL): +₹150 per passenger
 *    - General Class (2S): +₹50 per passenger
 * 2. No concessions (including Senior Citizen) are permitted under Tatkal quota.
 */
public class TatkalTicket extends Ticket {
    public static final double TATKAL_AC_SURCHARGE = 300.0;
    public static final double TATKAL_SL_SURCHARGE = 150.0;
    public static final double TATKAL_GEN_SURCHARGE = 50.0;

    private final double tatkalSurcharge;

    public TatkalTicket(String pnr, Passenger passenger, Train train, int seatCount, TicketClass ticketClass) {
        super(pnr, passenger, train, seatCount, ticketClass);

        switch (ticketClass) {
            case AC:
                this.tatkalSurcharge = TATKAL_AC_SURCHARGE * seatCount;
                break;
            case SLEEPER:
                this.tatkalSurcharge = TATKAL_SL_SURCHARGE * seatCount;
                break;
            case GENERAL:
            default:
                this.tatkalSurcharge = TATKAL_GEN_SURCHARGE * seatCount;
                break;
        }
    }

    @Override
    public double calculateFare() {
        // IRCTC Tatkal Rule: Standard base fare without concessions + mandatory Tatkal surcharge
        double baseRate = getTrain().getBaseFare();
        double classMultiplier = getTicketClass().getFareMultiplier();
        double baseTotal = baseRate * classMultiplier * getSeatCount();
        return baseTotal + tatkalSurcharge;
    }

    public double getTatkalSurcharge() {
        return tatkalSurcharge;
    }

    @Override
    public void printSummary() {
        super.printSummary();
        System.out.printf("Quota: TATKAL | Tatkal Surcharge: ₹%.2f (No concessions applicable)%n", tatkalSurcharge);
    }
}
