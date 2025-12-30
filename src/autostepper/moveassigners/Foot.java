package autostepper.moveassigners;

public enum Foot {
    LEFT(0),
    RIGHT(1);
    private final int value;

    Foot(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}