package railway.model;

/**
 * Represents demo ticket travel classes with distinct fare multipliers.
 */
public enum TicketClass {
    GENERAL("General (2S)", 1.0),
    SLEEPER("Sleeper (SL)", 1.5),
    AC("AC Tier (3A)", 2.5);

    private final String displayName;
    private final double fareMultiplier;

    TicketClass(String displayName, double fareMultiplier) {
        this.displayName = displayName;
        this.fareMultiplier = fareMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getFareMultiplier() {
        return fareMultiplier;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
