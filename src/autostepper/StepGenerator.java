package autostepper;

import gnu.trove.list.array.TFloatArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

import autostepper.SoundEvent;
import ddf.minim.AudioSample;

/**
 *
 * @author Phr00t
 */
public class StepGenerator {
    
    static private int MAX_HOLD_BEAT_COUNT = 4;
    static private int NO_HOLD = -1;
    static private char EMPTY = '0', STEP = '1', HOLD = '2', STOP = '3', MINE = 'M', KEEP_HOLDING = 'H';

    static private int ACTION_HOLD = 0;
    static private int TAP = 1;
    static private int JUMP = 2;

    static private int lastHoldIndex = 0;
    static private int lastStepIndex = 0;
    static private int lastSkipRoll = 0;

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
  
    private static ArrayList<ArrayList<Character>> AssignMoves(ArrayList<Map<VibeScore, Integer>> NoteVibes)
    {
        ArrayList<ArrayList<Character>> NoteMoves = new ArrayList<>();
        // Here it will work in context windows
        // To start with something, it will search for patterns of 3 and 4 combos
        // Meaning steps without empty line inbetween
        int numberOfRows = NoteVibes.size();
        for (int i = 0; i < numberOfRows; i++)
        {
            List<Integer> action = new ArrayList<>();
            if ((i + 2 <= numberOfRows && NoteVibes.get(i).get(VibeScore.POWER) == 2)
                && (NoteVibes.get(i).get(VibeScore.POWER) == 0))
            {
                if ((i + 1 <= numberOfRows && (NoteVibes.get(i).get(VibeScore.POWER) == 0)))
                {
                    action.add(JUMP);
                }
                else
                {
                    action.add(TAP);
                }
            }
            else if (NoteVibes.get(i).get(VibeScore.POWER) == 1)
            {
                action.add(TAP);
            }
            if (NoteVibes.get(i).get(VibeScore.SUSTAIN) > 0)
            {
                action.add(ACTION_HOLD);
            }

            ArrayList<Character> noteLine = new ArrayList<>(List.of('0', '0', '0', '0'));

            if (i - 1 >= 0)
            {
                int heldIndex = NoteMoves.get(i - 1).indexOf(HOLD);
                int keepHeldIndex = NoteMoves.get(i - 1).indexOf(KEEP_HOLDING);
                if (heldIndex != -1)
                {
                    if (action.contains(ACTION_HOLD))
                    {
                        noteLine.set(heldIndex, KEEP_HOLDING);
                    }
                    else
                    {
                        noteLine.set(heldIndex, STOP);
                    }
                }
                else if (keepHeldIndex != -1)
                {
                    if (action.contains(ACTION_HOLD))
                    {
                        noteLine.set(keepHeldIndex, KEEP_HOLDING);
                    }
                    else
                    {
                        noteLine.set(heldIndex, STOP);
                    }
                }
            }

            int arrows = 0;
            if (action.contains(JUMP))
            {
                arrows = 2;
            }
            else if (action.contains(TAP))
            {
                arrows = 1;
            }

            while(arrows > 0)
            {
                int stepindex;
                if( AutoStepper.RANDOMIZED )
                {
                    stepindex = rand.nextInt(4);
                }
                else
                {
                    stepindex = lastStepIndex++ % 4;
                }
                if( noteLine.get(stepindex) != EMPTY || (i - 1 >= 0 && (NoteMoves.get(i - 1).get(stepindex) == HOLD || NoteMoves.get(i - 1).get(stepindex) == KEEP_HOLDING)))
                {
                    // Already holds or just busy
                    continue;
                }
                else
                {
                    noteLine.set(stepindex, STEP);
                }
                
                arrows--;
            }
            NoteMoves.add(noteLine);
        }
        return NoteMoves;
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

    public static ArrayList<Map<SoundEvent, Boolean>> GetEvents(int stepGranularity, float timePerBeat, float timeOffset, float totalTime, TFloatArrayList FFTAverages, TFloatArrayList FFTMaxes, float timePerFFT, TFloatArrayList[] fewTimes)
    {
        ArrayList<Map<SoundEvent, Boolean>> NoteEvents = new ArrayList<>();

        int timeIndex = 0;
        float timeGranularity = timePerBeat / stepGranularity;
        for(float t = timeOffset; t <= totalTime; t += timeGranularity) {
            boolean isHalfBeat = timeIndex % 2 == 1;
            boolean nearKick = false;
            boolean nearSnare = false;
            boolean nearEnergy = false;
            if( t > 0f ) {
                float fftmax = getFFT(t, FFTMaxes, timePerFFT);
                boolean sustained = sustainedFFT(t, 0.75f, timeGranularity, timePerFFT, FFTMaxes, FFTAverages, 0.25f, 0.45f);
                nearKick = isNearATime(t, fewTimes[SoundEvent.KICKS.value()], timePerBeat / stepGranularity);
                nearSnare = isNearATime(t, fewTimes[SoundEvent.SNARE.value()], timePerBeat / stepGranularity);
                nearEnergy = isNearATime(t, fewTimes[SoundEvent.BEAT.value()], timePerBeat / stepGranularity);
                Map<SoundEvent, Boolean> events = new HashMap<>();
                events.put(SoundEvent.KICKS, nearKick);
                events.put(SoundEvent.SNARE, nearSnare);
                events.put(SoundEvent.BEAT, nearEnergy);
                events.put(SoundEvent.SUSTAINED, sustained);
                events.put(SoundEvent.SILENCE, fftmax < 0.5f);
                events.put(SoundEvent.HALF_BEAT, isHalfBeat);
                events.put(SoundEvent.NOTHING, false);
                NoteEvents.add(events);
            }
            else
            {
                Map<SoundEvent, Boolean> events = new HashMap<>();
                events.put(SoundEvent.KICKS, false);
                events.put(SoundEvent.SNARE, false);
                events.put(SoundEvent.BEAT, false);
                events.put(SoundEvent.SUSTAINED, false);
                events.put(SoundEvent.SILENCE, true);
                events.put(SoundEvent.HALF_BEAT, false);
                events.put(SoundEvent.NOTHING, true);
                NoteEvents.add(events);
            }
            timeIndex++;
        }
        return NoteEvents;
    }

    public static ArrayList<Map<VibeScore, Integer>> GetVibes(ArrayList<Map<SoundEvent, Boolean>> NoteEvents)
    {
        ArrayList<Map<VibeScore, Integer>> NoteVibes = new ArrayList<>();

        for (Map<SoundEvent,Boolean> map : NoteEvents) {
            int vibePower = 0;
            int sustainPower = 0;
            if (!map.get(SoundEvent.NOTHING))
            {

                if (map.get(SoundEvent.SNARE) || map.get(SoundEvent.SUSTAINED))
                {
                    // Some standard sound or something to sustain deserves at least some vibe
                    vibePower++;
                }

                if (map.get(SoundEvent.BEAT) && map.get(SoundEvent.KICKS) && !map.get(SoundEvent.HALF_BEAT))
                {
                    // If it's kicks, on beat, without half-beat, that sounds like a good candidate for jump, that sounds like good vibe
                    vibePower++;
                }

                if ( map.get(SoundEvent.SUSTAINED) ) {
                    sustainPower++;
                    if ( map.get(SoundEvent.BEAT) )
                    {
                        // Extra boost if that's the beat
                        sustainPower++;
                    }
                } else if( map.get(SoundEvent.SILENCE) ) {
                    sustainPower--;
                }
            }
            Map<VibeScore, Integer> noteVibe = new HashMap<>();
            noteVibe.put(VibeScore.POWER, vibePower);
            noteVibe.put(VibeScore.SUSTAIN, sustainPower);
            NoteVibes.add(noteVibe);
        }
        return NoteVibes;
    }

    public static void preview(String filename, ArrayList<Map<SoundEvent, Boolean>> detected, long stepGranularity, float timePerBeat)
    {
        AudioSample fullSong = AutoStepper.minim.loadSample(filename);
 
        // get the most accurate start time as possible
        long millis = System.currentTimeMillis();
        fullSong.trigger();

        try
        {
            long timeGranularity = (long)(1000f * timePerBeat) / stepGranularity;
            for (Map<SoundEvent,Boolean> map : detected)
            {
                StringBuilder sb = new StringBuilder();
                sb.append(map.get(SoundEvent.KICKS) ? "K" : " ");
                sb.append(map.get(SoundEvent.SNARE) ? "S" : " ");
                sb.append(map.get(SoundEvent.BEAT) ? "B" : " ");
                sb.append(map.get(SoundEvent.SUSTAINED) ? "~" : " ");
                sb.append(map.get(SoundEvent.SILENCE) ? "." : " ");
                sb.append(map.get(SoundEvent.NOTHING) ? "N" : " ");
                while (System.currentTimeMillis() - millis < timeGranularity)
                {
                    Thread.sleep(3); // 100 ms
                    System.out.println(sb.toString());
                }
                millis += timeGranularity;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            fullSong.stop();
            fullSong.close();
        }
    }
    
    public static String GenerateNotes(String filename, int stepGranularity, int skipChance,
                                       TFloatArrayList[] manyTimes,
                                       TFloatArrayList[] fewTimes,
                                       TFloatArrayList FFTAverages, TFloatArrayList FFTMaxes, float timePerFFT,
                                       float timePerBeat, float timeOffset, float totalTime,
                                       boolean allowMines) {     
        // To gather infor about kicks, snares etc
        // It parses whole song, so that next steps can access context
        ArrayList<Map<SoundEvent, Boolean>> NoteEvents = GetEvents(stepGranularity, timePerBeat, timeOffset, totalTime, FFTAverages, FFTMaxes, timePerFFT, fewTimes);

        // Just like with vibe coding :)
        // Idea is to assign vibes if current note id drop-like, standard, or it's sustained which is a candidate for hold
        // For now only checking if something should happen, not exactly on which arrow
        // So that later on it could be assigned to exact arrow, so '0210' or '1002' etc.
        ArrayList<Map<VibeScore, Integer>> NoteVibes = GetVibes(NoteEvents);

        // Simfile format, like '0210'
        ArrayList<ArrayList<Character>> AllNoteLines = AssignMoves(NoteVibes);

        // ok, put together AllNotes
        int commaSeperatorReset = 4 * stepGranularity;
        String AllNotes = "";
        int commaSeperator = commaSeperatorReset;
        for (ArrayList<Character> arrayList : AllNoteLines) {
            String result = arrayList.stream()
                .map(c -> c == 'W' ? '0' : c) // to replace custom "HOLDING" to empty
                .map(String::valueOf)
                .collect(Collectors.joining());
            AllNotes += result + "\n";
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

        if (AutoStepper.PREVIEW_DETECTION)
        {
            preview(filename, NoteEvents, stepGranularity, timePerBeat);
        }
        
        return AllNotes;
    }
    
}
