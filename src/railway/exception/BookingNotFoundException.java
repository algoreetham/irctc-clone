package railway.exception;

/**
 * Thrown when a booking cannot be located by its PNR.
 */
public class BookingNotFoundException extends RailwayException {
    public BookingNotFoundException(String message) {
        super(message);
    }
}
