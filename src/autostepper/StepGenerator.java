package autostepper;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Phr00t
 */
public class StepGenerator {
    
    static private int MAX_HOLD_BEAT_COUNT = 4;
    static private int NO_HOLD = -1;
    static private char EMPTY = '0', STEP = '1', HOLD = '2', STOP = '3', MINE = 'M';

    public static final Map<Integer, List<List<String>>> STEP_COMBOS;

    static {
        Map<Integer, List<List<String>>> map = new HashMap<>();

        // left - down - up - right
        //   0     1     0     1

        // 3-step combos
        List<List<String>> combos_of_three = new ArrayList<>();
        combos_of_three.add(Arrays.asList("1000", "0001", "1001"));
        combos_of_three.add(Arrays.asList("1000", "0001", "0100"));
        map.put(3, combos_of_three);

        // 4-step combos
        List<List<String>> combos_of_four = new ArrayList<>();
        combos_of_four.add(Arrays.asList("1000", "0001", "0010", "0100"));
        combos_of_four.add(Arrays.asList("1000", "0001", "0010", "1001"));
        map.put(4, combos_of_four);
        STEP_COMBOS = Collections.unmodifiableMap(map); // read-only
    }

    
    static Random rand = new Random();
    
    static private int getHoldCount(float[] holding) {
        int ret = 0;
        if( holding[0] > 0f ) ret++;
        if( holding[1] > 0f ) ret++;
        if( holding[2] > 0f ) ret++;
        if( holding[3] > 0f ) ret++;
        return ret;
    }
    
    static private int getRandomHold(float[] holding) {
        int hc = getHoldCount(holding);
        if(hc == 0) return NO_HOLD;
        int pickHold = rand.nextInt(hc);
        for(int i=0;i<4;i++) {
            if( holding[i] > 0f ) {
                if( pickHold == 0 ) return i;
                pickHold--;
            }
        }
        return NO_HOLD;
    }
    
    private static char[] getHoldStops(int currentHoldCount, float time, int newHolds, float[] holding) {
        char[] holdstops = new char[4];
        holdstops[0] = EMPTY;
        holdstops[1] = EMPTY;
        holdstops[2] = EMPTY;
        holdstops[3] = EMPTY;
        if( currentHoldCount > 0 ) {
            while( newHolds < 0 ) {
                int index = getRandomHold(holding);
                if( index == NO_HOLD ) {
                    newHolds = 0;
                    currentHoldCount = 0;
                } else {
                    holding[index] = 0f;
                    holdstops[index] = STOP;
                    newHolds++; currentHoldCount--;
                }
            }
            // if we still have holds, subtract counter until 0
            for(int i=0;i<4;i++) {
                if( holding[i] > 0f ) {
                    holding[i] -= 1f;
                    if( holding[i] <= 0f ) {
                        holding[i] = 0f;
                        holdstops[i] = STOP;
                        currentHoldCount--;
                    }
                } 
            }
        }        
        return holdstops;
    }
    
    private static String getNoteLineIndex(int i, ArrayList<char[]> AllNoteLines) {
        if( i < 0 || i >= AllNoteLines.size() ) return "0000";
        return String.valueOf(AllNoteLines.get(i));
    }
    
    private static String getLastNoteLine(ArrayList<char[]> AllNoteLines) {
        return getNoteLineIndex(AllNoteLines.size()-1, AllNoteLines);
    }

    private static float getMinimumJumpCooldownSeconds(boolean mines) {
        // seconds, if we have mines, minimum interval is shorter (???)
        return mines ? 2f : 4f;
    }

    private static void assignAction(ArrayList<Map<Character, Integer>> AllNoteStepCount, String lastLine, float time, int steps, int newHolds, int mineCount, boolean mines, float[] holding, float lastJumpTime, int holdRun) {
        Map<Character, Integer> currentState;
        Map<Character, Integer> previousState;
        // Map<Character, Integer> previousToPreviousState;

        if( AutoStepper.DEBUG_STEPS ) {
            System.out.println("Input: steps: " + steps + ", newHold: " + newHolds);
        }

        if (AllNoteStepCount.size() > 0)
        {
            previousState = new HashMap<>(AllNoteStepCount.get(AllNoteStepCount.size() - 1));

            // Make a copy of previous state, if it exists, but keep only holds
            currentState = new HashMap<>(AllNoteStepCount.get(AllNoteStepCount.size() - 1));
            currentState.put(STEP, 0);
            currentState.put(MINE, 0);
        }
        else
        {
            // No previous state, everything empty
            previousState = new HashMap<>();
            previousState.put(STEP, 0);
            previousState.put(HOLD, 0);
            previousState.put(MINE, 0);

            currentState = new HashMap<>();
            currentState.put(STEP, 0);
            currentState.put(HOLD, 0);
            currentState.put(MINE, 0);
        }

        int currentHoldCount = currentState.get(HOLD);
        
        if( steps == 0 ) {
            // steps == 0 does not reinforce holding, so let's subtract one if we are holding any step
            if (currentHoldCount > 0)
            {
                currentState.put(HOLD, currentHoldCount - 1);
            }
            AllNoteStepCount.add(currentState);
            if( AutoStepper.DEBUG_STEPS ) {
                System.out.println("Nothing to add");
            }
            return;
        }
        
        if( steps > 1 && time - lastJumpTime < getMinimumJumpCooldownSeconds(mines) )
        {
            steps = 1; // don't spam jumps
            if( AutoStepper.DEBUG_STEPS ) {
                System.out.println("Jump spam prevention");
            }
        }
        if( steps > 2 ) {
            // no hands usage allowed for now, let's reduce it to just jump
            steps = 2;
            lastJumpTime = time;
            if( AutoStepper.DEBUG_STEPS ) {
                System.out.println("Removing hand usage");
            }
        }
        
        // can't hold or step more than 2, for now
        if( newHolds + currentHoldCount > 2 )
        {
            newHolds = 2 - currentHoldCount;
            if( AutoStepper.DEBUG_STEPS ) {
                System.out.println("Removing hand usage from holding");
            }
        }
        if( steps + currentHoldCount > 2 )
        {
            steps = 2 - currentHoldCount;
            if( AutoStepper.DEBUG_STEPS ) {
                System.out.println("Removing hand usage to step while holding");
            }
        }
        // if we have had a run of 3 newHolds, don't make a new hold to prevent player from spinning
        if( holdRun >= 2 && newHolds > 0 )
        {
            newHolds = 0;
            if( AutoStepper.DEBUG_STEPS ) {
                System.out.println("Spin prevention");
            }
        }
        // TODO: To investigate what is happening here
        // // are we stopping newHolds?
        // char[] noteLine = getHoldStops(currentHoldCount, time, newHolds, holding);

        // if we are making a step, but just coming off a hold, move that hold end up to give proper
        // time to make move to new step
        if( steps > 0 && (previousState.get(HOLD) > 0))
        {
            previousState.put(HOLD, 0);
            if( AutoStepper.DEBUG_STEPS ) {
                System.out.println("Stopping hold early to make it easier to switch step");
            }
        }
        int holds = newHolds + previousState.get(HOLD);
        if (holds < 0)
        {
            holds = 0;
        }

        currentState.put(STEP, steps);
        currentState.put(HOLD, holds);
        if( AutoStepper.DEBUG_STEPS ) {
            System.out.println("Current state ends up being: S: " + steps + ", H: " + holds);
        }
        AllNoteStepCount.add(currentState);
    }

    private static char[] greedySingleNote(ArrayList<Map<Character, Integer>> AllNoteStepCount, ArrayList<char[]> AllNoteLines, int i)
    {
        // ok, make the steps
        String completeLine;
        String lastLine;
        char[] orig = new char[4];
        char[] noteLine = new char[4];
        orig[0] = '0';
        orig[1] = '0';
        orig[2] = '0';
        orig[3] = '0';
        float[] willhold = new float[4];
        if (i - 1 >= 0)
        {
            lastLine = new String(AllNoteLines.get(i - 1));
        }
        else
        {
            lastLine = "0000";
        }

        do {
            int stepcount = AllNoteStepCount.get(i).get(STEP);
            int holdcount = AllNoteStepCount.get(i).get(HOLD);
            noteLine[0] = '0';
            noteLine[1] = '0';
            noteLine[2] = '0';
            noteLine[3] = '0';
            while(stepcount > 0) {
                int stepindex = rand.nextInt(4);
                if( noteLine[stepindex] != EMPTY || (i - 1 >= 0 && AllNoteStepCount.get(i - 1).get(HOLD) > 0))
                {
                    continue;
                }
                if( holdcount > 0 ) {
                    noteLine[stepindex] = HOLD;
                    holdcount--; stepcount--;
                } else {
                    noteLine[stepindex] = STEP;
                    stepcount--;
                }
            }
            completeLine = String.valueOf(noteLine);
        } while( completeLine.equals(lastLine) && completeLine.equals("0000") == false );

        return noteLine;
    }

    private static void makeNoteLine(ArrayList<Map<Character, Integer>> AllNoteStepCount, ArrayList<char[]> AllNoteLines, float time)
    {
        // Here it will work in context windows
        // To start with something, it will search for patterns of 3 and 4 combos
        // Meaning steps without empty line inbetween
        int noteSize = AllNoteStepCount.size();
        for (int i = 0; i < AllNoteStepCount.size(); i++) {
            if (i + 3 <= noteSize)
            {
                // Combo of 4 is possible, let's see if it's happening
                if (AllNoteStepCount.get(i).get(STEP) > 0
                    && AllNoteStepCount.get(i + 1).get(STEP) > 0
                    && AllNoteStepCount.get(i + 2).get(STEP) > 0
                    && AllNoteStepCount.get(i + 3).get(STEP) > 0
                    )
                {
                    AllNoteLines.add(STEP_COMBOS.get(4).get(0).get(0).toCharArray());
                    AllNoteLines.add(STEP_COMBOS.get(4).get(0).get(1).toCharArray());
                    AllNoteLines.add(STEP_COMBOS.get(4).get(0).get(2).toCharArray());
                    AllNoteLines.add(STEP_COMBOS.get(4).get(0).get(3).toCharArray());
                    i += 3;
                }
                else
                {
                    if( AutoStepper.DEBUG_STEPS ) {
                        System.out.println("Fallback to greedy algorithm for combo 4");
                    }
                    char[] noteLine = greedySingleNote(AllNoteStepCount, AllNoteLines, i);
                    AllNoteLines.add(noteLine);
                }
            }
            if (i + 2 <= noteSize)
            {
                // Combo of 3 is possible, let's see if it's happening
                if (AllNoteStepCount.get(i).get(STEP) > 0
                    && AllNoteStepCount.get(i + 1).get(STEP) > 0
                    && AllNoteStepCount.get(i + 2).get(STEP) > 0
                    )
                {
                    AllNoteLines.add(STEP_COMBOS.get(4).get(0).get(0).toCharArray());
                    AllNoteLines.add(STEP_COMBOS.get(4).get(0).get(1).toCharArray());
                    AllNoteLines.add(STEP_COMBOS.get(4).get(0).get(2).toCharArray());
                    i += 2;
                }
                else
                {
                    if( AutoStepper.DEBUG_STEPS ) {
                        System.out.println("Fallback to greedy algorithm for combo 3");
                    }
                    char[] noteLine = greedySingleNote(AllNoteStepCount, AllNoteLines, i);
                    AllNoteLines.add(noteLine);
                }
            }
            else
            {
                if( AutoStepper.DEBUG_STEPS ) {
                    System.out.println("Too short combo - greedy algorithm");
                }
                char[] noteLine = greedySingleNote(AllNoteStepCount, AllNoteLines, i);
                AllNoteLines.add(noteLine);
            }
        }

    }
    
    private static boolean isNearATime(float time, TFloatArrayList timelist, float threshold) {
        for(int i=0;i<timelist.size();i++) {
            float checktime = timelist.get(i);
            if( Math.abs(checktime - time) <= threshold ) return true;
            if( checktime > time + threshold ) return false;
        }
        return false;
    }
    
    private static float getFFT(float time, TFloatArrayList FFTAmounts, float timePerFFT) {
        int index = Math.round(time / timePerFFT);
        if( index < 0 || index >= FFTAmounts.size()) return 0f;
        return FFTAmounts.getQuick(index);
    }
    
    private static boolean sustainedFFT(float startTime, float len, float granularity, float timePerFFT, TFloatArrayList FFTMaxes, TFloatArrayList FFTAvg, float aboveAvg, float averageMultiplier) {
        int endIndex = (int)Math.floor((startTime + len) / timePerFFT);
        if( endIndex >= FFTMaxes.size() ) return false;
        int wiggleRoom = Math.round(0.1f * len / timePerFFT);
        int startIndex = (int)Math.floor(startTime / timePerFFT);
        int pastGranu = (int)Math.floor((startTime + granularity) / timePerFFT);
        boolean startThresholdReached = false;
        for(int i=startIndex;i<=endIndex;i++) {
            float amt = FFTMaxes.getQuick(i);
            float avg = FFTAvg.getQuick(i) * averageMultiplier;
            if( i <= pastGranu ) {
                startThresholdReached |= amt >= avg + aboveAvg;
            } else {
                if( startThresholdReached == false ) return false;
                if( amt < avg ) {
                    wiggleRoom--;
                    if( wiggleRoom <= 0 ) return false;
                }
            }
        }
        return true;
    }
    
    public static String GenerateNotes(int stepGranularity, int skipChance,
                                       TFloatArrayList[] manyTimes,
                                       TFloatArrayList[] fewTimes,
                                       TFloatArrayList FFTAverages, TFloatArrayList FFTMaxes, float timePerFFT,
                                       float timePerBeat, float timeOffset, float totalTime,
                                       boolean allowMines) {     
        // make a note line, with lots of checks, balances & filtering
        float[] holding = new float[4];
        holding[0] = 0f;
        holding[1] = 0f;
        holding[2] = 0f;
        holding[3] = 0f;

        float lastJumpTime = -10f;
        // Simfile format, like '0210'
        ArrayList<char[]> AllNoteLines = new ArrayList<>();
        // Number of active action type, like STEP - 1, HOLD - 1
        // So that later on it could be assigned to '0210' or '1002' etc.
        ArrayList<Map<Character, Integer>> AllNoteStepCount = new ArrayList<>();
        float lastKickTime = 0f;
        int commaSeperatorReset = 4 * stepGranularity;
        int mineCount = 0, holdRun = 0; 

        int timeIndex = 0;
        float timeGranularity = timePerBeat / stepGranularity;
        for(float t = timeOffset; t <= totalTime; t += timeGranularity) {
            boolean isHalfBeat = timeIndex % 2 == 1;
            int steps = 0, holds = 0;
            String lastLine = getLastNoteLine(AllNoteLines);
            if( t > 0f ) {
                float fftavg = getFFT(t, FFTAverages, timePerFFT);
                float fftmax = getFFT(t, FFTMaxes, timePerFFT);
                boolean sustained = sustainedFFT(t, 0.75f, timeGranularity, timePerFFT, FFTMaxes, FFTAverages, 0.25f, 0.45f);
                boolean nearKick = isNearATime(t, fewTimes[AutoStepper.KICKS], timePerBeat / stepGranularity);
                boolean nearSnare = isNearATime(t, fewTimes[AutoStepper.SNARE], timePerBeat / stepGranularity);
                boolean nearEnergy = isNearATime(t, fewTimes[AutoStepper.ENERGY], timePerBeat / stepGranularity);
                steps = sustained || nearKick || nearSnare || nearEnergy ? 1 : 0;
                if( sustained ) {
                    holds = 1 + (nearEnergy ? 1 : 0);
                } else if( fftmax < 0.5f ) {
                    holds = fftmax < 0.25f ? -2 : -1;
                }
                if( nearKick && (nearSnare || nearEnergy) && !isHalfBeat &&
                    steps > 0 && lastLine.contains(Character.toString(STEP)) == false && lastLine.contains(Character.toString(HOLD)) == false && lastLine.contains(Character.toString(STOP)) == false ) {
                     // only jump in high areas, on solid beats (not half beats)
                    steps = 2;
                }
                // wait, are we skipping new steps?
                // if we just got done from a jump, don't have a half beat
                // if we are holding something, don't do half-beat steps
                if( isHalfBeat &&
                    (skipChance > 1 && rand.nextInt(skipChance) > 0 || getHoldCount(holding) > 0) ||
                    t - lastJumpTime < timePerBeat ) {
                    steps = 0;
                    if( holds > 0 ) holds = 0;
                }                
            }
            assignAction(AllNoteStepCount, lastLine, t, steps, holds, mineCount, allowMines, holding, lastJumpTime, holdRun);
            timeIndex++;
        }

        for (Map<Character,Integer> map : AllNoteStepCount) {
            if( AutoStepper.DEBUG_STEPS ) {
                System.out.println("S" + map.get(STEP) + " H" + map.get(HOLD));
            }
        }

        for (float t = timeOffset; t <= totalTime; t += timeGranularity)
        {
            makeNoteLine(AllNoteStepCount, AllNoteLines, t);
        }


        // ok, put together AllNotes
        String AllNotes = "";
        int commaSeperator = commaSeperatorReset;
        for(int i=0;i<AllNoteLines.size();i++) {
            AllNotes += getNoteLineIndex(i, AllNoteLines) + "\n";
            commaSeperator--;
            if( commaSeperator == 0 ) {
                AllNotes += ",\n";
                commaSeperator = commaSeperatorReset;
            }
        }
        // fill out the last empties
        while( commaSeperator > 0 ) {
            AllNotes += "3333";
            commaSeperator--;
            if( commaSeperator > 0 ) AllNotes += "\n";
        }
        int _stepCount = AllNotes.length() - AllNotes.replace(Character.toString(STEP), "").length();
        int _holdCount = AllNotes.length() - AllNotes.replace(Character.toString(HOLD), "").length();
        int _mineCount = AllNotes.length() - AllNotes.replace(Character.toString(MINE), "").length();
        System.out.println("Steps: " + _stepCount + ", Holds: " + _holdCount + ", Mines: " + _mineCount);
        return AllNotes;
    }
    
}
