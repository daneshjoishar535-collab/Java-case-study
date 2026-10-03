/** Thrown when a ticket is missing, expired, cancelled or otherwise unusable. */
public class InvalidTicketException extends Exception {
    public InvalidTicketException(String message) {
        super(message);
    }
}
