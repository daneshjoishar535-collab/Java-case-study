import java.util.ArrayList;
import java.util.List;

/** A ride with safety restrictions, a capacity, a waiting queue and current riders. */
public class Ride implements Restrictable {
    private final String id;
    private String name;
    private RideCategory category;
    private int minHeightCm;
    private int maxHeightCm;   // 0 = no upper limit
    private int minAge;
    private int capacity;      // safe number of riders per cycle
    private final RideQueue queue;
    private final List<Ticket> onboard = new ArrayList<>();
    private int totalRiders;

    public Ride(String id, String name, RideCategory category, int minHeightCm, int maxHeightCm,
                int minAge, int capacity, int maxQueueLength) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Ride ID is required.");
        this.id = id.trim().toUpperCase();
        validate(name, category, minHeightCm, maxHeightCm, minAge, capacity, maxQueueLength);
        this.name = name.trim();
        this.category = category;
        this.minHeightCm = minHeightCm;
        this.maxHeightCm = maxHeightCm;
        this.minAge = minAge;
        this.capacity = capacity;
        this.queue = new RideQueue(maxQueueLength);
    }

    private static void validate(String name, RideCategory category, int minH, int maxH,
                                 int minAge, int capacity, int maxQ) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Ride name is required.");
        if (category == null) throw new IllegalArgumentException("Ride category is required.");
        if (minH < 0) throw new IllegalArgumentException("Minimum height cannot be negative.");
        if (maxH != 0 && maxH < minH) throw new IllegalArgumentException("Maximum height must be 0 (no limit) or >= minimum height.");
        if (minAge < 0) throw new IllegalArgumentException("Minimum age cannot be negative.");
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be at least 1.");
        if (maxQ <= 0) throw new IllegalArgumentException("Max queue length must be at least 1.");
    }

    /** Updates ride settings (the ID never changes). */
    public void update(String name, RideCategory category, int minH, int maxH, int minAge,
                       int capacity, int maxQueueLength) {
        validate(name, category, minH, maxH, minAge, capacity, maxQueueLength);
        if (capacity < onboard.size())
            throw new IllegalArgumentException("Capacity cannot be below riders currently on the ride (" + onboard.size() + ").");
        queue.setMaxLength(maxQueueLength);
        this.name = name.trim();
        this.category = category;
        this.minHeightCm = minH;
        this.maxHeightCm = maxH;
        this.minAge = minAge;
        this.capacity = capacity;
    }

    // ---- Restrictable ----
    @Override
    public boolean isEligible(Visitor v) {
        try {
            checkEligibility(v);
            return true;
        } catch (RestrictionViolationException e) {
            return false;
        }
    }

    @Override
    public void checkEligibility(Visitor v) throws RestrictionViolationException {
        if (v.getHeightCm() < minHeightCm)
            throw new RestrictionViolationException(v.getName() + " is too short for " + name
                    + " (" + v.getHeightCm() + " cm, minimum " + minHeightCm + " cm).");
        if (maxHeightCm != 0 && v.getHeightCm() > maxHeightCm)
            throw new RestrictionViolationException(v.getName() + " is too tall for " + name
                    + " (" + v.getHeightCm() + " cm, maximum " + maxHeightCm + " cm).");
        if (v.getAge() < minAge)
            throw new RestrictionViolationException(v.getName() + " is too young for " + name
                    + " (age " + v.getAge() + ", minimum " + minAge + ").");
    }

    // ---- Queue & capacity operations ----
    public void joinQueue(Ticket t) throws InvalidTicketException, RestrictionViolationException, OverCapacityException {
        if (t == null) throw new InvalidTicketException("Ticket not found.");
        if (!t.isValid())
            throw new InvalidTicketException("Ticket " + t.getId() + " is not valid (" + t.status() + ").");
        checkEligibility(t.getVisitor());
        String vid = t.getVisitor().getId();
        if (queue.contains(vid) || isOnboard(vid))
            throw new InvalidTicketException(t.getVisitor().getName() + " is already queued or riding " + name + ".");
        if (queue.isFull())
            throw new OverCapacityException("Queue for " + name + " is full (" + queue.size() + "/" + queue.getMaxLength() + ").");
        queue.enqueue(t);
    }

    /** Moves the next waiting visitor onto the ride. Returns null if nobody is waiting. */
    public Ticket boardNext() throws OverCapacityException {
        if (queue.isEmpty()) return null;
        if (onboard.size() >= capacity)
            throw new OverCapacityException(name + " is at full capacity (" + onboard.size() + "/" + capacity
                    + "). Complete the current cycle first.");
        Ticket t = queue.dequeue();
        onboard.add(t);
        return t;
    }

    /** Boards visitors until the ride is full or the queue is empty. */
    public int boardAll() {
        int count = 0;
        while (!queue.isEmpty() && onboard.size() < capacity) {
            onboard.add(queue.dequeue());
            count++;
        }
        return count;
    }

    /** Finishes a ride cycle; everybody gets off. Returns how many riders were on board. */
    public int completeCycle() {
        int n = onboard.size();
        for (Ticket t : onboard) t.recordRide();
        totalRiders += n;
        onboard.clear();
        return n;
    }

    public boolean leaveQueue(String visitorId) { return queue.remove(visitorId); }

    public boolean isOnboard(String visitorId) {
        for (Ticket t : onboard) if (t.getVisitor().getId().equals(visitorId)) return true;
        return false;
    }

    // ---- Getters ----
    public String getId() { return id; }
    public String getName() { return name; }
    public RideCategory getCategory() { return category; }
    public int getMinHeightCm() { return minHeightCm; }
    public int getMaxHeightCm() { return maxHeightCm; }
    public int getMinAge() { return minAge; }
    public int getCapacity() { return capacity; }
    public RideQueue getQueue() { return queue; }
    public int getQueueLength() { return queue.size(); }
    public int getMaxQueueLength() { return queue.getMaxLength(); }
    public List<Ticket> getOnboard() { return new ArrayList<>(onboard); }
    public int getOnboardCount() { return onboard.size(); }
    public int getTotalRiders() { return totalRiders; }

    @Override
    public String toString() {
        return id + " - " + name;
    }
}
