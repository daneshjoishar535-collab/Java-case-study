/** Thrown when a visitor fails a ride's height/age safety restriction. */
public class RestrictionViolationException extends Exception {
    public RestrictionViolationException(String message) {
        super(message);
    }
}
