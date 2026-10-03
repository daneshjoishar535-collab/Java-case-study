/** Thrown when a ride or its queue would exceed its safe capacity. */
public class OverCapacityException extends Exception {
    public OverCapacityException(String message) {
        super(message);
    }
}
