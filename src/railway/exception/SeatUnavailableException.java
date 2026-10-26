package railway.exception;

/**
 * Thrown when requested seat count exceeds available seats on a train.
 */
public class SeatUnavailableException extends RailwayException {
    public SeatUnavailableException(String message) {
        super(message);
    }
}
