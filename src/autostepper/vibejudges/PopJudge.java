package autostepper.vibejudges;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PopJudge implements IVibeJudge {

    @Override
    public ArrayList<Map<VibeScore, Integer>> GetVibes(ArrayList<Map<SoundParameter, Object>> NoteEvents)
    {
        ArrayList<Map<VibeScore, Integer>> NoteVibes = new ArrayList<>();

        for (Map<SoundParameter,Object> map : NoteEvents) {
            int vibePower = 0;
            int sustainPower = 0;
            if (!(boolean)map.get(SoundParameter.NOTHING))
            {
                if ((boolean)map.get(SoundParameter.KICKS))
                {
                    // Some standard sound or something to sustain deserves at least some vibe
                    vibePower++;
                }

                if ((boolean)map.get(SoundParameter.SNARE))
                {
                    // Some standard sound or something to sustain deserves at least some vibe
                    vibePower++;
                }
                
                if ((boolean)map.get(SoundParameter.BEAT))
                {
                    // Some standard sound or something to sustain deserves at least some vibe
                    vibePower++;
                }

                if ((boolean)map.get(SoundParameter.BEAT) && (boolean)map.get(SoundParameter.KICKS) && !(boolean)map.get(SoundParameter.HALF_BEAT))
                {
                    // If it's kicks, on beat, without half-beat, that sounds like a good candidate for jump, that sounds like good vibe
                    vibePower++;
                }

                if ((float)map.get(SoundParameter.FFT_AVG) > 0.8f)
                {
                    // That's some loud sound right there, give it some vibe
                    vibePower++;
                }

                if ( (boolean)map.get(SoundParameter.SUSTAINED) ) {
                    sustainPower++;
                    if ( (boolean)map.get(SoundParameter.BEAT) || (boolean)map.get(SoundParameter.HALF_BEAT))
                    {
                        // Extra boost if that's the beat
                        sustainPower++;
                    }
                } else if( (boolean)map.get(SoundParameter.SILENCE) ) {
                    sustainPower--;
                }
            }
            Map<VibeScore, Integer> noteVibe = new HashMap<>();
            noteVibe.put(VibeScore.POWER, vibePower);
            noteVibe.put(VibeScore.SUSTAIN, sustainPower);
            NoteVibes.add(noteVibe);
        }
        return NoteVibes;
    }
    
}
