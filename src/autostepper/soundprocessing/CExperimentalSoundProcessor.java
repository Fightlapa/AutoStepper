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
    private static int FFT_SIZE = 512;
    public static int BPM_SENSITIVITY_MS = 23;
    public static float MIN_BPM = 70f;
    public static float MAX_BPM = 170f;

    private float bpm;
    private TFloatArrayList MidFFTAmount;
    private TFloatArrayList MidFFTMaxes;
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
        AudioRecordingStream stream = AutoStepper.minimLib.loadFileStream(song.getFilename(), FFT_SIZE, false);

        // tell it to "play" so we can read from it.
        stream.play();

        // create the fft/beatdetect objects we'll use for analysis
        BeatDetect beatDetectFrequencyHighSensitivity = new BeatDetect(FFT_SIZE, stream.getFormat().getSampleRate());
        BeatDetect beatDetectFrequencyLowSensitivity = new BeatDetect(FFT_SIZE, stream.getFormat().getSampleRate());
        BeatDetect beatDetectSoundHighSensitivity = new BeatDetect(stream.getFormat().getSampleRate());
        BeatDetect beatDetectSoundLowSensitivity = new BeatDetect(stream.getFormat().getSampleRate());
        beatDetectFrequencyHighSensitivity.setSensitivity(BPM_SENSITIVITY_MS);
        beatDetectSoundHighSensitivity.setSensitivity(BPM_SENSITIVITY_MS);
        beatDetectFrequencyLowSensitivity.setSensitivity(Math.round(60000f / (float) MAX_BPM));
        beatDetectSoundLowSensitivity.setSensitivity(Math.round(60000f / (float) MAX_BPM));

        // create the buffer we use for reading from the stream
        MultiChannelBuffer buffer = new MultiChannelBuffer(FFT_SIZE, stream.getFormat().getChannels());

        // figure out how many samples are in the stream so we can allocate the correct
        // number of spectra
        int totalSamples = (int) (song.getSongTime() * stream.getFormat().getSampleRate());

        // now we'll analyze the samples in chunks
        int totalChunks = (totalSamples / FFT_SIZE) + 1;

        for (int i = 0; i < fewTimes.length; i++) {
            if (fewTimes[i] == null)
                fewTimes[i] = new TFloatArrayList();
            if (manyTimes[i] == null)
                manyTimes[i] = new TFloatArrayList();
            fewTimes[i].clear();
            manyTimes[i].clear();
        }
        MidFFTAmount = new TFloatArrayList();
        MidFFTMaxes = new TFloatArrayList();

        float largestAvg = 0f, largestMax = 0f;
        FFT fft = new FFT(FFT_SIZE, stream.getFormat().getSampleRate());
        int lowFreq = fft.freqToIndex(300f);
        int highFreq = fft.freqToIndex(3000f);

        float timePerSample = song.getTimePerSample();
        float time = 0;
        for (int chunkIdx = 0; chunkIdx < totalChunks; ++chunkIdx)
        {
            stream.read(buffer);
            float[] data = buffer.getChannel(0);
            // now analyze the left channel
            beatDetectFrequencyHighSensitivity.detect(data);
            beatDetectSoundHighSensitivity.detect(data);
            beatDetectFrequencyLowSensitivity.detect(data);
            beatDetectSoundLowSensitivity.detect(data);
            fft.forward(data);
            // fft processing
            float avg = fft.calcAvg(300f, 3000f);
            float max = 0f;
            for (int b = lowFreq; b <= highFreq; b++) {
                float bandamp = fft.getBand(b);
                if (bandamp > max)
                    max = bandamp;
            }
            if (max > largestMax)
                largestMax = max;
            if (avg > largestAvg)
                largestAvg = avg;
            MidFFTAmount.add(avg);
            MidFFTMaxes.add(max);
            // store basic percussion times
            if (beatDetectFrequencyHighSensitivity.isRange(lowerFrequencyKickThreshold, higherFrequencyKickThreshold, kickBandsThreshold))
                manyTimes[SoundParameter.KICKS.value()].add(time);
            if (beatDetectFrequencyHighSensitivity.isRange(lowerFrequencyHatThreshold, higherFrequencyHatThreshold, hatBandsThreshold))
                manyTimes[SoundParameter.HAT.value()].add(time);
            if (beatDetectFrequencyHighSensitivity.isRange(lowerFrequencySnareThreshold, higherFrequencySnareThreshold, snareBandsThreshold))
                manyTimes[SoundParameter.SNARE.value()].add(time);

            if (beatDetectFrequencyLowSensitivity.isRange(lowerFrequencyKickThreshold, higherFrequencyKickThreshold, kickBandsThreshold))
                fewTimes[SoundParameter.KICKS.value()].add(time);
            if (beatDetectFrequencyLowSensitivity.isRange(lowerFrequencyHatThreshold, higherFrequencyHatThreshold, hatBandsThreshold))
                fewTimes[SoundParameter.HAT.value()].add(time);
            if (beatDetectFrequencyLowSensitivity.isRange(lowerFrequencySnareThreshold, higherFrequencySnareThreshold, snareBandsThreshold))
                fewTimes[SoundParameter.SNARE.value()].add(time);

            if (beatDetectSoundLowSensitivity.isOnset())
                fewTimes[SoundParameter.BEAT.value()].add(time);

            if (beatDetectSoundHighSensitivity.isOnset())
                manyTimes[SoundParameter.BEAT.value()].add(time);

            // if (beatDetectFrequencyHighSensitivity.isKick())
            //     manyTimes[SoundParameter.KICKS.value()].add(time);
            // if (beatDetectFrequencyHighSensitivity.isHat())
            //     manyTimes[SoundParameter.HAT.value()].add(time);
            // if (beatDetectFrequencyHighSensitivity.isSnare(false))
            //     manyTimes[SoundParameter.SNARE.value()].add(time);

            // if (beatDetectFrequencyLowSensitivity.isKick())
            //     fewTimes[SoundParameter.KICKS.value()].add(time);
            // if (beatDetectFrequencyLowSensitivity.isHat())
            //     fewTimes[SoundParameter.HAT.value()].add(time);
            // if (beatDetectFrequencyLowSensitivity.isSnare(false))
            //     fewTimes[SoundParameter.SNARE.value()].add(time);

            // if (beatDetectSoundLowSensitivity.isOnset())
            //     fewTimes[SoundParameter.BEAT.value()].add(time);

            // if (beatDetectSoundHighSensitivity.isOnset())
            //     manyTimes[SoundParameter.BEAT.value()].add(time);
            time += timePerSample;
        }

        float scaleBy = 1f / largestAvg;
        float scaleMaxBy = 1f / largestMax;
        for (int i = 0; i < MidFFTAmount.size(); i++) {
            MidFFTAmount.replace(i, MidFFTAmount.get(i) * scaleBy);
            MidFFTMaxes.replace(i, MidFFTMaxes.get(i) * scaleMaxBy);
        }

        // calculate differences between percussive elements,
        // then find the most common differences among all
        // use this to calculate BPM
        TFloatArrayList common = new TFloatArrayList();
        float doubleSpeed = 60f / (MAX_BPM * 2f);
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
        if (commonBPM > MAX_BPM) {
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

    public TFloatArrayList GetMidFFTAmount() {
        return MidFFTAmount;
    }

    public TFloatArrayList GetMidFFTMaxes() {
        return MidFFTMaxes;
    }

    public float GetTimePerBeat() {
        return timePerBeat;
    }

    public float GetStartTime() {
        return startTime;
    }
}
