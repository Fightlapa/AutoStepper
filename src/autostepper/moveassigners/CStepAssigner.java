package autostepper.moveassigners;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import autostepper.AutoStepper;
import autostepper.vibejudges.VibeScore;

public abstract class CStepAssigner {
    static int ACTION_HOLD = 0;
    static int TAP = 1;
    static int JUMP = 2;
    static int DOUBLE_HOLD = 0;

    static public char EMPTY = '0', STEP = '1', HOLD = '2', STOP = '3', MINE = 'M', KEEP_HOLDING = 'H';

    Foot lastUsedFoot = Foot.LEFT;
    boolean footLocked = false; // For example during hold
    ArrowPosition leftFoot = ArrowPosition.LEFT;
    ArrowPosition rightFoot = ArrowPosition.RIGHT;
    
    Random rand;

    public CStepAssigner()
    {
        if (AutoStepper.RANDOMIZED )
        {
            rand = new Random();
        }
        else
        {
            rand = new Random(123L);
        }
    }
  

    public abstract ArrayList<ArrayList<Character>> AssignMoves(ArrayList<Map<VibeScore, Integer>> NoteVibes, SimfileDifficulty difficulty, float totalTime);

    ArrowPosition getAvailableArrow(ArrayList<Character> arrowLine)
    {
        ArrowPosition stepPosition = null;
        boolean found = false;

        while (!found)
        {
            stepPosition = ArrowPosition.fromValue(rand.nextInt(4));
            if( arrowLine.get(stepPosition.value()) != EMPTY)
            {
                continue; // Already busy
            }
            else
            {
                found = true;
            }
        }

        return stepPosition;
    }

    void SwitchFoot()
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

    void reset()
    {
        lastUsedFoot = Foot.LEFT;
        footLocked = false;
        leftFoot = ArrowPosition.LEFT;
        rightFoot = ArrowPosition.RIGHT;
    }
}
