package railway.web.dto;

import railway.model.TicketClass;

/**
 * Data Transfer Object for incoming ticket booking requests.
 */
public class BookingRequest {
    private String trainNumber;
    private String passengerName;
    private int passengerAge;
    private int seatCount;
    private TicketClass ticketClass;
    private boolean simulateFailure;

    public BookingRequest() {
    }

    public BookingRequest(String trainNumber, String passengerName, int passengerAge, int seatCount, TicketClass ticketClass) {
        this.trainNumber = trainNumber;
        this.passengerName = passengerName;
        this.passengerAge = passengerAge;
        this.seatCount = seatCount;
        this.ticketClass = ticketClass;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public int getPassengerAge() {
        return passengerAge;
    }

    public void setPassengerAge(int passengerAge) {
        this.passengerAge = passengerAge;
    }

    public int getSeatCount() {
        return seatCount;
    }

    public void setSeatCount(int seatCount) {
        this.seatCount = seatCount;
    }

    public TicketClass getTicketClass() {
        return ticketClass;
    }

    public void setTicketClass(TicketClass ticketClass) {
        this.ticketClass = ticketClass;
    }

    public boolean isSimulateFailure() {
        return simulateFailure;
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }
}
