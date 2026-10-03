/** A park visitor. */
public class Visitor {
    private final String id;
    private String name;
    private int age;
    private int heightCm;

    public Visitor(String id, String name, int age, int heightCm) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Visitor ID is required.");
        this.id = id.trim();
        setDetails(name, age, heightCm);
    }

    /** Validates and sets name, age and height. */
    public void setDetails(String name, int age, int heightCm) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Visitor name is required.");
        if (age < 0 || age > 120) throw new IllegalArgumentException("Age must be between 0 and 120.");
        if (heightCm < 30 || heightCm > 250) throw new IllegalArgumentException("Height must be between 30 and 250 cm.");
        this.name = name.trim();
        this.age = age;
        this.heightCm = heightCm;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public int getHeightCm() { return heightCm; }

    @Override
    public String toString() {
        return id + " - " + name + " (" + age + "y, " + heightCm + " cm)";
    }
}
