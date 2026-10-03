import java.time.LocalDate;

/** An entry ticket belonging to a visitor, valid on a single date. */
public class Ticket {
    private final String id;
    private final Visitor visitor;
    private final double price;
    private final LocalDate validDate;
    private boolean cancelled;
    private int ridesTaken;

    public Ticket(String id, Visitor visitor, double price, LocalDate validDate) {
        if (visitor == null) throw new IllegalArgumentException("Ticket needs a visitor.");
        if (price <= 0) throw new IllegalArgumentException("Ticket price must be positive.");
        if (validDate == null) throw new IllegalArgumentException("Ticket needs a valid date.");
        this.id = id;
        this.visitor = visitor;
        this.price = price;
        this.validDate = validDate;
    }

    public String getId() { return id; }
    public Visitor getVisitor() { return visitor; }
    public double getPrice() { return price; }
    public LocalDate getValidDate() { return validDate; }
    public boolean isCancelled() { return cancelled; }
    public int getRidesTaken() { return ridesTaken; }

    public void cancel() { cancelled = true; }
    public void recordRide() { ridesTaken++; }

    /** ACTIVE, CANCELLED, EXPIRED or NOT YET VALID. */
    public String status() {
        if (cancelled) return "CANCELLED";
        LocalDate today = LocalDate.now();
        if (validDate.isBefore(today)) return "EXPIRED";
        if (validDate.isAfter(today)) return "NOT YET VALID";
        return "ACTIVE";
    }

    /** A ticket is usable only if it is not cancelled and valid today. */
    public boolean isValid() {
        return status().equals("ACTIVE");
    }

    @Override
    public String toString() {
        return id + " [" + visitor.getName() + ", " + status() + "]";
    }
}
