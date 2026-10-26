package railway.web.spring;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import railway.exception.RailwayException;
import railway.model.Train;
import railway.service.TrainService;
import railway.web.dto.ApiResponse;

/**
 * Spring Boot REST Controller for Train searches and seat availability.
 */
@RestController
@RequestMapping("/api/trains")
@CrossOrigin(origins = "*")
public class TrainApiController {

    private final TrainService trainService;

    public TrainApiController(TrainService trainService) {
        this.trainService = trainService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Train>>> getAllTrains() {
        List<Train> trains = trainService.getAllTrains();
        return ResponseEntity.ok(ApiResponse.ok("All demo trains", trains));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Train>>> searchTrains(
            @RequestParam String source,
            @RequestParam String destination) {
        try {
            List<Train> results = trainService.searchTrains(source, destination);
            return ResponseEntity.ok(ApiResponse.ok("Found " + results.size() + " train(s)", results));
        } catch (RailwayException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/{number}")
    public ResponseEntity<ApiResponse<Train>> getTrainByNumber(@PathVariable String number) {
        Train train = trainService.getTrainByNumber(number);
        if (train != null) {
            return ResponseEntity.ok(ApiResponse.ok("Train details", train));
        }
        return ResponseEntity.status(404).body(ApiResponse.error("Train not found: " + number));
    }
}
