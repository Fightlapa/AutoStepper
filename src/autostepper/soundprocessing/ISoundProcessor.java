package autostepper.soundprocessing;

import java.io.File;

import ddf.minim.Minim;
import gnu.trove.list.array.TFloatArrayList;

public interface ISoundProcessor {
    public void ProcessMusic(Minim minimLib, File filename, float songLengthLimitSeconds, final TFloatArrayList[] manyTimes, final TFloatArrayList[] fewTimes);

    public float GetBpm();
    public TFloatArrayList GetMidFFTAmount();
    public TFloatArrayList GetMidFFTMaxes();
    public float timePerSample();
}
