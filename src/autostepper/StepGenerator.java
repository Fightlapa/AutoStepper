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

import autostepper.moveassigners.SimfileDifficulty;
import autostepper.musiceventsdetector.CMusicEventsDetector;
import autostepper.moveassigners.CStepAssigner;
import autostepper.moveassigners.PopStepAssigner;
import autostepper.vibejudges.IVibeJudge;
import autostepper.vibejudges.PopJudge;
import autostepper.vibejudges.SoundParameter;
import autostepper.vibejudges.VibeScore;
import autostepper.musiceventsdetector.StandardEventsDetector;
import ddf.minim.AudioSample;

/**
 *
 * @author Phr00t
 */
public class StepGenerator {

    public void preview(String filename, ArrayList<Map<SoundParameter, Object>> detected, long stepGranularity, float timePerBeat)
    {
        AudioSample fullSong = AutoStepper.minim.loadSample(filename);
 
        // get the most accurate start time as possible
        long millis = System.currentTimeMillis();
        // Just an assumption how long it can take to start music
        millis += 3;
        fullSong.trigger();

        try
        {
            long timeGranularity = (long)(1000f * timePerBeat) / stepGranularity;
            int idx = 0;
            for (Map<SoundParameter,Object> map : detected)
            {
                StringBuilder sb = new StringBuilder();
                sb.append((boolean)map.get(SoundParameter.KICKS) ? "K" : " ");
                sb.append((boolean)map.get(SoundParameter.SNARE) ? "S" : " ");
                sb.append((boolean)map.get(SoundParameter.HAT) ? "H" : " ");
                if ((boolean)map.get(SoundParameter.HALF_BEAT))
                {
                    sb.append("b");
                }
                else if ((boolean)map.get(SoundParameter.BEAT))
                {
                    sb.append("B");
                }
                else
                {
                    sb.append(" ");
                }
                sb.append((boolean)map.get(SoundParameter.SUSTAINED) ? "~" : " ");
                sb.append((boolean)map.get(SoundParameter.SILENCE) ? "." : " ");
                sb.append((boolean)map.get(SoundParameter.NOTHING) ? "N" : " ");
                sb.append("  FFTMAX: " + map.get(SoundParameter.FFT_MAX));
                sb.append("  FFTAVG: " + map.get(SoundParameter.FFT_AVG));
                sb.append("  " + idx + " / " + detected.size());
                
                while (System.currentTimeMillis() - millis < timeGranularity)
                {
                    Thread.sleep(timeGranularity/5);
                    System.out.println(sb.toString());
                }
                idx++;
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
    
    public String GenerateNotes(String filename, SimfileDifficulty difficulty, int stepGranularity,
                                       TFloatArrayList[] fewTimes,
                                       TFloatArrayList FFTAverages, TFloatArrayList FFTMaxes, float timePerFFT,
                                       float timePerBeat, float timeOffset, float totalTime,
                                       boolean allowMines) {     
        // To gather infor about kicks, snares etc
        // It parses whole song, so that next steps can access context

        CMusicEventsDetector eventsDetector = new StandardEventsDetector();
        ArrayList<Map<SoundParameter, Object>> NoteEvents = eventsDetector.GetEvents(stepGranularity, timePerBeat, timeOffset, totalTime, FFTAverages, FFTMaxes, timePerFFT, fewTimes);
        if (AutoStepper.PREVIEW_DETECTION)
        {
            preview(filename, NoteEvents, stepGranularity, timePerBeat);
        }

        // Here we'll try different algorithms, we have some targets, like:
        // Map to indicate for how many arrows are we aiming for whole song
        // Beginner -> 55 / minute
        // Easy -> 80 / minute
        // Medium -> 105 / minute
        // Beginner -> 170 / minute
        // Beginner -> 190 / minute
        int targetSteps = 0;
        float songLengthMinutes = totalTime / 60f;
        switch (difficulty) {
            case BEGINNER: targetSteps = (int) (55 * songLengthMinutes); break;
            case EASY: targetSteps = (int) (80 * songLengthMinutes); break;
            case MEDIUM: targetSteps = (int) (105 * songLengthMinutes); break;
            case HARD: targetSteps = (int) (170 * songLengthMinutes); break;
            case CHALLENGE: targetSteps = (int) (190 * songLengthMinutes); break;
        };

        // We'll try different algorithms, as for slower songs it's hard to get many arrows so we should decide to put arrow easier for those

        // Just like with vibe coding :)
        // Idea is to assign vibes if current note id drop-like, standard, or it's sustained which is a candidate for hold
        // For now only checking if something should happen, not exactly on which arrow
        // So that later on it could be assigned to exact arrow, so '0210' or '1002' etc.
        IVibeJudge vibeJudge = new PopJudge();
        ArrayList<Map<VibeScore, Integer>> NoteVibes = vibeJudge.GetVibes(NoteEvents);

        // Simfile format, like '0210'
        CStepAssigner stepAssigner = new PopStepAssigner();
        ArrayList<ArrayList<Character>> AllarrowLines = stepAssigner.AssignMoves(NoteVibes, difficulty, totalTime);

        // ok, put together AllNotes
        int commaSeperatorReset = 4 * stepGranularity;
        String AllNotes = "";
        int commaSeperator = commaSeperatorReset;
        for (ArrayList<Character> arrayList : AllarrowLines) {
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
        int _stepCount = AllNotes.length() - AllNotes.replace(Character.toString(CStepAssigner.STEP), "").length();
        int _holdCount = AllNotes.length() - AllNotes.replace(Character.toString(CStepAssigner.HOLD), "").length();
        int _mineCount = AllNotes.length() - AllNotes.replace(Character.toString(CStepAssigner.MINE), "").length();
        System.out.println("New algorithm. Steps: " + _stepCount + ", Holds: " + _holdCount + ", Expected steps: " + targetSteps);

        return AllNotes;
    }
    
}
