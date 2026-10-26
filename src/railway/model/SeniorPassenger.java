package railway.model;

/**
 * Represents a senior citizen passenger eligible for demo concessions.
 * Demonstrates inheritance, constructor chaining with super, and method overriding.
 */
public class SeniorPassenger extends Passenger {
    public static final int MIN_SENIOR_AGE = 60;
    public static final double SENIOR_DISCOUNT_PERCENT = 40.0; // 40% demo concession

    public SeniorPassenger(String name, int age) {
        super(name, age);
        if (age < MIN_SENIOR_AGE) {
            throw new IllegalArgumentException("Senior passenger age must be at least " + MIN_SENIOR_AGE + ".");
        }
    }

    @Override
    public double getFareDiscountFactor() {
        return (100.0 - SENIOR_DISCOUNT_PERCENT) / 100.0; // 0.60
    }

    @Override
    public String getCategory() {
        return "Senior Citizen (40% concession)";
    }
}
