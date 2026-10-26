package railway.exception;

/**
 * Base checked exception for domain-level railway reservation application errors.
 */
public class RailwayException extends Exception {
    public RailwayException(String message) {
        super(message);
    }

    public RailwayException(String message, Throwable cause) {
        super(message, cause);
    }
}
