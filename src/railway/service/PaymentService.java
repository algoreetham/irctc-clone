package railway.service;

/**
 * Interface contract for payment processing.
 * Demonstrates abstraction and loose coupling via interfaces.
 */
public interface PaymentService {
    /**
     * Attempts to process payment for the given amount.
     *
     * @param amount the amount to process
     * @return true if simulated payment succeeded, false otherwise
     */
    boolean pay(double amount);
}
