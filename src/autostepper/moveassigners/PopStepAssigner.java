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

            Map<VibeScore, Integer> previousPreviousVibe;
            if (i - 2 >= 0)
            {
                previousPreviousVibe = NoteVibes.get(i - 2);
            }
            else
            {
                previousPreviousVibe = new HashMap<>();
                previousPreviousVibe.put(VibeScore.POWER, 0);
                previousPreviousVibe.put(VibeScore.SUSTAIN, 0);
            }

            Map<VibeScore, Integer> previousVibe;
            if (i - 1 >= 0)
            {
                previousVibe = NoteVibes.get(i - 1);
            }
            else
            {
                previousVibe = new HashMap<>();
                previousVibe.put(VibeScore.POWER, 0);
                previousVibe.put(VibeScore.SUSTAIN, 0);
            }
            Map<VibeScore, Integer> currentVibe = NoteVibes.get(i);
            Map<VibeScore, Integer> nextVibe;
            if (i + 1 < NoteVibes.size())
            {
                nextVibe = NoteVibes.get(i + 1);
            }
            else
            {
                nextVibe = new HashMap<>();
                nextVibe.put(VibeScore.POWER, 0);
                nextVibe.put(VibeScore.SUSTAIN, 0);
            }
            Map<VibeScore, Integer> nextNextVibe;
            if (i + 2 < NoteVibes.size())
            {
                nextNextVibe = NoteVibes.get(i + 2);
            }
            else
            {
                nextNextVibe = new HashMap<>();
                nextNextVibe.put(VibeScore.POWER, 0);
                nextNextVibe.put(VibeScore.SUSTAIN, 0);
            }

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

}
