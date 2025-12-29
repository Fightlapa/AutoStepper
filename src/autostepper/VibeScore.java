package autostepper;

public enum VibeScore {
    POWER(0),
    SUSTAIN(1);

    private final int value;

    VibeScore(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}