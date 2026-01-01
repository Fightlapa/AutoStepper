package autostepper.misc;

import java.io.File;
import java.util.ArrayList;

import autostepper.AutoStepper;
import ddf.minim.spi.AudioRecordingStream;
import gnu.trove.list.array.TFloatArrayList;

// class - a legend, everything and nothing in every project
public class Utils
{
    public static TFloatArrayList calculateDifferences(TFloatArrayList arr, float timeThreshold) {
        TFloatArrayList diff = new TFloatArrayList();
        int currentlyAt = 0;
        while (currentlyAt < arr.size() - 1) {
            float mytime = arr.getQuick(currentlyAt);
            int oldcurrentlyat = currentlyAt;
            for (int i = currentlyAt + 1; i < arr.size(); i++) {
                float diffcheck = arr.getQuick(i) - mytime;
                if (diffcheck >= timeThreshold) {
                    diff.add(diffcheck);
                    currentlyAt = i;
                    break;
                }
            }
            if (oldcurrentlyat == currentlyAt)
                break;
        }
        return diff;
    }

    public static float getDifferenceAverage(TFloatArrayList arr) {
        float avg = 0f;
        for (int i = 0; i < arr.size() - 1; i++) {
            avg += Math.abs(arr.getQuick(i + 1) - arr.getQuick(i));
        }
        if (arr.size() <= 1)
            return 0f;
        return avg / arr.size() - 1;
    }

    public static float getSongTime(String inputFile) {
        // anything to get song length
        int fftSize = 512;

        AudioRecordingStream stream = AutoStepper.minimLib.loadFileStream(inputFile, fftSize, false);
        float songTime = stream.getMillisecondLength() / 1000f;
        return songTime;
    }

}
