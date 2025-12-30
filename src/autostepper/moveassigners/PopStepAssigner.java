package autostepper.moveassigners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import autostepper.vibejudges.VibeScore;

public class PopStepAssigner extends CStepAssigner {

    @Override
    public ArrayList<ArrayList<Character>> AssignMoves(ArrayList<Map<VibeScore, Integer>> NoteVibes,
            SimfileDifficulty difficulty, float totalTime)
    {
        reset();
        ArrayList<ArrayList<Character>> NoteMoves = new ArrayList<>();
        // Here it will work in context windows
        // To start with something, it will search for patterns of 3 and 4 combos
        // Meaning steps without empty line inbetween
        int numberOfRows = NoteVibes.size();
        for (int i = 0; i < numberOfRows; i++)
        {
            List<Integer> actions = new ArrayList<>();

            Map<VibeScore, Integer> previousPreviousVibe = getVibe(NoteVibes, i, -2);
            Map<VibeScore, Integer> previousVibe = getVibe(NoteVibes, i, -1);
            Map<VibeScore, Integer> currentVibe = NoteVibes.get(i);
            Map<VibeScore, Integer> nextVibe = getVibe(NoteVibes, i, 1);
            Map<VibeScore, Integer> nextNextVibe = getVibe(NoteVibes, i, 2);

            if (currentVibe.get(VibeScore.POWER) > 2)
            {
                if ((i + 1 <= numberOfRows && (nextVibe.get(VibeScore.POWER) < 2) && (nextNextVibe.get(VibeScore.POWER) < 2)))
                {
                    actions.add(JUMP);
                }
                else
                {
                    actions.add(TAP);
                }
            }
            else if (currentVibe.get(VibeScore.POWER) == 1)
            {
                actions.add(TAP);
            }
            if (currentVibe.get(VibeScore.SUSTAIN) > 0)
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
            anyHolds = maintainHolds(NoteMoves, i, actions, arrowLine, anyHolds);
            
            if (!anyHolds && (actions.contains(ACTION_HOLD) || actions.contains(DOUBLE_HOLD)))
            {
                // Start holding
                footLocked = true;
                ArrowPosition stepPosition = getAvailableArrow(arrowLine);
                arrowLine.set(stepPosition.value(), HOLD);
                SwitchFoot();

                if (actions.contains(DOUBLE_HOLD))
                {
                    // Start holding
                    footLocked = true;
                    stepPosition = getAvailableArrow(arrowLine);
                    arrowLine.set(stepPosition.value(), HOLD);
                    SwitchFoot();
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

            while(arrows > 0)
            {
                ArrowPosition stepPosition;

                stepPosition = ArrowPosition.fromValue(rand.nextInt(4));
                if( arrowLine.get(stepPosition.value()) != EMPTY)
                {
                    continue; // Already busy
                }
                if(i - 1 >= 0 && (NoteMoves.get(i - 1).get(stepPosition.value()) == HOLD || NoteMoves.get(i - 1).get(stepPosition.value()) == KEEP_HOLDING))
                {
                    // Already holds
                    continue;
                }
                else if ((lastUsedFoot == Foot.LEFT && leftFoot == ArrowPosition.DOWN && stepPosition == ArrowPosition.LEFT)
                    || (lastUsedFoot == Foot.RIGHT && rightFoot == ArrowPosition.DOWN && stepPosition == ArrowPosition.RIGHT)
                    || (lastUsedFoot == Foot.LEFT && leftFoot == ArrowPosition.UP && stepPosition == ArrowPosition.LEFT)
                    || (lastUsedFoot == Foot.RIGHT && rightFoot == ArrowPosition.UP && stepPosition == ArrowPosition.RIGHT))
                {
                    continue; // Avoid crossovers, for now
                }
                else
                {
                    SwitchFoot();
                    arrowLine.set(stepPosition.value(), STEP);
                }
                
                arrows--;
            }
            NoteMoves.add(arrowLine);
        }
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

    private boolean maintainHolds(ArrayList<ArrayList<Character>> NoteMoves, int i, List<Integer> actions,
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
                        footLocked = false;
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
                        footLocked = false;
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
