package railway.exception;

/**
 * Thrown when user input violates validation criteria (e.g. blank station, invalid seat count, negative fare).
 */
public class InvalidInputException extends RailwayException {
    public InvalidInputException(String message) {
        super(message);
    }
}
