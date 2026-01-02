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

    public int getExpectedStepsPerMinute()
    {
        switch (this) {
            case BEGINNER: return 55;
            case EASY: return 80;
            case MEDIUM: return 105;
            case HARD: return 170;
            case CHALLENGE: return 190;
            default: throw new RuntimeException("Wrong difficulty");
        }
    }
}