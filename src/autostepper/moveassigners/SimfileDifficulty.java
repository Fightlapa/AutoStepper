package autostepper.moveassigners;

public enum SimfileDifficulty {
    BEGINNER(1),
    EASY(2),
    MEDIUM(3),
    HARD(4),
    CHALLENGE(5);

    private final int value;

    SimfileDifficulty(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}