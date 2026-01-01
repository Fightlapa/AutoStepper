package autostepper.soundprocessing;

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
    private float songTime;
    private String filename;
    private float timePerSample;
    private TFloatArrayList volume = new TFloatArrayList();

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

        float maxVolume = 0f;
        for (int chunkIdx = 0; chunkIdx < totalChunks; ++chunkIdx)
        {
            stream.read(buffer);
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
        float scaleVolume = 1f / maxVolume;
        for (int i = 0; i < volume.size(); i++) {
            volume.replace(i, volume.get(i) * scaleVolume);
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
}
