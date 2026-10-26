package railway.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import railway.ai.AiSwarmManager;
import railway.ai.DishaChatbotService;
import railway.exception.BookingNotFoundException;
import railway.exception.RailwayException;
import railway.model.Booking;
import railway.model.TicketClass;
import railway.model.Train;
import railway.repository.BookingRepository;
import railway.repository.FileBookingRepository;
import railway.repository.JdbcBookingRepository;
import railway.service.BookingService;
import railway.service.DemoPaymentService;
import railway.service.TrainService;
import railway.service.WaitingListWorker;
import railway.web.util.JsonUtil;

/**
 * Built-in HTTP Web Server that powers the web UI and REST API.
 * Uses JDK built-in HttpServer (com.sun.net.httpserver).
 * Integrates:
 * - Local AI Concurrent Passenger Swarm ("Tatkal Rush" Simulation)
 * - Ask DISHA 2.0 Local Conversational Assistant
 * Demonstrates Servlets / HTTP Request-Response handling (CO-5, PrO-14, PrO-15).
 */
public class WebServer {
    private static final int PORT = 8080;
    private static final String STATIC_DIR = "src/railway/web/static";

    private final TrainService trainService;
    private final BookingService bookingService;
    private final DemoPaymentService paymentService;
    private final WaitingListWorker waitingListWorker;
    private final AiSwarmManager swarmManager;
    private final DishaChatbotService dishaService;
    private HttpServer server;

    public WebServer(TrainService trainService, BookingService bookingService, DemoPaymentService paymentService, BookingRepository repo) {
        this.trainService = trainService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        // Multithreaded background worker (CO-3, PrO-8, PrO-9)
        this.waitingListWorker = new WaitingListWorker(bookingService.getAllocationLock(), bookingService, repo);
        // Local AI Swarm & Ask DISHA local chatbot
        this.swarmManager = new AiSwarmManager(trainService, bookingService);
        this.dishaService = new DishaChatbotService(trainService, bookingService, this.swarmManager);
    }

    public static void main(String[] args) throws IOException {
        TrainService trainService = new TrainService();
        DemoPaymentService paymentService = new DemoPaymentService();

        // Seed additional authentic IRCTC trains for the web portal
        trainService.addTrain(new Train("12007", "Shatabdi Express", "Bengaluru", "Chennai",
                "06:00 AM", "11:00 AM", 60, 420.0));
        trainService.addTrain(new Train("20607", "Vande Bharat Express", "Bengaluru", "Mysuru",
                "05:45 AM", "07:35 AM", 40, 360.0));
        trainService.addTrain(new Train("12658", "Chennai Central Mail", "Bengaluru", "Chennai",
                "10:40 PM", "04:30 AM", 55, 290.0));
        trainService.addTrain(new Train("12610", "Mysuru Express", "Mysuru", "Bengaluru",
                "07:00 AM", "09:40 AM", 50, 180.0));
        trainService.addTrain(new Train("12760", "Charminar Express", "Chennai", "Hyderabad",
                "06:10 PM", "08:00 AM", 45, 520.0));

        // Demonstrates JDBC persistence (CO-5, PrO-13)
        BookingRepository repository;
        try {
            Class.forName("org.sqlite.JDBC");
            repository = new JdbcBookingRepository("jdbc:sqlite:data/railway.db", trainService);
        } catch (Throwable t) {
            System.out.println("[Notice] SQLite driver not found in classpath. Falling back to FileBookingRepository.");
            repository = new FileBookingRepository("data/bookings.csv", trainService);
        }

        BookingService bookingService = new BookingService(trainService, paymentService, repository);

        WebServer webServer = new WebServer(trainService, bookingService, paymentService, repository);
        webServer.start(PORT);
    }

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        // Start background multithreaded worker
        waitingListWorker.start();

        // Start AI passenger swarm in Normal mode
        swarmManager.startContinuousSwarm("NORMAL");

        // API Contexts
        server.createContext("/api/trains", new TrainsHandler());
        server.createContext("/api/bookings", new BookingsHandler());
        server.createContext("/api/ai", new AiHandler());

        // Static Web UI Context
        server.createContext("/", new StaticFileHandler());

        server.start();
        System.out.println("================================================================");
        System.out.println("   IRCTC-INSPIRED RAILWAY RESERVATION SYSTEM (WEB PORTAL)       ");
        System.out.println("      Alliance University Course Code: E1CSA25306               ");
        System.out.println("================================================================");
        System.out.printf("   🚀 Web Server running at: http://localhost:%d%n", port);
        System.out.printf("   🚆 API Documentation  at: http://localhost:%d/api/trains%n", port);
        System.out.println("   🤖 Local AI Swarm & Ask DISHA 2.0 active!");
        System.out.println("   Press Ctrl+C to stop the server.");
        System.out.println("================================================================\n");
    }

    public void stop() {
        if (waitingListWorker != null) {
            waitingListWorker.shutdown();
        }
        if (swarmManager != null) {
            swarmManager.shutdown();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    private class TrainsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(method)) {
                sendJsonResponse(exchange, 405, JsonUtil.toApiResponseJson(false, "Method not allowed", null));
                return;
            }

            String path = exchange.getRequestURI().getPath();
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());

            if (path.equals("/api/trains/search")) {
                String source = query.get("source");
                String destination = query.get("destination");
                try {
                    List<Train> results = trainService.searchTrains(source, destination);
                    String dataJson = JsonUtil.trainsToJson(results);
                    sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "Found " + results.size() + " train(s)", dataJson));
                } catch (RailwayException ex) {
                    sendJsonResponse(exchange, 400, JsonUtil.toApiResponseJson(false, ex.getMessage(), null));
                }
            } else if (path.endsWith("/waiting-list")) {
                // /api/trains/{number}/waiting-list
                String[] parts = path.split("/");
                String trainNum = parts[parts.length - 2];
                List<Booking> wl = bookingService.getWaitingList(trainNum);
                sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "Waiting list for " + trainNum, JsonUtil.bookingsToJson(wl)));
            } else if (path.startsWith("/api/trains/")) {
                String trainNum = path.substring("/api/trains/".length()).trim();
                Train t = trainService.getTrainByNumber(trainNum);
                if (t != null) {
                    sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "Train details", JsonUtil.toJson(t)));
                } else {
                    sendJsonResponse(exchange, 404, JsonUtil.toApiResponseJson(false, "Train not found: " + trainNum, null));
                }
            } else {
                List<Train> all = trainService.getAllTrains();
                sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "All demo trains", JsonUtil.trainsToJson(all)));
            }
        }
    }

    private class BookingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();

            if ("POST".equalsIgnoreCase(method)) {
                if (path.endsWith("/cancel")) {
                    String[] parts = path.split("/");
                    if (parts.length >= 4) {
                        String pnr = parts[parts.length - 2].trim();
                        try {
                            Booking cancelled = bookingService.cancelBooking(pnr);
                            sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "Booking cancelled successfully", JsonUtil.toJson(cancelled)));
                        } catch (BookingNotFoundException ex) {
                            sendJsonResponse(exchange, 404, JsonUtil.toApiResponseJson(false, ex.getMessage(), null));
                        } catch (RailwayException ex) {
                            sendJsonResponse(exchange, 400, JsonUtil.toApiResponseJson(false, ex.getMessage(), null));
                        }
                    } else {
                        sendJsonResponse(exchange, 400, JsonUtil.toApiResponseJson(false, "Invalid cancellation URL", null));
                    }
                } else {
                    String body = readRequestBody(exchange);
                    String trainNumber = JsonUtil.extractJsonString(body, "trainNumber");
                    String passengerName = JsonUtil.extractJsonString(body, "passengerName");
                    int passengerAge = JsonUtil.extractJsonInt(body, "passengerAge", 0);
                    int seatCount = JsonUtil.extractJsonInt(body, "seatCount", 0);
                    String ticketClassStr = JsonUtil.extractJsonString(body, "ticketClass");
                    String quota = JsonUtil.extractJsonString(body, "quota");
                    boolean simulateFailure = JsonUtil.extractJsonBool(body, "simulateFailure", false);

                    TicketClass ticketClass = TicketClass.GENERAL;
                    if (ticketClassStr != null) {
                        try {
                            ticketClass = TicketClass.valueOf(ticketClassStr.toUpperCase().trim());
                        } catch (Exception ignored) {}
                    }

                    paymentService.setSimulateFailure(simulateFailure);

                    try {
                        Booking booking = bookingService.bookTicket(trainNumber, passengerName, passengerAge, seatCount, ticketClass, quota);
                        String msg = booking.isWaitingList()
                                ? ("Booking registered on WAITING LIST (Rank: " + booking.getStatusLabel() + ")")
                                : (booking.isTatkal() ? "⚡ Tatkal booking confirmed!" : "Booking confirmed!");
                        sendJsonResponse(exchange, 201, JsonUtil.toApiResponseJson(true, msg, JsonUtil.toJson(booking)));
                    } catch (RailwayException ex) {
                        sendJsonResponse(exchange, 400, JsonUtil.toApiResponseJson(false, ex.getMessage(), null));
                    } finally {
                        paymentService.setSimulateFailure(false);
                    }
                }
            } else if ("GET".equalsIgnoreCase(method)) {
                if (path.equals("/api/bookings") || path.equals("/api/bookings/")) {
                    List<Booking> all = bookingService.getBookingHistory();
                    sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "Booking history", JsonUtil.bookingsToJson(all)));
                } else {
                    String pnr = path.substring("/api/bookings/".length()).trim();
                    try {
                        Booking b = bookingService.getBooking(pnr);
                        sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "Booking details", JsonUtil.toJson(b)));
                    } catch (BookingNotFoundException ex) {
                        sendJsonResponse(exchange, 404, JsonUtil.toApiResponseJson(false, ex.getMessage(), null));
                    } catch (RailwayException ex) {
                        sendJsonResponse(exchange, 400, JsonUtil.toApiResponseJson(false, ex.getMessage(), null));
                    }
                }
            } else {
                sendJsonResponse(exchange, 405, JsonUtil.toApiResponseJson(false, "Method not allowed", null));
            }
        }
    }

    private class AiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            String method = exchange.getRequestMethod();

            if ("OPTIONS".equalsIgnoreCase(method)) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            String path = exchange.getRequestURI().getPath();
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());

            if (path.equals("/api/ai/feed")) {
                if (!"GET".equalsIgnoreCase(method)) {
                    sendJsonResponse(exchange, 405, JsonUtil.toApiResponseJson(false, "Method not allowed", null));
                    return;
                }
                int limit = 30;
                if (query.containsKey("limit")) {
                    try { limit = Integer.parseInt(query.get("limit")); } catch (Exception ignored) {}
                }
                sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "AI Activity Feed", swarmManager.getFeedJson(limit)));

            } else if (path.equals("/api/ai/rush")) {
                if (!"POST".equalsIgnoreCase(method)) {
                    sendJsonResponse(exchange, 405, JsonUtil.toApiResponseJson(false, "Method not allowed", null));
                    return;
                }
                String body = readRequestBody(exchange);
                String trainNumber = query.get("trainNumber");
                if (trainNumber == null) trainNumber = JsonUtil.extractJsonString(body, "trainNumber");
                if (trainNumber == null || trainNumber.trim().isEmpty()) trainNumber = "DEMO101";

                int calculated = swarmManager.calculateTatkalBotCount(trainNumber);
                int count = calculated;
                if (query.containsKey("count")) {
                    try { 
                        int c = Integer.parseInt(query.get("count")); 
                        if (c > 15) count = c;
                    } catch (Exception ignored) {}
                } else {
                    int bodyCount = JsonUtil.extractJsonInt(body, "count", 0);
                    if (bodyCount > 15) count = bodyCount;
                }

                swarmManager.triggerTatkalRush(trainNumber, count);
                String msg = String.format("⚡ 10:00 AM TATKAL RUSH LIVE! Dispatched %d AI passengers exclusively on TATKAL quota for %s (50%% of capacity, min >15 bots)!",
                        count, trainNumber);
                String data = String.format("{\"trainNumber\":\"%s\",\"botCount\":%d,\"quota\":\"TATKAL\"}", trainNumber, count);
                sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, msg, data));

            } else if (path.equals("/api/ai/toggle")) {
                if (!"POST".equalsIgnoreCase(method)) {
                    sendJsonResponse(exchange, 405, JsonUtil.toApiResponseJson(false, "Method not allowed", null));
                    return;
                }
                String body = readRequestBody(exchange);
                String mode = query.get("mode");
                if (mode == null) mode = JsonUtil.extractJsonString(body, "mode");
                if (mode == null) mode = "NORMAL";

                swarmManager.startContinuousSwarm(mode);
                String statusJson = String.format("{\"active\":%b,\"mode\":\"%s\"}", swarmManager.isSwarmActive(), swarmManager.getSwarmSpeedMode());
                sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "Swarm mode updated to " + mode, statusJson));

            } else if (path.equals("/api/ai/status")) {
                String statusJson = String.format("{\"active\":%b,\"mode\":\"%s\"}", swarmManager.isSwarmActive(), swarmManager.getSwarmSpeedMode());
                sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "Swarm status", statusJson));

            } else if (path.equals("/api/ai/disha")) {
                if (!"POST".equalsIgnoreCase(method)) {
                    sendJsonResponse(exchange, 405, JsonUtil.toApiResponseJson(false, "Method not allowed", null));
                    return;
                }
                String body = readRequestBody(exchange);
                String message = JsonUtil.extractJsonString(body, "message");
                if (message == null || message.trim().isEmpty()) {
                    message = query.get("message");
                }

                DishaChatbotService.ChatResponse reply = dishaService.processMessage(message);
                sendJsonResponse(exchange, 200, JsonUtil.toApiResponseJson(true, "Ask DISHA Response", reply.toJson()));

            } else {
                sendJsonResponse(exchange, 404, JsonUtil.toApiResponseJson(false, "Unknown AI endpoint: " + path, null));
            }
        }
    }

    private class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File(STATIC_DIR, path);
            if (!file.exists() || file.isDirectory()) {
                file = new File(STATIC_DIR, "index.html");
            }

            if (!file.exists()) {
                String notFound = "<h1>404 Not Found</h1>";
                exchange.sendResponseHeaders(404, notFound.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            String contentType = "text/html; charset=UTF-8";
            if (path.endsWith(".css")) {
                contentType = "text/css; charset=UTF-8";
            } else if (path.endsWith(".js")) {
                contentType = "application/javascript; charset=UTF-8";
            } else if (path.endsWith(".png")) {
                contentType = "image/png";
            } else if (path.endsWith(".svg")) {
                contentType = "image/svg+xml";
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private void sendJsonResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return map;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2) {
                map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            } else if (pair.length == 1) {
                map.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), "");
            }
        }
        return map;
    }
}
