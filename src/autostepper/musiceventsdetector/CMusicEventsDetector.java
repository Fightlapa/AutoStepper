package autostepper.musiceventsdetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import autostepper.vibejudges.SoundParameter;
import gnu.trove.list.array.TFloatArrayList;

public class CMusicEventsDetector {
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

    public ArrayList<Map<SoundParameter, Object>> GetEvents(int stepGranularity, float timePerBeat, float timeOffset,
            float totalTime, TFloatArrayList FFTAverages, TFloatArrayList FFTMaxes, TFloatArrayList volumes, float timePerFFT,
            TFloatArrayList[] fewTimes, float sustainThresholdFactor, float granularityModifier, float preciseGranularityModifier) {
        ArrayList<Map<SoundParameter, Object>> NoteEvents = new ArrayList<>();
        int timeIndex = 0;
        float timeGranularity = timePerBeat / stepGranularity;
        float standardPrecision = timeGranularity * granularityModifier;
        float highPrecision = standardPrecision * preciseGranularityModifier;
        float ultraPrecision = highPrecision / 2f;

        long sustains = 0;
        long kicks = 0;
        long snares = 0;
        long beat = 0;
        long hats = 0;
        long silences = 0;
        long beats = 0;

        for(float t = timeOffset; t <= totalTime; t += timeGranularity) {
            boolean isQuarterBeat = false;
            boolean isHalfBeat = false;
            if (stepGranularity == 2)
            {
                isHalfBeat = timeIndex % 2 == 1;
            }
            else if (stepGranularity == 4)
            {
                isQuarterBeat = (timeIndex % 4 == 1 || timeIndex % 4 == 3);
                isHalfBeat = timeIndex % 2 == 2;
            }
            boolean nearKick = false;
            boolean nearHat = false;
            boolean nearSnare = false;
            boolean nearEnergy = false;
            boolean silence = true;
            if( t > 0f ) {
                int idx = (int)Math.floor((t * FFTAverages.size()) / totalTime);
                float fftmax = FFTMaxes.getQuick(idx);
                float fftavg = FFTAverages.getQuick(idx);
                boolean sustained = sustainedFFT(t, 0.75f, timeGranularity, timePerFFT, FFTMaxes, FFTAverages, sustainThresholdFactor, sustainThresholdFactor * 2);

                // OLD
                // nearKick = isNearATime(t, fewTimes[SoundParameter.KICKS.value()], checkWindow);
                // nearSnare = isNearATime(t, fewTimes[SoundParameter.SNARE.value()], checkWindow);
                // nearEnergy = isNearATime(t, fewTimes[SoundParameter.BEAT.value()], checkWindow);
                // // nearHat = isNearATime(t, fewTimes[SoundParameter.HAT.value()], checkWindow);

                // new
                nearKick = isNearATime(t, fewTimes[SoundParameter.KICKS.value()], standardPrecision) && !isNearATime(t - highPrecision, fewTimes[SoundParameter.KICKS.value()], ultraPrecision);
                nearSnare = isNearATime(t, fewTimes[SoundParameter.SNARE.value()], standardPrecision) && !isNearATime(t - highPrecision, fewTimes[SoundParameter.SNARE.value()], ultraPrecision);
                nearEnergy = isNearATime(t, fewTimes[SoundParameter.BEAT.value()], standardPrecision) && !isNearATime(t - highPrecision, fewTimes[SoundParameter.BEAT.value()], ultraPrecision);
                // nearHat = isNearATime(t, fewTimes[SoundParameter.HAT.value()], standardPrecision) && !isNearATime(t - highPrecision, fewTimes[SoundParameter.HAT.value()], ultraPrecision);
                Map<SoundParameter, Object> events = new HashMap<>();
                events.put(SoundParameter.KICKS, nearKick);
                events.put(SoundParameter.SNARE, nearSnare);
                events.put(SoundParameter.BEAT, nearEnergy);
                // events.put(SoundParameter.HAT, nearHat);
                events.put(SoundParameter.SUSTAINED, sustained);
                // Some heuristic, best effort detection
                silence = (fftmax + fftavg) < 0.2f;
                events.put(SoundParameter.SILENCE, silence);
                events.put(SoundParameter.HALF_BEAT, nearEnergy && isHalfBeat);
                events.put(SoundParameter.QUARTER_BEAT, nearEnergy && isQuarterBeat);
                events.put(SoundParameter.NOTHING, false);
                events.put(SoundParameter.FFT_MAX, fftmax);
                events.put(SoundParameter.FFT_AVG, fftavg);
                events.put(SoundParameter.VOLUME, volumes.getQuick(idx));
                
                if (sustained) sustains++;
                if (nearSnare) snares++;
                if (nearKick) kicks++;
                if (nearHat) hats++;
                if (silence) silences++;
                if (nearEnergy) beats++;
                NoteEvents.add(events);
            }
            else
            {
                Map<SoundParameter, Object> events = new HashMap<>();
                events.put(SoundParameter.KICKS, false);
                events.put(SoundParameter.SNARE, false);
                events.put(SoundParameter.BEAT, false);
                events.put(SoundParameter.SUSTAINED, false);
                events.put(SoundParameter.SILENCE, true);
                events.put(SoundParameter.HALF_BEAT, false);
                events.put(SoundParameter.QUARTER_BEAT, false);
                // events.put(SoundParameter.HAT, false);
                events.put(SoundParameter.NOTHING, true);
                events.put(SoundParameter.FFT_MAX, 0f);
                events.put(SoundParameter.FFT_AVG, 0f);
                events.put(SoundParameter.VOLUME, 0f);
                NoteEvents.add(events);
            }
            timeIndex++;
        }

        // System.out.println("SustainFactor: " + sustainFactor);
        System.out.println("Sustains: " + sustains + ", Kicks: " + kicks + ", Snares: " + snares + ", Beat: " + beats + ", Silence: " + silences);

        return NoteEvents;
    }
}
