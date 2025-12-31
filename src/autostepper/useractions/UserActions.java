package autostepper.useractions;

import java.util.Scanner;

import autostepper.AutoStepper;
import ddf.minim.AudioSample;
import gnu.trove.list.array.TFloatArrayList;


public class UserActions
{
    public static BPMOffset getTappedBPM(String filename) {
        // now we load the whole song so we don't have to worry about streaming a
        // variable mp3 with timing inaccuracies
        System.out.println("Loading whole song for tapping...");
        AudioSample fullSong = AutoStepper.minimLib.loadSample(filename);
        System.out.println(
                "\n********************************************************************\n\nPress [ENTER] to start song, then press [ENTER] to tap to the beat.\nIt will complete after 30 entries.\nDon't worry about hitting the first beat, just start anytime.\n\n********************************************************************");
        TFloatArrayList positions = new TFloatArrayList();
        Scanner in = new Scanner(System.in);
        try {
            in.nextLine();
        } catch (Exception e) {
        }
        // get the most accurate start time as possible
        long nano = System.nanoTime();
        fullSong.trigger();
        nano = (System.nanoTime() + nano) / 2;
        try {
            for (int i = 0; i < 30; i++) {
                in.nextLine();
                // get two playtime values & average them together for accuracy
                long now = System.nanoTime();
                // calculate the time difference
                // we note a consistent 0.11 second delay in input to song here
                double time = (double) ((now - nano) / 1000000000.0) + AutoStepper.TAPSYNC;
                positions.add((float) time);
                System.out.println("#" + positions.size() + "/30: " + time + "s");
            }
        } catch (Exception e) {
        }
        fullSong.stop();
        fullSong.close();
        float avg = ((positions.getQuick(positions.size() - 1) - positions.getQuick(0)) / (positions.size() - 1));
        int BPM = (int) Math.floor(60f / avg);
        float timePerBeat = 60f / BPM;
        float tappedOffset = -AutoStepper.getBestOffset(timePerBeat, positions, 0.1f);
        return new BPMOffset(BPM, tappedOffset);
    }
}
