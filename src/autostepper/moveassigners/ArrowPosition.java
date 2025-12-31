package autostepper.moveassigners;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum ArrowPosition {
    LEFT(0),
    DOWN(1),
    UP(2),
    RIGHT(3),
    NOWHERE(4);

    private final int value;

    ArrowPosition(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    private static final Map<Integer, ArrowPosition> BY_VALUE =
            Arrays.stream(values())
                  .collect(Collectors.toMap(ArrowPosition::value, e -> e));

    public static ArrowPosition fromValue(int value) {
        return BY_VALUE.get(value); // may return null
    }
}