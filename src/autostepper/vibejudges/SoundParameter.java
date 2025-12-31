package autostepper.vibejudges;

public enum SoundParameter {
    KICKS(0),
    BEAT(1),
    SNARE(2),
    HAT(3),
    SUSTAINED(4),
    SILENCE(5),
    NOTHING(6),
    HALF_BEAT(7),
    FFT_MAX(8),
    FFT_AVG(9),
    VOLUME(10);

    private final int value;

    SoundParameter(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}