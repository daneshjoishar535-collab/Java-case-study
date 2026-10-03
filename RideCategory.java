/** Category of a ride. */
public enum RideCategory {
    THRILL("Thrill"),
    FAMILY("Family"),
    KIDS("Kids");

    private final String label;

    RideCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
