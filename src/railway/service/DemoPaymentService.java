package railway.service;

/**
 * Simulated payment processor implementation.
 * Never requests or stores real banking credentials or card details.
 * Demonstrates interface implementation and polymorphism.
 */
public class DemoPaymentService implements PaymentService {
    private boolean simulateFailure = false;

    public DemoPaymentService() {
    }

    public DemoPaymentService(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @Override
    public boolean pay(double amount) {
        if (amount <= 0) {
            System.out.println("[Payment Gate] Rejected: Amount must be greater than zero.");
            return false;
        }

        if (simulateFailure) {
            System.out.println("[Payment Gate] Simulated failure: Transaction declined by bank.");
            return false;
        }

        System.out.printf("[Payment Gate] SUCCESS: Demo payment of ₹%.2f processed successfully.%n", amount);
        return true;
    }
}
