package railway.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import railway.repository.BookingRepository;
import railway.repository.FileBookingRepository;
import railway.service.BookingService;
import railway.service.DemoPaymentService;
import railway.service.TrainService;

/**
 * Spring Boot Application entrypoint for the IRCTC-Inspired Railway Reservation System.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"railway"})
public class RailwayWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(RailwayWebApplication.class, args);
    }

    @Bean
    public TrainService trainService() {
        return new TrainService();
    }

    @Bean
    public DemoPaymentService paymentService() {
        return new DemoPaymentService();
    }

    @Bean
    public BookingRepository bookingRepository(TrainService trainService) {
        return new FileBookingRepository("data/bookings.csv", trainService);
    }

    @Bean
    public BookingService bookingService(TrainService trainService,
                                         DemoPaymentService paymentService,
                                         BookingRepository bookingRepository) {
        return new BookingService(trainService, paymentService, bookingRepository);
    }
}
