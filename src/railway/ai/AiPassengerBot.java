package railway.ai;

import java.util.Random;
import railway.model.TicketClass;

/**
 * Represents an autonomous AI passenger agent that browses and books train tickets.
 * Generates realistic human personas with authentic Indian names, age distributions,
 * ticket class preferences, booking sizes, and quota behaviors (General vs Tatkal).
 */
public class AiPassengerBot {
    private static final Random RNG = new Random();

    private static final String[] FIRST_NAMES = {
        "Aarav", "Priya", "Rohan", "Sneha", "Vikram", "Ananya", "Rajesh", "Sunita",
        "Arjun", "Meera", "Deepa", "Aditya", "Kavita", "Suresh", "Pooja", "Sanjay",
        "Neha", "Karthik", "Divya", "Manoj", "Aishwarya", "Rahul", "Swati", "Vivek",
        "Tanvi", "Gaurav", "Nandini", "Harish", "Preeti", "Alok"
    };

    private static final String[] LAST_NAMES = {
        "Sharma", "Patel", "Iyer", "Verma", "Gupta", "Reddy", "Nair", "Kumar",
        "Rao", "Singh", "Joshi", "Deshmukh", "Mukherjee", "Das", "Pillai", "Choudhury",
        "Bhat", "Mehta", "Kulkarni", "Banerjee"
    };

    private final String id;
    private final String name;
    private final int age;
    private final TicketClass preferredClass;
    private final int seatsToBook;
    private final String quota; // "GENERAL" or "TATKAL"

    public AiPassengerBot() {
        this(false);
    }

    public AiPassengerBot(boolean tatkalMode) {
        this.id = "BOT-" + (1000 + RNG.nextInt(9000));
        this.name = FIRST_NAMES[RNG.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[RNG.nextInt(LAST_NAMES.length)];
        this.quota = tatkalMode ? "TATKAL" : "GENERAL";

        // Age distribution
        if (tatkalMode) {
            // Tatkal rush is predominantly tech-savvy adults / commuters
            this.age = 21 + RNG.nextInt(40);
        } else {
            if (RNG.nextDouble() < 0.25) {
                this.age = 60 + RNG.nextInt(19);
            } else {
                this.age = 19 + RNG.nextInt(41);
            }
        }

        // Ticket class distribution
        if (tatkalMode) {
            // 55% AC, 40% Sleeper, 5% General in Tatkal
            double roll = RNG.nextDouble();
            if (roll < 0.55) {
                this.preferredClass = TicketClass.AC;
            } else if (roll < 0.95) {
                this.preferredClass = TicketClass.SLEEPER;
            } else {
                this.preferredClass = TicketClass.GENERAL;
            }

            // In Tatkal rush, passengers rush 1 or 2 seats to maximize confirmation
            this.seatsToBook = RNG.nextDouble() < 0.85 ? 1 : 2;
        } else {
            double classRoll = RNG.nextDouble();
            if (classRoll < 0.45) {
                this.preferredClass = TicketClass.SLEEPER;
            } else if (classRoll < 0.80) {
                this.preferredClass = TicketClass.AC;
            } else {
                this.preferredClass = TicketClass.GENERAL;
            }

            double seatRoll = RNG.nextDouble();
            if (seatRoll < 0.70) {
                this.seatsToBook = 1;
            } else if (seatRoll < 0.90) {
                this.seatsToBook = 2;
            } else {
                this.seatsToBook = 3;
            }
        }
    }

    public AiPassengerBot(String name, int age, TicketClass preferredClass, int seatsToBook, String quota) {
        this.id = "BOT-" + (1000 + RNG.nextInt(9000));
        this.name = name;
        this.age = age;
        this.preferredClass = preferredClass;
        this.seatsToBook = seatsToBook;
        this.quota = quota != null ? quota.toUpperCase() : "GENERAL";
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public TicketClass getPreferredClass() { return preferredClass; }
    public int getSeatsToBook() { return seatsToBook; }
    public String getQuota() { return quota; }
    public boolean isTatkal() { return "TATKAL".equalsIgnoreCase(quota); }
    public boolean isSenior() { return age >= 60 && !isTatkal(); } // No senior discount in Tatkal

    @Override
    public String toString() {
        return String.format("[%s] %s (Age: %d%s, Wants: %d x %s)",
                quota, name, age, (isSenior() ? ", Senior Citizen" : ""), seatsToBook, preferredClass);
    }
}
