package railway.web.spring;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import railway.exception.BookingNotFoundException;
import railway.exception.RailwayException;
import railway.model.Booking;
import railway.service.BookingService;
import railway.service.DemoPaymentService;
import railway.web.dto.ApiResponse;
import railway.web.dto.BookingRequest;

/**
 * Spring Boot REST Controller for reservations, PNR inquiries, and cancellations.
 */
@RestController
@RequestMapping("/api/bookings")
@CrossOrigin(origins = "*")
public class BookingApiController {

    private final BookingService bookingService;
    private final DemoPaymentService paymentService;

    public BookingApiController(BookingService bookingService, DemoPaymentService paymentService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Booking>> createBooking(@RequestBody BookingRequest req) {
        try {
            paymentService.setSimulateFailure(req.isSimulateFailure());
            Booking booking = bookingService.bookTicket(
                    req.getTrainNumber(),
                    req.getPassengerName(),
                    req.getPassengerAge(),
                    req.getSeatCount(),
                    req.getTicketClass()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Booking confirmed!", booking));
        } catch (RailwayException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } finally {
            paymentService.setSimulateFailure(false);
        }
    }

    @GetMapping("/{pnr}")
    public ResponseEntity<ApiResponse<Booking>> getBookingByPnr(@PathVariable String pnr) {
        try {
            Booking booking = bookingService.getBooking(pnr);
            return ResponseEntity.ok(ApiResponse.ok("Booking located", booking));
        } catch (BookingNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        } catch (RailwayException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/{pnr}/cancel")
    public ResponseEntity<ApiResponse<Booking>> cancelBooking(@PathVariable String pnr) {
        try {
            Booking cancelled = bookingService.cancelBooking(pnr);
            return ResponseEntity.ok(ApiResponse.ok("Booking cancelled successfully", cancelled));
        } catch (BookingNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
        } catch (RailwayException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Booking>>> getAllBookings() {
        List<Booking> history = bookingService.getBookingHistory();
        return ResponseEntity.ok(ApiResponse.ok("All bookings", history));
    }
}
