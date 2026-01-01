package autostepper.musiceventsdetector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import autostepper.vibejudges.SoundParameter;
import gnu.trove.list.array.TFloatArrayList;

public class PreciseDiffSensitiveEventsDetector extends CMusicEventsDetector {

    @Override
    public ArrayList<Map<SoundParameter, Object>> GetEvents(int stepGranularity, float timePerBeat, float timeOffset,
            float totalTime, TFloatArrayList FFTAverages, TFloatArrayList FFTMaxes, TFloatArrayList volumes, float timePerFFT,
            TFloatArrayList[] fewTimes, float sustainThresholdFactor, float granularityModifier, float preciseGranularityModifier) {
        ArrayList<Map<SoundParameter, Object>> NoteEvents = new ArrayList<>();

        int timeIndex = 0;
        float timeGranularity = timePerBeat / stepGranularity;
        float highPrecision = timeGranularity / 2;
        float ultraPrecision = timeGranularity / 4;
        for(float t = timeOffset; t <= totalTime; t += timeGranularity) {
            boolean isHalfBeat = timeIndex % 2 == 1;
            boolean nearKick = false;
            boolean nearHat = false;
            boolean nearSnare = false;
            boolean nearEnergy = false;
            if( t > 0f ) {
                int idx = (int)Math.floor((t * FFTAverages.size()) / totalTime);
                // float fftmax = getFFT(t, FFTMaxes, timePerFFT);
                float fftmax = FFTMaxes.getQuick(idx);
                float fftavg = FFTAverages.getQuick(idx);
                boolean sustained = sustainedFFT(t, 0.75f, timeGranularity, timePerFFT, FFTMaxes, FFTAverages, sustainThresholdFactor, sustainThresholdFactor * 2);

                nearKick = isNearATime(t, fewTimes[SoundParameter.KICKS.value()], timeGranularity) && !isNearATime(t - highPrecision, fewTimes[SoundParameter.KICKS.value()], highPrecision);
                int previousKickTime = getNearestTimeSeriesIndex(t, fewTimes[SoundParameter.KICKS.value()], timeGranularity) - 1;
                if (previousKickTime >= 0)
                {

                }

                nearSnare = isNearATime(t, fewTimes[SoundParameter.SNARE.value()], timeGranularity) && !isNearATime(t - highPrecision, fewTimes[SoundParameter.SNARE.value()], highPrecision);
                nearEnergy = isNearATime(t, fewTimes[SoundParameter.BEAT.value()], timeGranularity) && !isNearATime(t - highPrecision, fewTimes[SoundParameter.BEAT.value()], highPrecision);
                // nearHat = isNearATime(t, fewTimes[SoundParameter.HAT.value()], timeGranularity) && !isNearATime(t - highPrecision, fewTimes[SoundParameter.HAT.value()], highPrecision);
                Map<SoundParameter, Object> events = new HashMap<>();
                events.put(SoundParameter.KICKS, nearKick);
                events.put(SoundParameter.SNARE, nearSnare);
                events.put(SoundParameter.BEAT, nearEnergy);
                // events.put(SoundParameter.HAT, nearHat);
                events.put(SoundParameter.SUSTAINED, sustained);
                // Some heuristic, best effort detection
                events.put(SoundParameter.SILENCE, (fftmax + fftavg) < 0.2f);
                events.put(SoundParameter.HALF_BEAT, nearEnergy && isHalfBeat);
                events.put(SoundParameter.NOTHING, false);
                events.put(SoundParameter.FFT_MAX, fftmax);
                events.put(SoundParameter.FFT_AVG, fftavg);
                events.put(SoundParameter.VOLUME, volumes.getQuick(idx));
                
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
                // events.put(SoundParameter.HAT, false);
                events.put(SoundParameter.NOTHING, true);
                events.put(SoundParameter.FFT_MAX, 0f);
                events.put(SoundParameter.FFT_AVG, 0f);
                events.put(SoundParameter.VOLUME, 0f);
                NoteEvents.add(events);
            }
            timeIndex++;
        }
        return NoteEvents;
    }
    
}
