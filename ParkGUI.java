import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Swing GUI for the Amusement Park Ride & Ticket Management System. */
public class ParkGUI extends JFrame {

    private interface Action { void run() throws Exception; }

    private final ParkService service;

    // Rides tab
    private final JTextField rId = new JTextField(), rName = new JTextField(), rMinH = new JTextField("0"),
            rMaxH = new JTextField("0"), rMinAge = new JTextField("0"), rCap = new JTextField(), rMaxQ = new JTextField();
    private final JComboBox<RideCategory> rCat = new JComboBox<>(RideCategory.values());
    private final JTextField rSearch = new JTextField(14);
    private final JComboBox<String> rSort = new JComboBox<>(new String[]{
            "Queue length (high-low)", "Queue length (low-high)", "Capacity (high-low)", "Capacity (low-high)", "Name (A-Z)"});
    private final DefaultTableModel rideModel = model("ID", "Name", "Category", "Min Ht", "Max Ht", "Min Age", "Capacity", "Queue", "On Board");
    private final JTable rideTable = new JTable(rideModel);
    private List<Ride> shownRides;

    // Visitors & tickets tab
    private final JTextField vName = new JTextField(), vAge = new JTextField(), vHeight = new JTextField();
    private final JTextField vSearch = new JTextField(12);
    private final JTextField tPrice = new JTextField("500"), tDate = new JTextField(LocalDate.now().toString());
    private final DefaultTableModel visitorModel = model("ID", "Name", "Age", "Height (cm)");
    private final JTable visitorTable = new JTable(visitorModel);
    private final DefaultTableModel ticketModel = model("Ticket", "Visitor", "Price", "Valid Date", "Status", "Rides Taken");
    private final JTable ticketTable = new JTable(ticketModel);
    private List<Visitor> shownVisitors;
    private List<Ticket> shownTickets;

    // Queue tab
    private final JComboBox<String> qRide = new JComboBox<>();
    private final JTextField qTicket = new JTextField(8);
    private final JLabel qStatus = new JLabel(" ");
    private final DefaultTableModel queueModel = model("#", "Ticket", "Visitor", "Height", "Age");
    private final DefaultTableModel boardModel = model("Ticket", "Visitor");
    private final JTextArea reportArea = new JTextArea();

    public ParkGUI(ParkService service) {
        super("Amusement Park Ride & Ticket Management System");
        this.service = service;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Rides", buildRideTab());
        tabs.addTab("Visitors & Tickets", buildVisitorTab());
        tabs.addTab("Queue & Operations", buildQueueTab());
        tabs.addTab("Reports", buildReportTab());
        tabs.addChangeListener(e -> refreshAll());
        add(tabs);
        refreshAll();
    }

    // ------------------------------------------------------------------ helpers
    private static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private void guard(Action a) {
        try {
            a.run();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid whole numbers.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Date must be in yyyy-MM-dd format.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (RestrictionViolationException | OverCapacityException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Safety / Capacity Violation", JOptionPane.WARNING_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        refreshAll();
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    private static int num(JTextField f) { return Integer.parseInt(f.getText().trim()); }

    private static JPanel formGrid(Object... labelsAndFields) {
        JPanel p = new JPanel(new GridLayout(0, 4, 8, 6));
        for (Object o : labelsAndFields) p.add(o instanceof String ? new JLabel((String) o) : (Component) o);
        return p;
    }

    private static JPanel flow(Component... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        for (Component c : comps) p.add(c);
        return p;
    }

    private JButton button(String text, Action a) {
        JButton b = new JButton(text);
        b.addActionListener(e -> guard(a));
        return b;
    }

    private String selectedRideId() {
        Object sel = qRide.getSelectedItem();
        if (sel == null) throw new IllegalArgumentException("Please add/select a ride first.");
        return sel.toString().split(" - ")[0];
    }

    // ------------------------------------------------------------------ Rides tab
    private JPanel buildRideTab() {
        JPanel form = formGrid("Ride ID", rId, "Name", rName, "Category", rCat, "Capacity (per cycle)", rCap,
                "Min height (cm)", rMinH, "Max height (cm, 0 = none)", rMaxH, "Min age", rMinAge, "Max queue length", rMaxQ);

        JPanel buttons = flow(
                button("Add Ride", () -> {
                    service.addRide(new Ride(rId.getText(), rName.getText(), (RideCategory) rCat.getSelectedItem(),
                            num(rMinH), num(rMaxH), num(rMinAge), num(rCap), num(rMaxQ)));
                    clearRideForm();
                }),
                button("Update Selected", () -> {
                    service.updateRide(rId.getText(), rName.getText(), (RideCategory) rCat.getSelectedItem(),
                            num(rMinH), num(rMaxH), num(rMinAge), num(rCap), num(rMaxQ));
                    info("Ride updated.");
                }),
                button("Delete Selected", () -> {
                    if (JOptionPane.showConfirmDialog(this, "Delete ride " + rId.getText() + "?", "Confirm",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        service.deleteRide(rId.getText());
                        clearRideForm();
                    }
                }),
                button("Clear Form", this::clearRideForm));

        JPanel searchSort = flow(new JLabel("Search:"), rSearch,
                button("Search", () -> { shownRides = service.searchRides(rSearch.getText()); fillRideTable(); }),
                button("Show All", () -> { rSearch.setText(""); shownRides = null; }),
                new JLabel("   Sort by:"), rSort,
                button("Sort", this::applySort));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBorder(BorderFactory.createTitledBorder("Ride Details"));
        north.add(form);
        north.add(buttons);
        north.add(searchSort);

        rideTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rideTable.getSelectionModel().addListSelectionListener(e -> {
            int row = rideTable.getSelectedRow();
            if (e.getValueIsAdjusting() || row < 0 || shownRides == null || row >= shownRides.size()) return;
            Ride r = shownRides.get(row);
            rId.setText(r.getId());
            rName.setText(r.getName());
            rCat.setSelectedItem(r.getCategory());
            rMinH.setText("" + r.getMinHeightCm());
            rMaxH.setText("" + r.getMaxHeightCm());
            rMinAge.setText("" + r.getMinAge());
            rCap.setText("" + r.getCapacity());
            rMaxQ.setText("" + r.getMaxQueueLength());
        });

        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(north, BorderLayout.NORTH);
        p.add(new JScrollPane(rideTable), BorderLayout.CENTER);
        return p;
    }

    private void clearRideForm() {
        rId.setText(""); rName.setText(""); rMinH.setText("0"); rMaxH.setText("0");
        rMinAge.setText("0"); rCap.setText(""); rMaxQ.setText(""); rCat.setSelectedIndex(0);
        rideTable.clearSelection();
    }

    private void applySort() {
        switch (rSort.getSelectedIndex()) {
            case 0: shownRides = service.sortedByQueueLength(true); break;
            case 1: shownRides = service.sortedByQueueLength(false); break;
            case 2: shownRides = service.sortedByCapacity(true); break;
            case 3: shownRides = service.sortedByCapacity(false); break;
            default: shownRides = service.sortedByName();
        }
    }

    private void fillRideTable() {
        if (shownRides == null) shownRides = service.getAllRides();
        rideModel.setRowCount(0);
        for (Ride r : shownRides) {
            rideModel.addRow(new Object[]{r.getId(), r.getName(), r.getCategory(), r.getMinHeightCm(),
                    r.getMaxHeightCm() == 0 ? "-" : r.getMaxHeightCm(), r.getMinAge(), r.getCapacity(),
                    r.getQueueLength() + "/" + r.getMaxQueueLength(), r.getOnboardCount() + "/" + r.getCapacity()});
        }
    }

    // ------------------------------------------------------------------ Visitors & tickets tab
    private JPanel buildVisitorTab() {
        JPanel vForm = formGrid("Name", vName, "Age", vAge, "Height (cm)", vHeight, "", new JLabel());
        JPanel vButtons = flow(
                button("Register Visitor", () -> {
                    Visitor v = service.registerVisitor(vName.getText(), num(vAge), num(vHeight));
                    vName.setText(""); vAge.setText(""); vHeight.setText("");
                    info("Registered " + v);
                }),
                button("Update Selected", () -> {
                    service.updateVisitor(selectedVisitor().getId(), vName.getText(), num(vAge), num(vHeight));
                }),
                button("Delete Selected", () -> {
                    Visitor v = selectedVisitor();
                    if (JOptionPane.showConfirmDialog(this, "Delete " + v + " and their tickets?", "Confirm",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) service.deleteVisitor(v.getId());
                }),
                new JLabel("   Search:"), vSearch,
                button("Search", () -> { shownVisitors = service.searchVisitors(vSearch.getText()); }),
                button("Show All", () -> { vSearch.setText(""); shownVisitors = null; }));

        JPanel vTop = new JPanel();
        vTop.setLayout(new BoxLayout(vTop, BoxLayout.Y_AXIS));
        vTop.setBorder(BorderFactory.createTitledBorder("Visitors"));
        vTop.add(vForm);
        vTop.add(vButtons);

        visitorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        visitorTable.getSelectionModel().addListSelectionListener(e -> {
            int row = visitorTable.getSelectedRow();
            if (e.getValueIsAdjusting() || row < 0 || shownVisitors == null || row >= shownVisitors.size()) return;
            Visitor v = shownVisitors.get(row);
            vName.setText(v.getName()); vAge.setText("" + v.getAge()); vHeight.setText("" + v.getHeightCm());
        });

        JPanel tForm = flow(new JLabel("Price:"), tPrice, new JLabel("Valid date (yyyy-MM-dd):"), tDate,
                button("Sell Ticket to Selected Visitor", () -> {
                    Visitor v = selectedVisitor();
                    Ticket t = service.sellTicket(v.getId(), Double.parseDouble(tPrice.getText().trim()),
                            LocalDate.parse(tDate.getText().trim()));
                    info("Ticket " + t.getId() + " issued to " + v.getName() + ".");
                }),
                button("Cancel Selected Ticket", () -> {
                    int row = ticketTable.getSelectedRow();
                    if (row < 0 || row >= shownTickets.size()) throw new IllegalArgumentException("Select a ticket first.");
                    service.cancelTicket(shownTickets.get(row).getId());
                }));
        tPrice.setColumns(7);
        tDate.setColumns(9);

        JPanel tPanel = new JPanel(new BorderLayout());
        tPanel.setBorder(BorderFactory.createTitledBorder("Tickets"));
        tPanel.add(tForm, BorderLayout.NORTH);
        ticketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tPanel.add(new JScrollPane(ticketTable), BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        top.add(vTop, BorderLayout.NORTH);
        top.add(new JScrollPane(visitorTable), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, tPanel);
        split.setResizeWeight(0.5);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(split, BorderLayout.CENTER);
        return p;
    }

    private Visitor selectedVisitor() {
        int row = visitorTable.getSelectedRow();
        if (row < 0 || shownVisitors == null || row >= shownVisitors.size())
            throw new IllegalArgumentException("Select a visitor in the table first.");
        return shownVisitors.get(row);
    }

    // ------------------------------------------------------------------ Queue tab
    private JPanel buildQueueTab() {
        qRide.addActionListener(e -> refreshQueueView());

        JPanel controls = flow(new JLabel("Ride:"), qRide, new JLabel("Ticket ID:"), qTicket,
                button("Join Queue", () -> {
                    service.joinQueue(selectedRideId(), qTicket.getText());
                    qTicket.setText("");
                }),
                button("Leave Queue", () -> {
                    if (!service.leaveQueue(selectedRideId(), qTicket.getText()))
                        throw new IllegalStateException("That visitor is not in this queue.");
                }));
        JPanel ops = flow(
                button("Board Next", () -> {
                    if (service.boardNext(selectedRideId()) == null) throw new IllegalStateException("Nobody is waiting in the queue.");
                }),
                button("Board All (to capacity)", () -> {
                    if (service.boardAll(selectedRideId()) == 0) throw new IllegalStateException("No one could be boarded.");
                }),
                button("Complete Ride Cycle", () -> {
                    int n = service.completeCycle(selectedRideId());
                    info(n + " rider(s) finished the ride.");
                }));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBorder(BorderFactory.createTitledBorder("Queue Management & Capacity Monitoring"));
        north.add(controls);
        north.add(ops);
        qStatus.setBorder(BorderFactory.createEmptyBorder(4, 10, 6, 10));
        qStatus.setFont(qStatus.getFont().deriveFont(Font.BOLD));
        north.add(qStatus);

        JPanel left = new JPanel(new BorderLayout());
        left.setBorder(BorderFactory.createTitledBorder("Waiting queue (front first)"));
        left.add(new JScrollPane(new JTable(queueModel)));
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(BorderFactory.createTitledBorder("Currently on the ride"));
        right.add(new JScrollPane(new JTable(boardModel)));

        JPanel center = new JPanel(new GridLayout(1, 2, 8, 8));
        center.add(left);
        center.add(right);

        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(north, BorderLayout.NORTH);
        p.add(center, BorderLayout.CENTER);
        return p;
    }

    private void refreshRideCombo() {
        Object prev = qRide.getSelectedItem();
        java.awt.event.ActionListener[] ls = qRide.getActionListeners();
        for (java.awt.event.ActionListener l : ls) qRide.removeActionListener(l);
        qRide.removeAllItems();
        for (Ride r : service.getAllRides()) qRide.addItem(r.toString());
        if (prev != null) qRide.setSelectedItem(prev);
        for (java.awt.event.ActionListener l : ls) qRide.addActionListener(l);
    }

    private void refreshQueueView() {
        queueModel.setRowCount(0);
        boardModel.setRowCount(0);
        Object sel = qRide.getSelectedItem();
        if (sel == null) { qStatus.setText("No rides available."); return; }
        Ride r;
        try { r = service.getRide(sel.toString().split(" - ")[0]); }
        catch (IllegalArgumentException e) { qStatus.setText(" "); return; }
        int pos = 1;
        for (Ticket t : r.getQueue().toList()) {
            queueModel.addRow(new Object[]{pos++, t.getId(), t.getVisitor().getName(), t.getVisitor().getHeightCm() + " cm", t.getVisitor().getAge()});
        }
        for (Ticket t : r.getOnboard()) boardModel.addRow(new Object[]{t.getId(), t.getVisitor().getName()});
        String warn = "";
        if (r.getOnboardCount() >= r.getCapacity()) warn += "   [RIDE FULL]";
        if (r.getQueue().isFull()) warn += "   [QUEUE FULL]";
        qStatus.setText(r.getName() + "  |  Queue " + r.getQueueLength() + "/" + r.getMaxQueueLength()
                + "  |  On board " + r.getOnboardCount() + "/" + r.getCapacity() + warn);
    }

    // ------------------------------------------------------------------ Reports tab
    private JPanel buildReportTab() {
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.add(flow(button("Refresh Report", () -> { })), BorderLayout.NORTH);
        p.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        return p;
    }

    // ------------------------------------------------------------------ refresh
    private void refreshAll() {
        fillRideTable();

        if (shownVisitors == null) shownVisitors = service.getAllVisitors();
        visitorModel.setRowCount(0);
        for (Visitor v : shownVisitors) visitorModel.addRow(new Object[]{v.getId(), v.getName(), v.getAge(), v.getHeightCm()});

        shownTickets = service.getAllTickets();
        ticketModel.setRowCount(0);
        for (Ticket t : shownTickets) {
            ticketModel.addRow(new Object[]{t.getId(), t.getVisitor().getName() + " (" + t.getVisitor().getId() + ")",
                    String.format("%.2f", t.getPrice()), t.getValidDate(), t.status(), t.getRidesTaken()});
        }

        refreshRideCombo();
        refreshQueueView();
        reportArea.setText(service.generateReport());
        reportArea.setCaretPosition(0);
    }
}
