package autostepper.genetic;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import autostepper.moveassigners.ArrowPosition;

public enum AlgorithmParameter {
    JUMP_THRESHOLD(0),
    TAP_THRESHOLD(1),
    SUSTAIN_THESHOLD(2),
    SUSTAIN_FACTOR(3),
    GRANULARITY_MODIFIER(4),
    PRECISE_GRANULARITY_MODIFIER(5),
    FIRST_VOLUME_THRESHOLD(6),
    SECOND_VOLUME_THRESHOLD(7),
    FFT_MAX_THRESHOLD(8),
    KICK_LOW_FREQ(9),
    KICK_HIGH_FREQ(10),
    KICK_BAND_FREQ(11),
    SNARE_LOW_FREQ(12),
    SNARE_HIGH_FREQ(13),
    SNARE_BAND_FREQ(14);

    private final int value;

    AlgorithmParameter(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static Optional<Float> maxValueForFloatParameter(AlgorithmParameter param)
    {
        switch (param) {
            case SUSTAIN_FACTOR:
                    return Optional.of(1f);
            case GRANULARITY_MODIFIER:
                    return Optional.of(1.2f);
            case PRECISE_GRANULARITY_MODIFIER:
                    return Optional.of(1f);
            case FIRST_VOLUME_THRESHOLD:
                    return Optional.of(0.9f);
            case SECOND_VOLUME_THRESHOLD:
                    return Optional.of(1f);
            case FFT_MAX_THRESHOLD:
                    return Optional.of(1f);
            default:
                    return Optional.empty();
        }
    }

    
    public static Optional<Float> minValueForFloatParameter(AlgorithmParameter param)
    {
        switch (param) {
            case SUSTAIN_FACTOR:
                    return Optional.of(0.1f);
            case GRANULARITY_MODIFIER:
                    return Optional.of(0.8f);
            case PRECISE_GRANULARITY_MODIFIER:
                    return Optional.of(0.5f);
            case FIRST_VOLUME_THRESHOLD:
                    return Optional.of(0.2f);
            case SECOND_VOLUME_THRESHOLD:
                    return Optional.of(0.3f);
            case FFT_MAX_THRESHOLD:
                    return Optional.of(0.3f);
            default:
                    return Optional.empty();
        }
    }

    public static Optional<Integer> maxValueForIntParameter(AlgorithmParameter param)
    {
        switch (param) {
            case JUMP_THRESHOLD:
                    return Optional.of(10);
            case TAP_THRESHOLD:
                    return Optional.of(9);
            case SUSTAIN_THESHOLD:
                    return Optional.of(10);
            case KICK_LOW_FREQ:
                    return Optional.of(10);
            case KICK_HIGH_FREQ:
                    return Optional.of(10);
            case KICK_BAND_FREQ:
                    return Optional.of(10);
            case SNARE_LOW_FREQ:
                    return Optional.of(4);
            case SNARE_HIGH_FREQ:
                    return Optional.of(40);
            case SNARE_BAND_FREQ:
                    return Optional.of(10);
            default:
                    return Optional.empty();
        }
    }

    public static Optional<Integer> minValueForIntParameter(AlgorithmParameter param)
    {
        switch (param) {
            case JUMP_THRESHOLD:
                    return Optional.of(2);
            case TAP_THRESHOLD:
                    return Optional.of(1);
            case SUSTAIN_THESHOLD:
                    return Optional.of(1);
            case KICK_LOW_FREQ:
                    return Optional.of(1);
            case KICK_HIGH_FREQ:
                    return Optional.of(4);
            case KICK_BAND_FREQ:
                    return Optional.of(1);
            case SNARE_LOW_FREQ:
                    return Optional.of(4);
            case SNARE_HIGH_FREQ:
                    return Optional.of(20);
            case SNARE_BAND_FREQ:
                    return Optional.of(1);
            default:
                    return Optional.empty();
        }
    }

    private static final Map<Integer, AlgorithmParameter> BY_VALUE =
            Arrays.stream(values())
                  .collect(Collectors.toMap(AlgorithmParameter::value, e -> e));

    public static AlgorithmParameter fromValue(int value) {
        return BY_VALUE.get(value); // may return null
    }
}
