package autostepper.soundprocessing;

import java.io.File;

import autostepper.AutoStepper;
import autostepper.genetic.AlgorithmParameter;
import autostepper.misc.Averages;
import autostepper.misc.Utils;
import autostepper.vibejudges.SoundParameter;
import ddf.minim.AudioSample;
import ddf.minim.Minim;
import ddf.minim.MultiChannelBuffer;
import ddf.minim.analysis.BeatDetect;
import ddf.minim.analysis.FFT;
import ddf.minim.spi.AudioRecordingStream;
import gnu.trove.list.array.TFloatArrayList;

public class CExperimentalSoundProcessor
{
    public static float SAMPLE_REDUCTION_RATIO = 1000f;
    public static float MIN_BPM = 70f;

    private float bpm;
    private float startTime = 0f;
    private float timePerBeat = 0f;
    private int lowerFrequencyKickThreshold;
    private int higherFrequencyKickThreshold;
    private int kickBandsThreshold;
    private int lowerFrequencySnareThreshold;
    private int higherFrequencySnareThreshold;
    private int snareBandsThreshold;
    private int lowerFrequencyHatThreshold;
    private int higherFrequencyHatThreshold;
    private int hatBandsThreshold;

    public CExperimentalSoundProcessor(TFloatArrayList params) {
        lowerFrequencyKickThreshold = Math.round(params.get(AlgorithmParameter.KICK_LOW_FREQ.value()));
        higherFrequencyKickThreshold = Math.round(params.get(AlgorithmParameter.KICK_HIGH_FREQ.value()));
        kickBandsThreshold = Math.round(params.get(AlgorithmParameter.KICK_BAND_FREQ.value()));

        lowerFrequencySnareThreshold = Math.round(params.get(AlgorithmParameter.SNARE_LOW_FREQ.value()));
        higherFrequencySnareThreshold = Math.round(params.get(AlgorithmParameter.SNARE_HIGH_FREQ.value()));
        snareBandsThreshold = Math.round(params.get(AlgorithmParameter.SNARE_BAND_FREQ.value()));

        lowerFrequencyHatThreshold = Math.round(params.get(AlgorithmParameter.HAT_LOW_FREQ.value()));
        higherFrequencyHatThreshold = Math.round(params.get(AlgorithmParameter.HAT_HIGH_FREQ.value()));
        hatBandsThreshold = Math.round(params.get(AlgorithmParameter.HAT_BAND_FREQ.value()));
    }

    public TFloatArrayList[] ProcessMusic(Song song, TFloatArrayList params)
    {
        // collected song data
        final TFloatArrayList[] manyTimes = new TFloatArrayList[4];
        final TFloatArrayList[] fewTimes = new TFloatArrayList[4];

        float timePerSample = song.getTimePerSample();

        FrequencyConfig kicksConfig = new FrequencyConfig(
            Math.round(params.get(AlgorithmParameter.KICK_LOW_FREQ.value())),
            Math.round(params.get(AlgorithmParameter.KICK_HIGH_FREQ.value())),
            Math.round(params.get(AlgorithmParameter.KICK_BAND_FREQ.value())));
        FrequencyConfig snaresConfig = new FrequencyConfig(
            Math.round(params.get(AlgorithmParameter.SNARE_LOW_FREQ.value())),
            Math.round(params.get(AlgorithmParameter.SNARE_HIGH_FREQ.value())),
            Math.round(params.get(AlgorithmParameter.SNARE_BAND_FREQ.value())));
        FrequencyConfig hatsConfig = new FrequencyConfig(
            Math.round(params.get(AlgorithmParameter.HAT_LOW_FREQ.value())),
            Math.round(params.get(AlgorithmParameter.HAT_HIGH_FREQ.value())),
            Math.round(params.get(AlgorithmParameter.HAT_BAND_FREQ.value())));
        song.processPercussions(kicksConfig, snaresConfig, hatsConfig, fewTimes, manyTimes);

        // calculate differences between percussive elements,
        // then find the most common differences among all
        // use this to calculate BPM
        TFloatArrayList common = new TFloatArrayList();
        float doubleSpeed = 60f / (Song.MAX_BPM * 2f);
        for (int i = 0; i < fewTimes.length; i++) {
            AddCommonBPMs(common, fewTimes[i], doubleSpeed, timePerSample * 1.5f);
            AddCommonBPMs(common, manyTimes[i], doubleSpeed, timePerSample * 1.5f);
        }
        if (common.isEmpty()) {
            System.out.println("[--- FAILED: COULDN'T CALCULATE BPM ---]");
            throw new RuntimeException("Cannot determine BPM");
        }
        bpm = Averages.getMostCommonFightlapa(common, 0.5f, true);
        if (AutoStepper.SHOW_INFO)
        {
            System.out.println("[--- bpm: " + bpm + " ---]");
        }

        timePerBeat = 60f / bpm;
        TFloatArrayList startTimes = new TFloatArrayList();
        for (int i = 0; i < fewTimes.length; i++) {
            startTimes.add(Utils.getBestOffset(timePerBeat, fewTimes[i], 0.01f));
            startTimes.add(Utils.getBestOffset(timePerBeat, manyTimes[i], 0.01f));
        }
        // give extra weight to fewKicks
        float kickStartTime = Utils.getBestOffset(timePerBeat, fewTimes[SoundParameter.KICKS.value()], 0.01f);
        startTimes.add(kickStartTime);
        startTimes.add(kickStartTime);
        startTime = -Averages.getMostCommonPhr00t(startTimes, 0.02f, false);
        return fewTimes;
    }

    void AddCommonBPMs(TFloatArrayList common, TFloatArrayList times, float doubleSpeed, float timePerSample) {
        float commonBPM = 60f / Averages.getMostCommonFightlapa(Utils.calculateDifferences(times, doubleSpeed), timePerSample, true);
        if (commonBPM > Song.MAX_BPM) {
            common.add(commonBPM * 0.5f);
        } else if (commonBPM < MIN_BPM / 2f) {
            common.add(commonBPM * 4f);
        } else if (commonBPM < MIN_BPM) {
            common.add(commonBPM * 2f);
        } else
            common.add(commonBPM);
    }

    public float GetBpm() {
        return bpm;
    }

    public float GetTimePerBeat() {
        return timePerBeat;
    }

    public float GetStartTime() {
        return startTime;
    }
}
