package autostepper.musiceventsdetector;

import java.util.ArrayList;
import java.util.Map;

import autostepper.vibejudges.SoundParameter;
import gnu.trove.list.array.TFloatArrayList;

public abstract class CMusicEventsDetector {
    public abstract ArrayList<Map<SoundParameter, Object>> GetEvents(int stepGranularity, float timePerBeat,
            float timeOffset, float totalTime, TFloatArrayList FFTAverages, TFloatArrayList FFTMaxes, TFloatArrayList volumes, float timePerFFT,
            TFloatArrayList[] fewTimes, float sustainThresholdFactor, float granularityModifier, float preciseGranularityModifier);

    int getNearestTimeSeriesIndex(float time, TFloatArrayList timelist, float threshold) {
        for (int i = 0; i < timelist.size(); i++) {
            float checktime = timelist.get(i);
            float diff = Math.abs(checktime - time);
            if (diff <= threshold) {
                // Just one more try to get better fit
                if (i + 1 < timelist.size() && Math.abs(checktime - time) < diff)
                {
                    return i + 1;
                }
                return i;
            }
            if (checktime > time + threshold) {
                return -1;
            }
        }
        return -1;
    }

    // Checks if it's withing threshold on both + and - side
    boolean isNearATime(float time, TFloatArrayList timelist, float threshold) {
        for (int i = 0; i < timelist.size(); i++) {
            float checktime = timelist.get(i);
            if (Math.abs(checktime - time) < threshold) {
                return true;
            }
            if (checktime > time + threshold) {
                return false;
            }
        }
        return false;
    }

    boolean sustainedFFT(float startTime, float len, float granularity, float timePerFFT, TFloatArrayList FFTMaxes,
            TFloatArrayList FFTAvg, float aboveAvg, float averageMultiplier) {
        int endIndex = (int) Math.floor((startTime + len) / timePerFFT);
        if (endIndex >= FFTMaxes.size())
            return false;
        int wiggleRoom = Math.round(0.1f * len / timePerFFT);
        int startIndex = (int) Math.floor(startTime / timePerFFT);
        int pastGranu = (int) Math.floor((startTime + granularity) / timePerFFT);
        boolean startThresholdReached = false;
        for (int i = startIndex; i <= endIndex; i++) {
            float amt = FFTMaxes.getQuick(i);
            float avg = FFTAvg.getQuick(i) * averageMultiplier;
            if (i <= pastGranu) {
                startThresholdReached |= amt >= avg + aboveAvg;
            } else {
                if (startThresholdReached == false)
                    return false;
                if (amt < avg) {
                    wiggleRoom--;
                    if (wiggleRoom <= 0)
                        return false;
                }
            }
        }
        return true;
    }

}
