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
import autostepper.soundprocessing.CExperimentalSoundProcessor;
import autostepper.soundprocessing.Song;
import ddf.minim.AudioSample;

/**
 *
 * @author Phr00t
 */
public class StepGenerator {

    CExperimentalSoundProcessor soundProcessor;
    private float granularityModifier;
    private float preciseGranularityModifier;
    private ArrayList<IVibeJudge> judges;
    private ArrayList<CStepAssigner> moveAssigners;

    public StepGenerator(TFloatArrayList params)
    {
        granularityModifier = params.get(AlgorithmParameter.GRANULARITY_MODIFIER.value());
        preciseGranularityModifier = params.get(AlgorithmParameter.PRECISE_GRANULARITY_MODIFIER.value());

        judges = new ArrayList<>();

        judges.add(new ParametrizedJudge(params.get(AlgorithmParameter.FIRST_VOLUME_THRESHOLD.value()),
                                         params.get(AlgorithmParameter.SECOND_VOLUME_THRESHOLD.value()),
                                        params.get(AlgorithmParameter.FFT_MAX_THRESHOLD.value())));
                                        
        // Just like with vibe coding :)
        // Idea is to assign vibes if current note id drop-like, standard, or it's sustained which is a candidate for hold
        // For now only checking if something should happen, not exactly on which arrow
        // So that later on it could be assigned to exact arrow, so '0210' or '1002' etc.

        // judges.add(new PopJudge());
        // judges.add(new ExcitedByEverythingJudge());
        // judges.add(new DeafToBeatJudge());

        moveAssigners = new ArrayList<>();
        // moveAssigners.add(new PopStepAssigner());
        // moveAssigners.add(new LazyPopStepAssigner());
        // moveAssigners.add(new MoreTapsAssigner());
        int jumpThreshold = Math.round(params.get(AlgorithmParameter.JUMP_THRESHOLD.value()));
        int tapThreshold = Math.round(params.get(AlgorithmParameter.TAP_THRESHOLD.value()));
        int sustainThreshold = Math.round(params.get(AlgorithmParameter.SUSTAIN_THESHOLD.value()));
        moveAssigners.add(new ParametrizedAssigner(jumpThreshold, tapThreshold, sustainThreshold));

        soundProcessor = new CExperimentalSoundProcessor(params);
    }

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
    
    public ArrayList<ArrayList<Character>> GenerateNotes(Song song, SimfileDifficulty difficulty, int stepGranularity,
                                       boolean allowMines, TFloatArrayList params, float expectedBpm)
    {
        long wholeFunctionTimer = System.currentTimeMillis();
        long timer = wholeFunctionTimer;
        float songTime = song.getSongTime();
        if (AutoStepper.DEBUG_TIMINGS)
        {
            System.out.println("Initial song analysis: " + (System.currentTimeMillis() - timer) / 1000f + "s");
        }

        // Frequencies matching snare, kicks etc. in really small time windows
        // Here even vocal could match those frequency triggering event to be set
        timer = System.currentTimeMillis();
        TFloatArrayList[] percussionEventsInTime = soundProcessor.ProcessMusic(song, params);
        if (AutoStepper.DEBUG_TIMINGS)
        {
            System.out.println("Initial percussion event parsing: " + (System.currentTimeMillis() - timer) / 1000f + "s");
        }

        if (expectedBpm != -1f)
        {
            if (Math.abs(soundProcessor.GetBpm() - expectedBpm) > 0.6f)
            {
                // not as expected
                return new ArrayList<>();
            }
        }

        TFloatArrayList FFTMaxes = soundProcessor.GetMidFFTMaxes();
        TFloatArrayList FFTAverages = soundProcessor.GetMidFFTAmount();
        TFloatArrayList volume = song.getVolume();
        float timePerBeat = soundProcessor.GetTimePerBeat();
        float startTime = soundProcessor.GetStartTime();

        // range which was doing the trick is usually between 0.2f and 0.3f
        float sustainFactor = 0.35f;
        CMusicEventsDetector eventsDetector = new CMusicEventsDetector();
        
        // To gather info about kicks, snares etc.
        // It parses whole song, so that next steps can access context
        // This time using some logic, it can split vocal matching snare freq from real snare
        timer = System.currentTimeMillis();
        ArrayList<Map<SoundParameter, Object>> NoteEvents = eventsDetector.GetEvents(stepGranularity, timePerBeat, startTime, songTime, FFTAverages, FFTMaxes, volume, song.getTimePerSample(), percussionEventsInTime, sustainFactor, granularityModifier, preciseGranularityModifier);
        if (AutoStepper.DEBUG_TIMINGS)
        {
            System.out.println("Precise music events parsing: " + (System.currentTimeMillis() - timer) / 1000f + "s");
        }


        if (AutoStepper.PREVIEW_DETECTION)
        {
            preview(song.getFilename(), NoteEvents, stepGranularity, timePerBeat, FFTAverages, FFTMaxes, volume, songTime);
        }

        timer = System.currentTimeMillis();
        ArrayList<Map<VibeScore, Integer>> NoteVibes = judges.get(0).GetVibes(NoteEvents);
        if (AutoStepper.DEBUG_TIMINGS)
        {
            System.out.println("Vibe assign: " + (System.currentTimeMillis() - timer) / 1000f + "s");
        }

        timer = System.currentTimeMillis();
        ArrayList<ArrayList<Character>> moveSet = moveAssigners.get(0).AssignMoves(NoteVibes, difficulty, songTime);
        if (AutoStepper.DEBUG_TIMINGS)
        {
            System.out.println("Assigning moves: " + (System.currentTimeMillis() - timer) / 1000f + "s");
            System.out.println("Whole generation took: " + (System.currentTimeMillis() - wholeFunctionTimer) / 1000f + "s");
        }

        return moveSet;
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

