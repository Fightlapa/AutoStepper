package autostepper.vibejudges;

import java.util.ArrayList;
import java.util.Map;

public interface IVibeJudge {

    public ArrayList<Map<VibeScore, Integer>> GetVibes(ArrayList<Map<SoundParameter, Object>> NoteEvents);
}
