package railway.model;

/**
 * Encapsulates passenger details with strict state validation.
 * Demonstrates encapsulation in OOP.
 */
public class Passenger {
    private String name;
    private int age;

    public Passenger(String name, int age) {
        setName(name);
        setAge(age);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Passenger name cannot be blank.");
        }
        this.name = name.trim();
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        if (age <= 0 || age > 120) {
            throw new IllegalArgumentException("Age must be between 1 and 120.");
        }
        this.age = age;
    }

    /**
     * Discount multiplier applied to base fare. Default is 1.0 (no concession).
     */
    public double getFareDiscountFactor() {
        return 1.0;
    }

    public String getCategory() {
        return "Adult";
    }

    @Override
    public String toString() {
        return name + " (" + age + " yrs, " + getCategory() + ")";
    }
}
