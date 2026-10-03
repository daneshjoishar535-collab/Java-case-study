import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Business logic for the park: CRUD, queues, searching, sorting and reports. */
public class ParkService {
    private final Map<String, Ride> rides = new HashMap<>();            // Ride ID -> Ride
    private final List<Visitor> visitors = new ArrayList<>();
    private final List<Ticket> tickets = new ArrayList<>();
    private int visitorCounter = 1000;
    private int ticketCounter = 5000;

    // ================= Ride Setup (CRUD) =================
    public void addRide(Ride ride) {
        if (rides.containsKey(ride.getId()))
            throw new IllegalArgumentException("A ride with ID " + ride.getId() + " already exists.");
        rides.put(ride.getId(), ride);
    }

    public Ride getRide(String id) {
        Ride r = id == null ? null : rides.get(id.trim().toUpperCase());
        if (r == null) throw new IllegalArgumentException("Ride not found: " + id);
        return r;
    }

    public void updateRide(String id, String name, RideCategory cat, int minH, int maxH,
                           int minAge, int capacity, int maxQ) {
        getRide(id).update(name, cat, minH, maxH, minAge, capacity, maxQ);
    }

    public void deleteRide(String id) {
        Ride r = getRide(id);
        if (r.getOnboardCount() > 0)
            throw new IllegalStateException("Cannot delete " + r.getName() + " while riders are on board.");
        rides.remove(r.getId());
    }

    public List<Ride> getAllRides() {
        List<Ride> list = new ArrayList<>(rides.values());
        list.sort(Comparator.comparing(Ride::getId));
        return list;
    }

    // ================= Visitors (CRUD) =================
    public Visitor registerVisitor(String name, int age, int heightCm) {
        Visitor v = new Visitor("V" + (++visitorCounter), name, age, heightCm);
        visitors.add(v);
        return v;
    }

    public Visitor getVisitor(String id) {
        for (Visitor v : visitors) if (v.getId().equalsIgnoreCase(id == null ? "" : id.trim())) return v;
        throw new IllegalArgumentException("Visitor not found: " + id);
    }

    public void updateVisitor(String id, String name, int age, int heightCm) {
        getVisitor(id).setDetails(name, age, heightCm);
    }

    public void deleteVisitor(String id) {
        Visitor v = getVisitor(id);
        for (Ride r : rides.values()) {
            if (r.isOnboard(v.getId()))
                throw new IllegalStateException(v.getName() + " is currently on " + r.getName() + ".");
        }
        for (Ride r : rides.values()) r.leaveQueue(v.getId());
        for (Ticket t : tickets) if (t.getVisitor() == v) t.cancel();
        tickets.removeIf(t -> t.getVisitor() == v);
        visitors.remove(v);
    }

    public List<Visitor> getAllVisitors() { return new ArrayList<>(visitors); }

    // ================= Ticket Sales (CRUD) =================
    public Ticket sellTicket(String visitorId, double price, LocalDate validDate) {
        Visitor v = getVisitor(visitorId);
        if (validDate == null || validDate.isBefore(LocalDate.now()))
            throw new IllegalArgumentException("Ticket date cannot be in the past.");
        Ticket t = new Ticket("T" + (++ticketCounter), v, price, validDate);
        tickets.add(t);
        return t;
    }

    public Ticket getTicket(String id) throws InvalidTicketException {
        for (Ticket t : tickets) if (t.getId().equalsIgnoreCase(id == null ? "" : id.trim())) return t;
        throw new InvalidTicketException("Ticket not found: " + id);
    }

    public void cancelTicket(String id) throws InvalidTicketException {
        Ticket t = getTicket(id);
        for (Ride r : rides.values()) {
            if (r.isOnboard(t.getVisitor().getId()))
                throw new InvalidTicketException("Cannot cancel: visitor is currently on " + r.getName() + ".");
        }
        for (Ride r : rides.values()) r.leaveQueue(t.getVisitor().getId());
        t.cancel();
    }

    public List<Ticket> getAllTickets() { return new ArrayList<>(tickets); }

    // ================= Queue Management =================
    public void joinQueue(String rideId, String ticketId)
            throws InvalidTicketException, RestrictionViolationException, OverCapacityException {
        getRide(rideId).joinQueue(getTicket(ticketId));
    }

    public boolean leaveQueue(String rideId, String ticketId) throws InvalidTicketException {
        return getRide(rideId).leaveQueue(getTicket(ticketId).getVisitor().getId());
    }

    public Ticket boardNext(String rideId) throws OverCapacityException {
        return getRide(rideId).boardNext();
    }

    public int boardAll(String rideId) { return getRide(rideId).boardAll(); }
    public int completeCycle(String rideId) { return getRide(rideId).completeCycle(); }

    // ================= Searching =================
    public List<Ride> searchRides(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Ride> result = new ArrayList<>();
        for (Ride r : getAllRides()) {
            if (r.getId().toLowerCase().contains(k) || r.getName().toLowerCase().contains(k)
                    || r.getCategory().getLabel().toLowerCase().contains(k)) result.add(r);
        }
        return result;
    }

    public List<Visitor> searchVisitors(String keyword) {
        String k = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Visitor> result = new ArrayList<>();
        for (Visitor v : visitors) {
            if (v.getId().toLowerCase().contains(k) || v.getName().toLowerCase().contains(k)) result.add(v);
        }
        return result;
    }

    // ================= Sorting =================
    public List<Ride> sortedByQueueLength(boolean descending) {
        List<Ride> list = getAllRides();
        Comparator<Ride> c = Comparator.comparingInt(Ride::getQueueLength);
        list.sort(descending ? c.reversed() : c);
        return list;
    }

    public List<Ride> sortedByCapacity(boolean descending) {
        List<Ride> list = getAllRides();
        Comparator<Ride> c = Comparator.comparingInt(Ride::getCapacity);
        list.sort(descending ? c.reversed() : c);
        return list;
    }

    public List<Ride> sortedByName() {
        List<Ride> list = getAllRides();
        list.sort(Comparator.comparing(r -> r.getName().toLowerCase()));
        return list;
    }

    /** TreeMap: queue length (longest first) -> rides with that queue length. */
    public TreeMap<Integer, List<Ride>> ridesByQueueLength() {
        TreeMap<Integer, List<Ride>> map = new TreeMap<>(Collections.reverseOrder());
        for (Ride r : getAllRides()) map.computeIfAbsent(r.getQueueLength(), k -> new ArrayList<>()).add(r);
        return map;
    }

    // ================= Park Reports =================
    public double totalRevenue() {
        double sum = 0;
        for (Ticket t : tickets) if (!t.isCancelled()) sum += t.getPrice();
        return sum;
    }

    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        int active = 0, cancelled = 0;
        for (Ticket t : tickets) { if (t.isCancelled()) cancelled++; else active++; }
        int waiting = 0, riding = 0, served = 0;
        for (Ride r : rides.values()) { waiting += r.getQueueLength(); riding += r.getOnboardCount(); served += r.getTotalRiders(); }

        sb.append("=========== PARK SUMMARY ===========\n");
        sb.append(String.format("Date              : %s%n", LocalDate.now()));
        sb.append(String.format("Rides             : %d%n", rides.size()));
        sb.append(String.format("Visitors          : %d%n", visitors.size()));
        sb.append(String.format("Tickets (valid)   : %d   Cancelled: %d%n", active, cancelled));
        sb.append(String.format("Revenue           : %.2f%n", totalRevenue()));
        sb.append(String.format("Waiting in queues : %d%n", waiting));
        sb.append(String.format("Currently riding  : %d%n", riding));
        sb.append(String.format("Total rides given : %d%n%n", served));

        sb.append("=========== RIDE STATUS ===========\n");
        sb.append(String.format("%-6s %-20s %-7s %-10s %-10s %-7s%n", "ID", "Name", "Type", "Queue", "On Board", "Served"));
        for (Ride r : getAllRides()) {
            sb.append(String.format("%-6s %-20s %-7s %-10s %-10s %-7d%n", r.getId(), r.getName(),
                    r.getCategory().getLabel(), r.getQueueLength() + "/" + r.getMaxQueueLength(),
                    r.getOnboardCount() + "/" + r.getCapacity(), r.getTotalRiders()));
        }

        sb.append("\n=========== QUEUE RANKING (longest first) ===========\n");
        for (Map.Entry<Integer, List<Ride>> e : ridesByQueueLength().entrySet()) {
            StringBuilder names = new StringBuilder();
            for (Ride r : e.getValue()) { if (names.length() > 0) names.append(", "); names.append(r.getName()); }
            sb.append(String.format("%3d waiting : %s%n", e.getKey(), names));
        }

        sb.append("\n=========== CAPACITY ALERTS ===========\n");
        boolean any = false;
        for (Ride r : getAllRides()) {
            if (r.getOnboardCount() >= r.getCapacity()) { sb.append(r.getName()).append(" is FULL on board.\n"); any = true; }
            if (r.getQueue().isFull()) { sb.append(r.getName()).append(" queue is FULL.\n"); any = true; }
        }
        if (!any) sb.append("No capacity alerts.\n");
        return sb.toString();
    }
}
