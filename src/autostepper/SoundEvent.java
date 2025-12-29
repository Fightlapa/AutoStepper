package autostepper;

public enum SoundEvent {
    KICKS(0),
    BEAT(1),
    SNARE(2),
    HAT(3),
    SUSTAINED(4),
    SILENCE(5),
    NOTHING(6),
    HALF_BEAT(7);

    private final int value;

    SoundEvent(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}