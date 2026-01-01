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

public class CExperimentalSoundProcessor implements ISoundProcessor
{
    public static float SAMPLE_REDUCTION_RATIO = 1000f;
    public static int BPM_SENSITIVITY_MS = 40;
    public static float MAX_BPM = 170f;
    public static float MIN_BPM = 70f;

    private float bpm;
    private TFloatArrayList MidFFTAmount;
    private TFloatArrayList MidFFTMaxes;
    private TFloatArrayList volume;
    private float timePerSample;
    private float startTime = 0f;
    private float timePerBeat = 0f;

    public void ProcessMusic(Minim minimLib, String filename, float songLengthLimitSeconds, final TFloatArrayList[] manyTimes, final TFloatArrayList[] fewTimes, TFloatArrayList params)
    {
        System.out.println("Processing sound");
        int fftSize = 512;

        AudioRecordingStream stream = minimLib.loadFileStream(filename, fftSize, false);

        System.out.println("\n[--- Processing " + songLengthLimitSeconds + "s of " + filename + " ---]");
        // tell it to "play" so we can read from it.
        stream.play();

        // create the fft/beatdetect objects we'll use for analysis
        BeatDetect beatDetectFrequencyHighSensitivity = new BeatDetect(fftSize, stream.getFormat().getSampleRate());
        BeatDetect beatDetectFrequencyLowSensitivity = new BeatDetect(fftSize, stream.getFormat().getSampleRate());
        BeatDetect beatDetectSoundHighSensitivity = new BeatDetect(stream.getFormat().getSampleRate());
        BeatDetect beatDetectSoundLowSensitivity = new BeatDetect(stream.getFormat().getSampleRate());
        beatDetectFrequencyHighSensitivity.setSensitivity(BPM_SENSITIVITY_MS);
        beatDetectSoundHighSensitivity.setSensitivity(BPM_SENSITIVITY_MS);
        beatDetectFrequencyLowSensitivity.setSensitivity(Math.round(60000f / (float) MAX_BPM));
        beatDetectSoundLowSensitivity.setSensitivity(Math.round(60000f / (float) MAX_BPM));

        FFT fft = new FFT(fftSize, stream.getFormat().getSampleRate());

        // create the buffer we use for reading from the stream
        MultiChannelBuffer buffer = new MultiChannelBuffer(fftSize, stream.getFormat().getChannels());

        // figure out how many samples are in the stream so we can allocate the correct
        // number of spectra
        int totalSamples = (int) (songLengthLimitSeconds * stream.getFormat().getSampleRate());
        timePerSample = fftSize / stream.getFormat().getSampleRate();

        // now we'll analyze the samples in chunks
        int totalChunks = (totalSamples / fftSize) + 1;

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
        volume = new TFloatArrayList();
        float maxVolume = 0f;
        float largestAvg = 0f, largestMax = 0f;
        int lowFreq = fft.freqToIndex(300f);
        int highFreq = fft.freqToIndex(3000f);
        for (int chunkIdx = 0; chunkIdx < totalChunks; ++chunkIdx)
        {
            if( AutoStepper.INDICATOR && chunkIdx == 50)
            {
                // Section to playback music
                AudioSample fullSong = AutoStepper.minimLib.loadSample(filename);
                stream.getMillisecondLength();
                fullSong.trigger();
                long millis = System.currentTimeMillis();

                long songLentgthToPlay = ((stream.getMillisecondLength() * chunkIdx) / totalChunks);
                System.out.println("Would play for : " + songLentgthToPlay);
                while (System.currentTimeMillis() - millis < songLentgthToPlay)
                {
                    // still play
                }
                fullSong.stop();
                fullSong.close();
            }


            stream.read(buffer);
            float[] data = buffer.getChannel(0);
            float time = chunkIdx * timePerSample;
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

            int lowerFrequencyKickThreshold = Math.round(params.get(AlgorithmParameter.KICK_LOW_FREQ.value()));
            int higherFrequencyKickThreshold = Math.round(params.get(AlgorithmParameter.KICK_HIGH_FREQ.value()));
            int kickBandsThreshold = Math.round(params.get(AlgorithmParameter.KICK_BAND_FREQ.value()));

            int lowerFrequencySnareThreshold = Math.round(params.get(AlgorithmParameter.SNARE_LOW_FREQ.value()));
            int higherFrequencySnareThreshold = Math.round(params.get(AlgorithmParameter.SNARE_HIGH_FREQ.value()));
            int snareBandsThreshold = Math.round(params.get(AlgorithmParameter.SNARE_BAND_FREQ.value()));

            if (beatDetectFrequencyHighSensitivity.isRange(lowerFrequencyKickThreshold, higherFrequencyKickThreshold, kickBandsThreshold))
                manyTimes[SoundParameter.KICKS.value()].add(time);
            // if (beatDetectFrequencyHighSensitivity.isHat())
            //     manyTimes[SoundParameter.HAT.value()].add(time);
            if (beatDetectFrequencyHighSensitivity.isRange(lowerFrequencySnareThreshold, higherFrequencySnareThreshold, snareBandsThreshold))
                manyTimes[SoundParameter.SNARE.value()].add(time);

            if (beatDetectFrequencyLowSensitivity.isRange(lowerFrequencyKickThreshold, higherFrequencyKickThreshold, kickBandsThreshold))
                fewTimes[SoundParameter.KICKS.value()].add(time);
            // if (beatDetectFrequencyLowSensitivity.isHat())
            //     fewTimes[SoundParameter.HAT.value()].add(time);
            if (beatDetectFrequencyLowSensitivity.isRange(lowerFrequencySnareThreshold, higherFrequencySnareThreshold, snareBandsThreshold))
                fewTimes[SoundParameter.SNARE.value()].add(time);

            if (beatDetectSoundLowSensitivity.isOnset())
                fewTimes[SoundParameter.BEAT.value()].add(time);

            if (beatDetectSoundHighSensitivity.isOnset())
                manyTimes[SoundParameter.BEAT.value()].add(time);

            // if (beatDetectFrequencyHighSensitivity.isKick())
            //     manyTimes[SoundParameter.KICKS.value()].add(time);
            // // if (beatDetectFrequencyHighSensitivity.isHat())
            // //     manyTimes[SoundParameter.HAT.value()].add(time);
            // if (beatDetectFrequencyHighSensitivity.isSnare(false))
            //     manyTimes[SoundParameter.SNARE.value()].add(time);

            // if (beatDetectFrequencyLowSensitivity.isKick())
            //     fewTimes[SoundParameter.KICKS.value()].add(time);
            // // if (beatDetectFrequencyLowSensitivity.isHat())
            // //     fewTimes[SoundParameter.HAT.value()].add(time);
            // if (beatDetectFrequencyLowSensitivity.isSnare(false))
            //     fewTimes[SoundParameter.SNARE.value()].add(time);

            // if (beatDetectSoundLowSensitivity.isOnset())
            //     fewTimes[SoundParameter.BEAT.value()].add(time);

            // if (beatDetectSoundHighSensitivity.isOnset())
            //     manyTimes[SoundParameter.BEAT.value()].add(time);

            float rms = 0f;
            float[] samples = buffer.getChannel(0);
            for (float s : samples) {
                rms += s * s;
            }
            float currVolume = (float) Math.sqrt(rms / samples.length);
            volume.add(currVolume);
            if (currVolume > maxVolume)
            {
                maxVolume = currVolume;
            }
        }
        // System.out.println("Loudest midrange average to normalize to 1: " + largestAvg);
        // System.out.println("Loudest midrange maximum to normalize to 1: " + largestMax);
        float scaleBy = 1f / largestAvg;
        float scaleMaxBy = 1f / largestMax;
        float scaleVolume = 1f / maxVolume;
        for (int i = 0; i < MidFFTAmount.size(); i++) {
            MidFFTAmount.replace(i, MidFFTAmount.get(i) * scaleBy);
            MidFFTMaxes.replace(i, MidFFTMaxes.get(i) * scaleMaxBy);
            volume.replace(i, volume.get(i) * scaleVolume);
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
            return;
        }
        bpm = Math.round(Averages.getMostCommonFightlapa(common, 0.5f, true));

        timePerBeat = 60f / bpm;
        TFloatArrayList startTimes = new TFloatArrayList();
        for (int i = 0; i < fewTimes.length; i++) {
            startTimes.add(AutoStepper.getBestOffset(timePerBeat, fewTimes[i], 0.01f));
            startTimes.add(AutoStepper.getBestOffset(timePerBeat, manyTimes[i], 0.01f));
        }
        // give extra weight to fewKicks
        float kickStartTime = AutoStepper.getBestOffset(timePerBeat, fewTimes[SoundParameter.KICKS.value()], 0.01f);
        startTimes.add(kickStartTime);
        startTimes.add(kickStartTime);
        startTime = -Averages.getMostCommonPhr00t(startTimes, 0.02f, false);
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

    @Override
    public float GetBpm() {
        return bpm;
    }

    @Override
    public TFloatArrayList GetMidFFTAmount() {
        return MidFFTAmount;
    }

    @Override
    public TFloatArrayList getVolume() {
        return volume;
    }

    @Override
    public TFloatArrayList GetMidFFTMaxes() {
        return MidFFTMaxes;
    }

    @Override
    public float timePerSample() {
        return timePerSample;
    }

    @Override
    public float GetTimePerBeat() {
        return timePerBeat;
    }

    @Override
    public float GetStartTime() {
        return startTime;
    }
}
