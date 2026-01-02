package autostepper.moveassigners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import autostepper.moveassigners.steporganizer.Foot;
import autostepper.moveassigners.steporganizer.StepOrganizer;
import autostepper.vibejudges.VibeScore;

public class ParametrizedAssigner extends CStepAssigner {

    private int jumpThreshold;
    private int tapThreshold;
    private int sustainThreshold;

    public ParametrizedAssigner(int jumpThreshold, int tapThreshold, int sustainThreshold) {
        super("Parametrized assigned jumpsThr: " + jumpThreshold + " tapThr: " + tapThreshold + " sustainThr: " + sustainThreshold);
        assert(tapThreshold < jumpThreshold);
        this.jumpThreshold = jumpThreshold;
        this.tapThreshold = tapThreshold;
        this.sustainThreshold = sustainThreshold;
    }

    @Override
    public ArrayList<ArrayList<Character>> AssignMoves(ArrayList<Map<VibeScore, Integer>> NoteVibes,
            SimfileDifficulty difficulty, float totalTime)
    {
        StepOrganizer stepOrganizer = new StepOrganizer();
        ArrayList<ArrayList<Character>> NoteMoves = new ArrayList<>();
        // Here it will work in context windows
        // To start with something, it will search for patterns of 3 and 4 combos
        // Meaning steps without empty line inbetween
        int steps = 0;
        int numberOfRows = NoteVibes.size();
        for (int i = 0; i < numberOfRows; i++)
        {
            Optional<ArrayList<Character>> previousLine;
            if (i > 0)
            {
                previousLine = Optional.of(NoteMoves.get(i - 1));
            }
            else
            {
                previousLine = Optional.empty();
            }
            List<Integer> actions = new ArrayList<>();

            Map<VibeScore, Integer> previousPreviousVibe = getVibe(NoteVibes, i, -2);
            Map<VibeScore, Integer> previousVibe = getVibe(NoteVibes, i, -1);
            Map<VibeScore, Integer> currentVibe = NoteVibes.get(i);
            Map<VibeScore, Integer> nextVibe = getVibe(NoteVibes, i, 1);
            Map<VibeScore, Integer> nextNextVibe = getVibe(NoteVibes, i, 2);

            if (currentVibe.get(VibeScore.POWER) >= jumpThreshold)
            {
                if ((nextVibe.get(VibeScore.POWER) < jumpThreshold) && (nextNextVibe.get(VibeScore.POWER) < jumpThreshold))
                {
                    actions.add(JUMP);
                }
                else
                {
                    actions.add(TAP);
                }
            }
            else if (currentVibe.get(VibeScore.POWER) >= tapThreshold)
            {
                actions.add(TAP);
            }
            if (currentVibe.get(VibeScore.SUSTAIN) >= sustainThreshold)
            {
                if (actions.contains(JUMP))
                {
                    actions.add(DOUBLE_HOLD);
                    actions.remove((Object)JUMP);
                    actions.remove((Object)TAP);
                }
                else
                {
                    actions.add(ACTION_HOLD);
                    actions.remove((Object)TAP);
                }
            }

            ArrayList<Character> arrowLine = new ArrayList<>(List.of('0', '0', '0', '0'));

            boolean anyHolds = false;
            // First maintain holds if there are any
            anyHolds = maintainHolds(stepOrganizer, NoteMoves, i, actions, arrowLine, anyHolds);
            
            if (!anyHolds)
            {
                // Start holding
                if (actions.contains(ACTION_HOLD))
                {
                    ArrayList<ArrowPosition> stepPosition = stepOrganizer.getAvailableArrow(1, arrowLine, previousLine, true);
                    for (ArrowPosition arrowPosition : stepPosition) {
                        arrowLine.set(arrowPosition.value(), HOLD);
                    }
                }
                else if (actions.contains(DOUBLE_HOLD))
                {
                    // For double hold, we can consider nothing locked as you have to use hands anyways
                    ArrayList<ArrowPosition> stepPosition = stepOrganizer.getAvailableArrow(2, arrowLine, previousLine, false);
                    for (ArrowPosition arrowPosition : stepPosition) {
                        arrowLine.set(arrowPosition.value(), HOLD);
                    }
                }
            }

            int numberOfHolds = getNumberOfHolds(arrowLine);
            if (numberOfHolds == 2)
            {
                actions.remove((Object)JUMP);
                actions.remove((Object)TAP);
            }
            else if (numberOfHolds == 1)
            {
                actions.remove((Object)JUMP);
            }

            int arrows = 0;
            if (actions.contains(JUMP))
            {
                arrows = 2;
            }
            else if (actions.contains(TAP))
            {
                arrows = 1;
            }

            ArrayList<ArrowPosition> stepPositions;
            if (i > 0)
            {
                stepPositions = stepOrganizer.getAvailableArrow(arrows, arrowLine, previousLine, false);
            }
            else
            {
                stepPositions = stepOrganizer.getAvailableArrow(arrows, arrowLine, previousLine, false);
            }

            for (ArrowPosition arrowPosition : stepPositions) {
                arrowLine.set(arrowPosition.value(), STEP);
                steps++;
            }

            NoteMoves.add(arrowLine);
        }
        System.out.println("Steps: " + steps);
        return NoteMoves;
    }

    private int getNumberOfHolds(ArrayList<Character> arrowLine) {
        int count = 0;
        for (char c : arrowLine) {
            if (c == HOLD || c == STOP || c == KEEP_HOLDING) {
                count++;
            }
        }
        return count;
    }

    private boolean maintainHolds(StepOrganizer stepOrganizer, ArrayList<ArrayList<Character>> NoteMoves, int i, List<Integer> actions,
            ArrayList<Character> arrowLine, boolean anyHolds) {
        if (i - 1 >= 0)
        {
            for (int j = 0; j < 4; j++)
            {
                if (NoteMoves.get(i - 1).get(j) == HOLD)
                {
                    anyHolds = true;
                    if (actions.contains(ACTION_HOLD) || actions.contains(DOUBLE_HOLD))
                    {
                        arrowLine.set(j, KEEP_HOLDING);
                    }
                    else
                    {
                        arrowLine.set(j, STOP);
                        stepOrganizer.unlock(ArrowPosition.fromValue(j));
                    }
                }
                if (NoteMoves.get(i - 1).get(j) == KEEP_HOLDING)
                {
                    anyHolds = true;
                    if (actions.contains(ACTION_HOLD))
                    {
                        arrowLine.set(j, KEEP_HOLDING);
                    }
                    else
                    {
                        arrowLine.set(j, STOP);
                        stepOrganizer.unlock(ArrowPosition.fromValue(j));
                    }
                }
            }
        }
        return anyHolds;
    }

    private Map<VibeScore, Integer> getVibe(ArrayList<Map<VibeScore, Integer>> NoteVibes, int currentVibeIdx, int offset) {
        Map<VibeScore, Integer> vibe;
        if (currentVibeIdx + offset >= 0 && currentVibeIdx + offset < NoteVibes.size())
        {
            vibe = NoteVibes.get(currentVibeIdx + offset);
        }
        else
        {
            vibe = new HashMap<>();
            vibe.put(VibeScore.POWER, 0);
            vibe.put(VibeScore.SUSTAIN, 0);
        }
        return vibe;
    }

}
