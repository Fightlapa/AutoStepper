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
import autostepper.genetic.AlgorithmParameter;
import autostepper.misc.Utils;
import autostepper.moveassigners.CStepAssigner;
import autostepper.moveassigners.ParametrizedAssigner;
import autostepper.vibejudges.ExcitedByEverythingJudge;
import autostepper.vibejudges.DeafToBeatJudge;
import autostepper.vibejudges.IVibeJudge;
import autostepper.vibejudges.ParametrizedJudge;
import autostepper.vibejudges.PopJudge;
import autostepper.vibejudges.SoundParameter;
import autostepper.vibejudges.VibeScore;
import autostepper.musiceventsdetector.StandardEventsDetector;
import autostepper.soundprocessing.CExperimentalSoundProcessor;
import autostepper.soundprocessing.ISoundProcessor;
import autostepper.musiceventsdetector.DiffSensitiveEventsDetector;
import autostepper.musiceventsdetector.PreciseDiffSensitiveEventsDetector;
import ddf.minim.AudioSample;

/**
 *
 * @author Phr00t
 */
public class StepGenerator {

    ISoundProcessor soundProcessor = new CExperimentalSoundProcessor();

    static String redStep(int step) {
        // step: 0–9
        int[] colors = {
            231, // white
            224, // very light pink
            217,
            210,
            203,
            196, // strong red
            160,
            124,
            88,
            52   // dark red
        };
        return "\u001B[38;5;" + colors[step] + "m";
    }

    public void preview(String filename, ArrayList<Map<SoundParameter, Object>> detected, long stepGranularity, float timePerBeat, TFloatArrayList FFTAverages, TFloatArrayList FFTMaxes, TFloatArrayList volume, float totalTime)
    {
        AudioSample fullSong = AutoStepper.minimLib.loadSample(filename);
 
        // get the most accurate start time as possible
        long millis = System.currentTimeMillis();
        long jazzMusicStarts = System.currentTimeMillis();
        // Just an assumption how long it can take to start music
        millis += 1;
        fullSong.trigger();
        int fftLength = FFTMaxes.size();

        try
        {
            String RESET = "\u001B[0m";
            long timeGranularity = (long)(1000f * timePerBeat) / stepGranularity;
            int idx = 0;
            for (Map<SoundParameter,Object> map : detected)
            {            
                StringBuilder sb = new StringBuilder();
                sb.append((boolean)map.get(SoundParameter.KICKS) ? "K" : " ");
                sb.append((boolean)map.get(SoundParameter.SNARE) ? "S" : " ");
                // sb.append((boolean)map.get(SoundParameter.HAT) ? "H" : " ");
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
                sb.append("  " + idx + " / " + detected.size());
                sb.append('\t');
                String base = sb.toString();
                while (System.currentTimeMillis() - millis < timeGranularity)
                {
                    StringBuilder sbFull = new StringBuilder();
                    sbFull.append(base);
                    int fftIndex = (int) (((System.currentTimeMillis() - jazzMusicStarts) * fftLength) / (totalTime * 1000));
                    if (fftIndex < 0)
                    {
                        fftIndex = 0;
                    }
                    if (fftIndex >= FFTMaxes.size())
                    {
                        System.out.println("ERROR!!!!");
                        fftIndex = FFTMaxes.size() - 1;
                    }
                    float fftMax = FFTMaxes.get(fftIndex);
                    int fftMaxLevel = (int)(fftMax*7);
                    sbFull.append(redStep(fftMaxLevel));
                    sbFull.append("  FFTMAX: " + "=".repeat(fftMaxLevel));
                    
                    sbFull.append('\t');
                    float fftAvg = FFTAverages.get(fftIndex);
                    int fftAvgLevel = (int)(fftAvg*7);
                    sbFull.append(redStep(fftAvgLevel));
                    sbFull.append("  FFTAVG: " + "=".repeat(fftAvgLevel));

                    sbFull.append('\t');
                    float volumeNow = volume.get(fftIndex);
                    int volumeLevel = (int)(volumeNow*7);
                    sbFull.append(redStep(volumeLevel));
                    sbFull.append("  VOL: " + "=".repeat(volumeLevel));

                    sbFull.append(RESET);
                    System.out.println(sbFull.toString());
                    Thread.sleep(timeGranularity/10);
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
    
    public ArrayList<ArrayList<Character>> GenerateNotes(String filename, SimfileDifficulty difficulty, int stepGranularity,
                                       boolean allowMines, TFloatArrayList params)
        {
        // collected song data
        final TFloatArrayList[] manyTimes = new TFloatArrayList[4];
        final TFloatArrayList[] fewTimes = new TFloatArrayList[4];

        float songTime = Utils.getSongTime(filename);
        soundProcessor.ProcessMusic(AutoStepper.minimLib, filename, songTime, manyTimes, fewTimes, params);

        TFloatArrayList FFTMaxes = soundProcessor.GetMidFFTMaxes();
        TFloatArrayList FFTAverages = soundProcessor.GetMidFFTAmount();
        TFloatArrayList volume = soundProcessor.getVolume();
        float timePerSample = soundProcessor.timePerSample();
        float timePerBeat = soundProcessor.GetTimePerBeat();
        float startTime = soundProcessor.GetStartTime();

        float granularityModifier = params.get(AlgorithmParameter.GRANULARITY_MODIFIER.value());
        float preciseGranularityModifier = params.get(AlgorithmParameter.PRECISE_GRANULARITY_MODIFIER.value());

        // To gather infor about kicks, snares etc
        // It parses whole song, so that next steps can access context
        ArrayList<Map<SoundParameter, Object>> NoteEvents = null;
        // 0 - jumpThreshold
        // 1 - tapThreshold
        // 2 - sustainThreshold
        for (float sustainFactor = 0.2f; sustainFactor < 0.3f; sustainFactor += 0.02f) {
            // CMusicEventsDetector eventsDetector = new StandardEventsDetector();
            CMusicEventsDetector eventsDetector = new DiffSensitiveEventsDetector();
            // CMusicEventsDetector eventsDetector = new PreciseDiffSensitiveEventsDetector();
            
            NoteEvents = eventsDetector.GetEvents(stepGranularity, timePerBeat, startTime, songTime, FFTAverages, FFTMaxes, volume, timePerSample, fewTimes, sustainFactor, granularityModifier, preciseGranularityModifier);

            long sustains = NoteEvents.stream().filter(m -> Boolean.TRUE.equals(m.get(SoundParameter.SUSTAINED))).count();
            long kicks = NoteEvents.stream().filter(m -> Boolean.TRUE.equals(m.get(SoundParameter.KICKS))).count();
            long snares = NoteEvents.stream().filter(m -> Boolean.TRUE.equals(m.get(SoundParameter.SNARE))).count();
            long beat = NoteEvents.stream().filter(m -> Boolean.TRUE.equals(m.get(SoundParameter.BEAT))).count();
            // long hat = NoteEvents.stream().filter(m -> Boolean.TRUE.equals(m.get(SoundParameter.HAT))).count();
            long silence = NoteEvents.stream().filter(m -> Boolean.TRUE.equals(m.get(SoundParameter.SILENCE))).count();
            // System.out.println("SustainFactor: " + sustainFactor);
            System.out.println("Sustains: " + sustains + ", Kicks: " + kicks + ", Snares: " + snares + ", Beat: " + beat + ", Silence: " + silence);

            // Random value for now 
            if (sustains < beat / 3)
            {
                if (AutoStepper.PREVIEW_DETECTION)
                {
                    preview(filename, NoteEvents, stepGranularity, timePerBeat, FFTAverages, FFTMaxes, volume, songTime);
                }
                break;
            }
        }

        // Here we'll try different algorithms, we have some targets, like:
        // Map to indicate for how many arrows are we aiming for whole song
        // Beginner -> 55 / minute
        // Easy -> 80 / minute
        // Medium -> 105 / minute
        // Beginner -> 170 / minute
        // Beginner -> 190 / minute
        int targetSteps = 0;
        float songLengthMinutes = songTime / 60f;
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
        ArrayList<IVibeJudge> judges = new ArrayList<>();

        judges.add(new ParametrizedJudge(params.get(AlgorithmParameter.FIRST_VOLUME_THRESHOLD.value()),
                                         params.get(AlgorithmParameter.SECOND_VOLUME_THRESHOLD.value()),
                                        params.get(AlgorithmParameter.FFT_MAX_THRESHOLD.value())));
        // judges.add(new PopJudge());
        // judges.add(new ExcitedByEverythingJudge());
        // judges.add(new DeafToBeatJudge());

        ArrayList<CStepAssigner> moveAssigners = new ArrayList<>();
        // moveAssigners.add(new PopStepAssigner());
        // moveAssigners.add(new LazyPopStepAssigner());
        // moveAssigners.add(new MoreTapsAssigner());
        int jumpThreshold = Math.round(params.get(AlgorithmParameter.JUMP_THRESHOLD.value()));
        int tapThreshold = Math.round(params.get(AlgorithmParameter.TAP_THRESHOLD.value()));
        int sustainThreshold = Math.round(params.get(AlgorithmParameter.SUSTAIN_THESHOLD.value()));
        moveAssigners.add(new ParametrizedAssigner(jumpThreshold, tapThreshold, sustainThreshold));
        // moveAssigners.add(new ParametrizedAssigner(3, 1, 1));
        // moveAssigners.add(new ParametrizedAssigner(3, 2, 1));
        // moveAssigners.add(new ParametrizedAssigner(3, 2, 1));


        long currentMargin = 99999;

        ArrayList<ArrayList<Character>> AllarrowLines = null;
        // Yeah, that looks great
        Map<IVibeJudge, Map<CStepAssigner, ArrayList<ArrayList<Character>>>> results = new HashMap<>();
        for (IVibeJudge judge : judges)
        {
            Map<CStepAssigner, ArrayList<ArrayList<Character>>> resultsFromJudge = new HashMap<>();
            ArrayList<Map<VibeScore, Integer>> NoteVibes = judge.GetVibes(NoteEvents);

            for (CStepAssigner moveAssigner : moveAssigners)
            {
                // Moves have Simfile format, like '0210' -> empty, hold start, step, empty
                ArrayList<ArrayList<Character>> currentMoveSet = moveAssigner.AssignMoves(NoteVibes, difficulty, songTime);
                resultsFromJudge.put(moveAssigner, currentMoveSet);
                long steps = 0;
                for (ArrayList<Character> moveLine : currentMoveSet) {
                    steps += moveLine.stream().filter(m -> m == CStepAssigner.STEP).count();
                }
                long stepsDiff = Math.abs(steps - targetSteps);
                if (stepsDiff < currentMargin)
                {
                    // System.out.println("New candidate: " + judge.WhatsYourNameMrJudge() + " With Assigner: " + moveAssigner.AssignerName() + " Target steps: " + targetSteps + " result steps:" + steps);
                    AllarrowLines = currentMoveSet;
                    currentMargin = stepsDiff;
                }
            }
            results.put(judge, resultsFromJudge);
        }

        return AllarrowLines;
    }

    public float getBPM()
    {
        return soundProcessor.GetBpm();
    }

    public float getStartTime()
    {
        return soundProcessor.GetStartTime();
    }
    
}

