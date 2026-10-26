package railway.repository;

import java.util.List;
import railway.model.Booking;

/**
 * Interface contract for persisting and retrieving bookings.
 * Demonstrates abstraction in data access layer.
 */
public interface BookingRepository {
    /**
     * Persists a new booking record.
     */
    void save(Booking booking);

    /**
     * Finds a booking by PNR.
     *
     * @param pnr the PNR string
     * @return Booking if found, null otherwise
     */
    Booking findByPnr(String pnr);

    /**
     * Returns an unmodifiable list of all bookings in the repository.
     */
    List<Booking> findAll();

    /**
     * Updates an existing booking record (e.g. on cancellation).
     */
    void update(Booking booking);
}
