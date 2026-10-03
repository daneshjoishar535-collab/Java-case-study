import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/** FIFO waiting line for a ride, backed by a LinkedList. */
public class RideQueue {
    private final LinkedList<Ticket> line = new LinkedList<>();
    private int maxLength;

    public RideQueue(int maxLength) {
        this.maxLength = maxLength;
    }

    public int size() { return line.size(); }
    public int getMaxLength() { return maxLength; }
    public boolean isEmpty() { return line.isEmpty(); }
    public boolean isFull() { return line.size() >= maxLength; }

    public void setMaxLength(int maxLength) {
        if (maxLength < line.size())
            throw new IllegalArgumentException("Max queue length cannot be below current queue size (" + line.size() + ").");
        this.maxLength = maxLength;
    }

    public void enqueue(Ticket t) { line.addLast(t); }
    public Ticket dequeue() { return line.pollFirst(); }
    public Ticket peek() { return line.peekFirst(); }

    public boolean contains(String visitorId) {
        for (Ticket t : line) if (t.getVisitor().getId().equals(visitorId)) return true;
        return false;
    }

    /** Removes the visitor's entry; returns true if one was removed. */
    public boolean remove(String visitorId) {
        Iterator<Ticket> it = line.iterator();
        while (it.hasNext()) {
            if (it.next().getVisitor().getId().equals(visitorId)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public List<Ticket> toList() {
        return new ArrayList<>(line);
    }
}
