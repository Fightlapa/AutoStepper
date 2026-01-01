package autostepper.soundprocessing;

import java.io.File;

import ddf.minim.Minim;
import gnu.trove.list.array.TFloatArrayList;

public interface ISoundProcessor {
    public void ProcessMusic(Minim minimLib, String filename, float songLengthLimitSeconds, final TFloatArrayList[] manyTimes, final TFloatArrayList[] fewTimes, TFloatArrayList params);

    public float GetBpm();
    public TFloatArrayList GetMidFFTAmount();
    public TFloatArrayList GetMidFFTMaxes();
    public TFloatArrayList getVolume();
    public float timePerSample();
    public float GetTimePerBeat();
    public float GetStartTime();
}
