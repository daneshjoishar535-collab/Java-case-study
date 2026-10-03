import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.time.LocalDate;

/** Entry point: seeds some sample data and launches the GUI. */
public class Main {
    public static void main(String[] args) {
        ParkService service = new ParkService();
        seed(service);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) { }
            new ParkGUI(service).setVisible(true);
        });
    }

    private static void seed(ParkService s) {
        s.addRide(new Ride("R101", "Thunder Coaster", RideCategory.THRILL, 140, 200, 10, 24, 60));
        s.addRide(new Ride("R102", "Giant Ferris Wheel", RideCategory.FAMILY, 100, 0, 0, 30, 80));
        s.addRide(new Ride("R103", "Mini Carousel", RideCategory.KIDS, 80, 130, 0, 16, 30));
        s.addRide(new Ride("R104", "Drop Tower", RideCategory.THRILL, 130, 200, 12, 12, 40));

        Visitor a = s.registerVisitor("Aarav Sharma", 25, 175);
        Visitor b = s.registerVisitor("Meera Patel", 9, 125);
        Visitor c = s.registerVisitor("Rohan Verma", 14, 150);
        s.registerVisitor("Ananya Iyer", 5, 105);

        LocalDate today = LocalDate.now();
        Ticket t1 = s.sellTicket(a.getId(), 800, today);
        Ticket t2 = s.sellTicket(b.getId(), 500, today);
        s.sellTicket(c.getId(), 800, today);
        try {
            s.joinQueue("R101", t1.getId());
            s.joinQueue("R102", t1.getId());
            s.joinQueue("R102", t2.getId());
        } catch (Exception e) {
            System.out.println("Seed note: " + e.getMessage());
        }
    }
}
