package autostepper.soundprocessing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import autostepper.AutoStepper;
import autostepper.misc.Utils;
import autostepper.vibejudges.SoundParameter;
import ddf.minim.AudioSample;
import ddf.minim.MultiChannelBuffer;
import ddf.minim.analysis.BeatDetect;
import ddf.minim.analysis.FFT;
import ddf.minim.spi.AudioRecordingStream;
import gnu.trove.list.array.TFloatArrayList;

public class Song
{
    public static int FFT_SIZE = 512;
    public static int BPM_SENSITIVITY_MS = 23;
    public static float MAX_BPM = 170f;
    private float songTime;
    private String filename;
    private float timePerSample;
    private TFloatArrayList volume = new TFloatArrayList();
    private TFloatArrayList MidFFTAmount = new TFloatArrayList();
    private TFloatArrayList MidFFTMaxes = new TFloatArrayList();

    final Map<FrequencyConfig, TFloatArrayList> kicksLookupManyTimes = new HashMap<>();
    final Map<FrequencyConfig, TFloatArrayList> kicksLookupFewTimes = new HashMap<>();
    final Map<FrequencyConfig, TFloatArrayList> snaresLookupManyTimes = new HashMap<>();
    final Map<FrequencyConfig, TFloatArrayList> snaresLookupFewTimes = new HashMap<>();
    final Map<FrequencyConfig, TFloatArrayList> hatsLookupManyTimes = new HashMap<>();
    final Map<FrequencyConfig, TFloatArrayList> hatsLookupFewTimes = new HashMap<>();
    final TFloatArrayList beatArrayFewTimes = new TFloatArrayList();
    final TFloatArrayList beatArrayManyTimes = new TFloatArrayList();

    public Song(String filename)
    {
        this.filename = filename;
        songTime = Utils.getSongTime(this.filename);

        AudioRecordingStream stream = AutoStepper.minimLib.loadFileStream(filename, FFT_SIZE, false);

        // tell it to "play" so we can read from it.
        stream.play();

        // create the buffer we use for reading from the stream
        MultiChannelBuffer buffer = new MultiChannelBuffer(FFT_SIZE, stream.getFormat().getChannels());

        // figure out how many samples are in the stream so we can allocate the correct
        // number of spectra
        int totalSamples = (int) (songTime * stream.getFormat().getSampleRate());
        timePerSample = FFT_SIZE / stream.getFormat().getSampleRate();

        // now we'll analyze the samples in chunks
        int totalChunks = (totalSamples / FFT_SIZE) + 1;

        float largestAvg = 0f, largestMax = 0f;
        FFT fft = new FFT(FFT_SIZE, stream.getFormat().getSampleRate());
        int lowFreq = fft.freqToIndex(300f);
        int highFreq = fft.freqToIndex(3000f);

        float maxVolume = 0f;
        for (int chunkIdx = 0; chunkIdx < totalChunks; ++chunkIdx)
        {
            stream.read(buffer);
            float[] samples = buffer.getChannel(0);

            fft.forward(samples);
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

            float rms = 0f;
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

        float scaleBy = 1f / largestAvg;
        float scaleMaxBy = 1f / largestMax;
        float scaleVolume = 1f / maxVolume;
        for (int i = 0; i < volume.size(); i++) {
            volume.replace(i, volume.get(i) * scaleVolume);
            MidFFTAmount.replace(i, MidFFTAmount.get(i) * scaleBy);
            MidFFTMaxes.replace(i, MidFFTMaxes.get(i) * scaleMaxBy);
        }
    }

    /**
     * This can be used for preprocessing, so that when revisiting sound parsing maybe we already calculated such results
     */
    public void processPercussions(FrequencyConfig kickConfig, FrequencyConfig snareConfig, FrequencyConfig hatConfig, TFloatArrayList[] fewTimes, TFloatArrayList[] manyTimes)
    {
        long timer = System.currentTimeMillis();
        for (int i = 0; i < fewTimes.length; i++) {
            if (fewTimes[i] == null)
                fewTimes[i] = new TFloatArrayList();
            if (manyTimes[i] == null)
                manyTimes[i] = new TFloatArrayList();
            fewTimes[i].clear();
            manyTimes[i].clear();
        }

        boolean kicksDone = false;
        boolean snaresDone = false;
        boolean hatsDone = false;
        boolean beatsDone = false;
        if (kicksLookupManyTimes.containsKey(kickConfig))
        {
            manyTimes[SoundParameter.KICKS.value()].addAll(kicksLookupManyTimes.get(kickConfig));
            fewTimes[SoundParameter.KICKS.value()].addAll(kicksLookupFewTimes.get(kickConfig));
            kicksDone = true;
        }
        if (snaresLookupFewTimes.containsKey(snareConfig))
        {
            manyTimes[SoundParameter.SNARE.value()].addAll(snaresLookupManyTimes.get(snareConfig));
            fewTimes[SoundParameter.SNARE.value()].addAll(snaresLookupFewTimes.get(snareConfig));
            snaresDone = true;
        }
        if (hatsLookupFewTimes.containsKey(hatConfig))
        {
            manyTimes[SoundParameter.HAT.value()].addAll(hatsLookupManyTimes.get(hatConfig));
            fewTimes[SoundParameter.HAT.value()].addAll(hatsLookupFewTimes.get(hatConfig));
            hatsDone = true;
        }
        if (!beatArrayFewTimes.isEmpty())
        {
            fewTimes[SoundParameter.BEAT.value()].addAll(beatArrayFewTimes);
            manyTimes[SoundParameter.BEAT.value()].addAll(beatArrayManyTimes);
            beatsDone = true;
        }

        if (kicksDone && snaresDone && hatsDone && beatsDone)
        {
            if (AutoStepper.DEBUG_TIMINGS)
            {
                System.out.println("DONE FROM CACHE!" + (System.currentTimeMillis() - timer) / 1000f + "s");
            }
            return; // Done from cache!
        }

        AudioRecordingStream stream = AutoStepper.minimLib.loadFileStream(getFilename(), Song.FFT_SIZE, false);

        // tell it to "play" so we can read from it.
        stream.play();

        // create the fft/beatdetect objects we'll use for analysis
        BeatDetect beatDetectFrequencyHighSensitivity = new BeatDetect(Song.FFT_SIZE, stream.getFormat().getSampleRate());
        BeatDetect beatDetectFrequencyLowSensitivity = new BeatDetect(Song.FFT_SIZE, stream.getFormat().getSampleRate());
        BeatDetect beatDetectSoundHighSensitivity = new BeatDetect(stream.getFormat().getSampleRate());
        BeatDetect beatDetectSoundLowSensitivity = new BeatDetect(stream.getFormat().getSampleRate());
        beatDetectFrequencyHighSensitivity.setSensitivity(BPM_SENSITIVITY_MS);
        beatDetectSoundHighSensitivity.setSensitivity(BPM_SENSITIVITY_MS);
        beatDetectFrequencyLowSensitivity.setSensitivity(Math.round(60000f / (float) MAX_BPM));
        beatDetectSoundLowSensitivity.setSensitivity(Math.round(60000f / (float) MAX_BPM));

        // create the buffer we use for reading from the stream
        MultiChannelBuffer buffer = new MultiChannelBuffer(Song.FFT_SIZE, stream.getFormat().getChannels());

        // figure out how many samples are in the stream so we can allocate the correct
        // number of spectra
        int totalSamples = (int) (getSongTime() * stream.getFormat().getSampleRate());

        // now we'll analyze the samples in chunks
        int totalChunks = (totalSamples / Song.FFT_SIZE) + 1;


        float timePerSample = getTimePerSample();
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

            // store basic percussion times
            if (!kicksDone)
            {
                if (beatDetectFrequencyHighSensitivity.isRange(kickConfig.getMinimalFrequency(), kickConfig.getMaximumFrequency(), kickConfig.getBandwithThreshold()))
                    manyTimes[SoundParameter.KICKS.value()].add(time);
                if (beatDetectFrequencyLowSensitivity.isRange(kickConfig.getMinimalFrequency(), kickConfig.getMaximumFrequency(), kickConfig.getBandwithThreshold()))
                    fewTimes[SoundParameter.KICKS.value()].add(time);
            }

            if (!snaresDone)
            {
                if (beatDetectFrequencyHighSensitivity.isRange(snareConfig.getMinimalFrequency(), snareConfig.getMaximumFrequency(), snareConfig.getBandwithThreshold()))
                    manyTimes[SoundParameter.SNARE.value()].add(time);
                if (beatDetectFrequencyLowSensitivity.isRange(snareConfig.getMinimalFrequency(), snareConfig.getMaximumFrequency(), snareConfig.getBandwithThreshold()))
                    fewTimes[SoundParameter.SNARE.value()].add(time);
            }

            if (!hatsDone)
            {
                if (beatDetectFrequencyHighSensitivity.isRange(hatConfig.getMinimalFrequency(), hatConfig.getMaximumFrequency(), hatConfig.getBandwithThreshold()))
                    manyTimes[SoundParameter.HAT.value()].add(time);
                if (beatDetectFrequencyLowSensitivity.isRange(hatConfig.getMinimalFrequency(), hatConfig.getMaximumFrequency(), hatConfig.getBandwithThreshold()))
                    fewTimes[SoundParameter.HAT.value()].add(time);
            }


            if (!beatsDone)
            {
                if (beatDetectSoundLowSensitivity.isOnset())
                    fewTimes[SoundParameter.BEAT.value()].add(time);
                if (beatDetectSoundHighSensitivity.isOnset())
                    manyTimes[SoundParameter.BEAT.value()].add(time);
            }

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

        // store basic percussion times
        if (!kicksDone)
        {
            kicksLookupFewTimes.putIfAbsent(kickConfig, new TFloatArrayList());
            kicksLookupManyTimes.putIfAbsent(kickConfig, new TFloatArrayList());
            kicksLookupFewTimes.get(kickConfig).addAll(fewTimes[SoundParameter.KICKS.value()]);
            kicksLookupManyTimes.get(kickConfig).addAll(manyTimes[SoundParameter.KICKS.value()]);
        }

        if (!snaresDone)
        {
            snaresLookupFewTimes.putIfAbsent(snareConfig, new TFloatArrayList());
            snaresLookupManyTimes.putIfAbsent(snareConfig, new TFloatArrayList());
            snaresLookupFewTimes.get(snareConfig).addAll(fewTimes[SoundParameter.SNARE.value()]);
            snaresLookupManyTimes.get(snareConfig).addAll(manyTimes[SoundParameter.SNARE.value()]);
        }

        if (!hatsDone)
        {
            hatsLookupFewTimes.putIfAbsent(hatConfig, new TFloatArrayList());
            hatsLookupManyTimes.putIfAbsent(hatConfig, new TFloatArrayList());
            hatsLookupFewTimes.get(hatConfig).addAll(fewTimes[SoundParameter.HAT.value()]);
            hatsLookupManyTimes.get(hatConfig).addAll(manyTimes[SoundParameter.HAT.value()]);
        }


        if (!beatsDone)
        {
            beatArrayFewTimes.addAll(fewTimes[SoundParameter.BEAT.value()]);
            beatArrayManyTimes.addAll(manyTimes[SoundParameter.BEAT.value()]);
        }

        if (AutoStepper.DEBUG_TIMINGS)
        {
            System.out.println("MAIN LOOP TIME: " + (System.currentTimeMillis() - timer) / 1000f + "s");
        }
    }

    public float getSongTime() {
        return songTime;
    }

    public String getFilename() {
        return filename;
    }

    public float getTimePerSample() {
        return timePerSample;
    }

    public TFloatArrayList getVolume() {
        return volume;
    }

    public TFloatArrayList getMidFFTAmount() {
        return MidFFTAmount;
    }

    public TFloatArrayList getMidFFTMaxes() {
        return MidFFTMaxes;
    }
}
