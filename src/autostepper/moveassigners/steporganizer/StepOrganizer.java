package autostepper.moveassigners.steporganizer;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;

import autostepper.moveassigners.ArrowPosition;
import autostepper.moveassigners.CStepAssigner;

public class StepOrganizer {
    // To encourage jumps to be <> instead of ^>  etc.
    private static int PREDEFINED_CHOICE_THRESHOLD = 5;
    private static Random rand = new Random(125L);

    Foot lastUsedFoot = Foot.LEFT;
    ArrowPosition leftFoot = ArrowPosition.LEFT;
    ArrowPosition rightFoot = ArrowPosition.RIGHT;
    ArrowPosition footLocked = ArrowPosition.NOWHERE;
    private int linesFromPredefinedChoice = 999;

    public ArrayList<ArrowPosition> getAvailableArrow(int numberOfArrows, ArrayList<Character> arrowLine, Optional<ArrayList<Character>> previousLine, boolean footLock)
    {
        ArrayList<ArrowPosition> result = new ArrayList<>();
        int arrowsLeft = numberOfArrows;

        while (arrowsLeft > 0)
        {
            // It's preferred to see symetrical duo, so that's what's happening there
            if (arrowsLeft == 2
                && linesFromPredefinedChoice > PREDEFINED_CHOICE_THRESHOLD
                && arrowLine.get(ArrowPosition.LEFT.value()) == CStepAssigner.EMPTY
                && arrowLine.get(ArrowPosition.RIGHT.value()) == CStepAssigner.EMPTY )
            {
                result.add(ArrowPosition.LEFT);
                result.add(ArrowPosition.RIGHT);
                arrowsLeft--;
                arrowsLeft--;
            }
            else
            {
                ArrowPosition attempt = ArrowPosition.fromValue(rand.nextInt(4));
                if ((lastUsedFoot == Foot.LEFT && leftFoot == ArrowPosition.DOWN && attempt == ArrowPosition.LEFT)
                    || (lastUsedFoot == Foot.RIGHT && rightFoot == ArrowPosition.DOWN && attempt == ArrowPosition.RIGHT)
                    || (lastUsedFoot == Foot.LEFT && leftFoot == ArrowPosition.UP && attempt == ArrowPosition.LEFT)
                    || (lastUsedFoot == Foot.RIGHT && rightFoot == ArrowPosition.UP && attempt == ArrowPosition.RIGHT))
                {
                    continue; // Avoid crossovers, for now
                }

                if( arrowLine.get(attempt.value()) != CStepAssigner.EMPTY)
                {
                    continue; // Already busy
                }
                else if (previousLine.isPresent() && previousLine.get().get(attempt.value()) != CStepAssigner.EMPTY)
                {
                    // Don't do same arrow twice
                    continue;
                }
                else
                {
                    result.add(attempt);
                    arrowsLeft--;
                    SwitchFoot();
                }
            }
        }

        return result;
    }

    void SwitchFoot()
    {
        if (footLocked != ArrowPosition.NOWHERE)
        {
            if (lastUsedFoot == Foot.LEFT)
            {
                lastUsedFoot = Foot.RIGHT;
            }
            else
            {
                lastUsedFoot = Foot.LEFT;
            }
        }
    }

    public void unlock(ArrowPosition location)
    {
        if (footLocked == location)
        {
            footLocked = ArrowPosition.NOWHERE;
        }
    }
}
