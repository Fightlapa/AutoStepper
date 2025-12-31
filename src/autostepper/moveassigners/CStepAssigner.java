package autostepper.moveassigners;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import autostepper.AutoStepper;
import autostepper.moveassigners.steporganizer.Foot;
import autostepper.vibejudges.VibeScore;

public abstract class CStepAssigner {
    static int ACTION_HOLD = 0;
    static int TAP = 1;
    static int JUMP = 2;
    static int DOUBLE_HOLD = 0;

    static public char EMPTY = '0', STEP = '1', HOLD = '2', STOP = '3', MINE = 'M', KEEP_HOLDING = 'H';

    String name;
    Random rand;

    public CStepAssigner(String name)
    {
        if (AutoStepper.RANDOMIZED )
        {
            rand = new Random();
        }
        else
        {
            rand = new Random(123L);
        }
        this.name = name;
    }

    public String AssignerName()
    {
        return name;
    }
  

    public abstract ArrayList<ArrayList<Character>> AssignMoves(ArrayList<Map<VibeScore, Integer>> NoteVibes, SimfileDifficulty difficulty, float totalTime);
}
