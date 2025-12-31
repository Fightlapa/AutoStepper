package autostepper;

import ddf.minim.AudioSample;
import ddf.minim.Minim;
import ddf.minim.MultiChannelBuffer;
import ddf.minim.analysis.BeatDetect;
import ddf.minim.analysis.FFT;
import ddf.minim.spi.AudioRecordingStream;
import gnu.trove.list.array.TFloatArrayList;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

import autostepper.misc.Averages;
import autostepper.misc.Utils;
import autostepper.moveassigners.SimfileDifficulty;
import autostepper.soundprocessing.CExperimentalSoundProcessor;
import autostepper.soundprocessing.CStandardSoundProcessor;
import autostepper.soundprocessing.ISoundProcessor;
import autostepper.useractions.BPMOffset;
import autostepper.useractions.UserActions;
import autostepper.vibejudges.SoundParameter;

/**
 *
 * @author Phr00t
 */
public class AutoStepper {

    public static boolean INDICATOR = false;
    public static boolean DEBUG_STEPS = true;
    public static boolean RANDOMIZED = false;
    public static boolean PREVIEW_DETECTION = false;

    // Having let's say sample rate of 44.100
    // We might want to reduce it a bit

    public static float STARTSYNC = 0.0f;

    public static double TAPSYNC = -0.11;
    public static boolean USETAPPER = false, HARDMODE = false, UPDATESM = false;

    public static Minim minimLib;
    public static AutoStepper myAS = new AutoStepper();

    // for minim
    public String sketchPath(String fileName) {
        return fileName;
    }

    // for minim
    public InputStream createInput(String fileName) {
        try {
            return new FileInputStream(new File(fileName));
        } catch (Exception e) {
            return null;
        }
    }

    // argument parser
    public static String getArg(String[] args, String argname, String def) {
        try {
            for (String s : args) {
                s = s.replace("\"", "");
                if (s.startsWith(argname)) {
                    return s.substring(s.indexOf("=") + 1).toLowerCase();
                }
            }
        } catch (Exception e) {
        }
        return def;
    }

    // argument parser
    public static boolean hasArg(String[] args, String argname) {
        for (String s : args) {
            if (s.toLowerCase().equals(argname))
                return true;
        }
        return false;
    }

    public static void main(String[] args) {
        minimLib = new Minim(myAS);
        String outputDir, input;
        float duration;
        System.out.println(
                "Starting AutoStepper by Phr00t's Software, v1.7 (See www.phr00t.com for more goodies!). Fork by Fightlapa - big thanks to original author.");
        if (hasArg(args, "help") || hasArg(args, "h") || hasArg(args, "?") || hasArg(args, "-help")
                || hasArg(args, "-?") || hasArg(args, "-h")) {
            System.out.println("Argument usage (all fields are optional):\n"
                    + "input=<file or dir> output=<songs dir> duration=<seconds to process, default: 90> tap=<true/false> tapsync=<tap time offset, default: -0.11> hard=<true/false> updatesm=<true/false>");
            return;
        }
        outputDir = getArg(args, "output", ".");
        if (outputDir.endsWith("/") == false)
            outputDir += "/";
        input = getArg(args, "input", ".");
        duration = Float.parseFloat(getArg(args, "duration", "90"));
        STARTSYNC = Float.parseFloat(getArg(args, "synctime", "0.0"));
        USETAPPER = getArg(args, "tap", "false").equals("true");
        PREVIEW_DETECTION = getArg(args, "preview", "false").equals("true");
        TAPSYNC = Double.parseDouble(getArg(args, "tapsync", "-0.11"));
        UPDATESM = getArg(args, "updatesm", "false").equals("true");
        File inputFile = new File(input);

        duration = correctTime(inputFile, duration);

        if (inputFile.isFile()) {
            myAS.analyzeUsingAudioRecordingStream(inputFile, duration, outputDir);
        } else if (inputFile.isDirectory()) {
            System.out.println("Processing directory: " + inputFile.getAbsolutePath());
            File[] allfiles = inputFile.listFiles();
            for (File f : allfiles) {
                String extCheck = f.getName().toLowerCase();
                if (f.isFile() &&
                        (extCheck.endsWith(".mp3") || extCheck.endsWith(".wav"))) {
                    myAS.analyzeUsingAudioRecordingStream(f, duration, outputDir);
                } else {
                    System.out.println("Skipping unsupported file: " + f.getName());
                }
            }
        } else {
            System.out.println("Couldn't find any input files.");
        }
    }

    private static float correctTime(File inputFile, float duration) {
        // anything to get song length
        int fftSize = 512;

        AudioRecordingStream stream = minimLib.loadFileStream(inputFile.getAbsolutePath(), fftSize, false);
        float songTime = stream.getMillisecondLength() / 1000f;
        if (duration > songTime) {
            duration = songTime;
        }
        return duration;
    }

    public static float getBestOffset(float timePerBeat, TFloatArrayList times, float groupBy) {
        TFloatArrayList offsets = new TFloatArrayList();
        for (int i = 0; i < times.size(); i++) {
            offsets.add(times.getQuick(i) % timePerBeat);
        }
        return Averages.getMostCommonPhr00t(offsets, groupBy, false);
    }

    void analyzeUsingAudioRecordingStream(File filename, float songLengthLimitSeconds, String outputDir) {

        // ISoundProcessor soundProcessor = new CStandardSoundProcessor();
        ISoundProcessor soundProcessor = new CExperimentalSoundProcessor();

        
        // collected song data
        final TFloatArrayList[] manyTimes = new TFloatArrayList[4];
        final TFloatArrayList[] fewTimes = new TFloatArrayList[4];
        soundProcessor.ProcessMusic(minimLib, filename, songLengthLimitSeconds, manyTimes, fewTimes);

        float BPM = 0f, startTime = 0f, timePerBeat = 0f;
        if (USETAPPER) {
            BPMOffset bpmOffset = UserActions.getTappedBPM(filename.getAbsolutePath());
            BPM = bpmOffset.BPM();
            timePerBeat = 60f / BPM;
            startTime = bpmOffset.offset();
        } else if (UPDATESM) {
            File smfile = SMGenerator.getSMFile(filename, outputDir);
            if (smfile.exists()) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(smfile));
                    while (br.ready() && (BPM == 0f || startTime == 0f)) {
                        String line = br.readLine();
                        if (line.contains("#OFFSET:")) {
                            int off = line.indexOf("#OFFSET:") + 8;
                            int end = line.indexOf(";", off);
                            startTime = Float.parseFloat(line.substring(off, end));
                            System.out.println("StartTime from SM file: " + startTime);
                        }
                        if (line.contains("#BPMS:")) {
                            int off = line.indexOf("#BPMS:");
                            off = line.indexOf("=", off) + 1;
                            int end = line.indexOf(";", off);
                            BPM = Float.parseFloat(line.substring(off, end));
                            System.out.println("BPM from SM file: " + BPM);
                        }
                    }
                    timePerBeat = 60f / BPM;
                } catch (Exception e) {
                }
            } else {
                System.out.println("Couldn't find SM to update: " + smfile.getAbsolutePath());
            }
        }
        if (BPM == 0f) {
            BPM = soundProcessor.GetBpm();
            timePerBeat = 60f / BPM;
            TFloatArrayList startTimes = new TFloatArrayList();
            for (int i = 0; i < fewTimes.length; i++) {
                startTimes.add(getBestOffset(timePerBeat, fewTimes[i], 0.01f));
                startTimes.add(getBestOffset(timePerBeat, manyTimes[i], 0.01f));
            }
            // give extra weight to fewKicks
            float kickStartTime = getBestOffset(timePerBeat, fewTimes[SoundParameter.KICKS.value()], 0.01f);
            startTimes.add(kickStartTime);
            startTimes.add(kickStartTime);
            startTime = -Averages.getMostCommonPhr00t(startTimes, 0.02f, false);
        }
        System.out.println("Time per beat: " + timePerBeat + ", BPM: " + BPM);
        System.out.println("Start Time: " + startTime);

        // start making the SM
        BufferedWriter smfile = SMGenerator.GenerateSM(BPM, startTime, filename, outputDir);

        StepGenerator newStepGenerator = new StepGenerator();

        TFloatArrayList MidFFTMaxes = soundProcessor.GetMidFFTMaxes();
        TFloatArrayList MidFFTAmount = soundProcessor.GetMidFFTAmount();
        TFloatArrayList volume = soundProcessor.getVolume();
        float timePerSample = soundProcessor.timePerSample();

        // SMGenerator.AddNotes(smfile, SMGenerator.Beginner,
        // StepGenerator.GenerateNotes(1, HARDMODE ? 2 : 4, manyTimes, fewTimes,
        // MidFFTAmount, MidFFTMaxes, timePerSample, timePerBeat, startTime, seconds,
        // false));
        // SMGenerator.AddNotes(smfile, SMGenerator.Easy, StepGenerator.GenerateNotes(1,
        // HARDMODE ? 1 : 2, manyTimes, fewTimes, MidFFTAmount, MidFFTMaxes,
        // timePerSample, timePerBeat, startTime, seconds, false));
        // SMGenerator.AddNotes(smfile, SMGenerator.Medium,
        // StepGenerator.GenerateNotes(2, HARDMODE ? 4 : 6, manyTimes, fewTimes,
        // MidFFTAmount, MidFFTMaxes, timePerSample, timePerBeat, startTime, seconds,
        // false));

        // String originalNotes = OgStepGenerator.GenerateNotes(2, 2, manyTimes, fewTimes, MidFFTAmount,
        //         MidFFTMaxes, timePerSample, timePerBeat, startTime, songLengthLimitSeconds, false, volume);
        String newNotes = newStepGenerator.GenerateNotes(filename.getAbsolutePath(), SimfileDifficulty.HARD, 4,
                fewTimes, MidFFTAmount, MidFFTMaxes, timePerSample, timePerBeat, startTime, songLengthLimitSeconds, false, volume);

        SMGenerator.AddNotes(smfile, SMGenerator.Hard, newNotes);
        // SMGenerator.AddNotes(smfile, SMGenerator.Hard,
        // OgStepGenerator.GenerateNotes(2, HARDMODE ? 2 : 4, manyTimes, fewTimes,
        // MidFFTAmount, MidFFTMaxes, timePerSample, timePerBeat, startTime, seconds,
        // false));
        // SMGenerator.AddNotes(smfile, SMGenerator.Challenge,
        // StepGenerator.GenerateNotes(2, HARDMODE ? 1 : 2, manyTimes, fewTimes,
        // MidFFTAmount, MidFFTMaxes, timePerSample, timePerBeat, startTime, seconds,
        // true));
        SMGenerator.Complete(smfile);

        System.out.println("[--------- SUCCESS ----------]");
    }
}
