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

import autostepper.genetic.AlgorithmParameter;
import autostepper.genetic.GeneticOptimizer;
import autostepper.misc.Averages;
import autostepper.moveassigners.SimfileDifficulty;
import autostepper.smfile.SmFileParser;
import autostepper.soundprocessing.Song;

/**
 *
 * @author Phr00t
 */


// TODO:
// Parametrize rest - CHECK
// assign enum values - CHECK
// provide min/max for different params - CHECK
// calculate percentage diff between results and expected
// train
public class AutoStepper {

    public static boolean TRAIN = false;
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
        PREVIEW_DETECTION = getArg(args, "preview", "false").equals("true");
        TAPSYNC = Double.parseDouble(getArg(args, "tapsync", "-0.11"));
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

    void analyzeUsingAudioRecordingStream(File filename, float songLengthLimitSeconds, String outputDir) {

        // String originalNotes = OgStepGenerator.GenerateNotes(2, 2, manyTimes, fewTimes, MidFFTAmount,
        //         MidFFTMaxes, timePerSample, timePerBeat, startTime, songLengthLimitSeconds, false, volume);

        TFloatArrayList startingPoint = new TFloatArrayList();

        startingPoint.insert(AlgorithmParameter.JUMP_THRESHOLD.value(), 2.0f);
        startingPoint.insert(AlgorithmParameter.TAP_THRESHOLD.value(), 1.0f);
        startingPoint.insert(AlgorithmParameter.SUSTAIN_THESHOLD.value(), 1.0f);
        startingPoint.insert(AlgorithmParameter.SUSTAIN_FACTOR.value(), 0.2f);
        startingPoint.insert(AlgorithmParameter.GRANULARITY_MODIFIER.value(), 0.98f);
        startingPoint.insert(AlgorithmParameter.PRECISE_GRANULARITY_MODIFIER.value(), 0.5f);
        startingPoint.insert(AlgorithmParameter.FIRST_VOLUME_THRESHOLD.value(), 0.4f);
        startingPoint.insert(AlgorithmParameter.SECOND_VOLUME_THRESHOLD.value(), 0.8f);
        startingPoint.insert(AlgorithmParameter.FFT_MAX_THRESHOLD.value(), 0.8f);
        startingPoint.insert(AlgorithmParameter.KICK_LOW_FREQ.value(), 1f);
        startingPoint.insert(AlgorithmParameter.KICK_HIGH_FREQ.value(), 6f);
        startingPoint.insert(AlgorithmParameter.KICK_BAND_FREQ.value(), 2f);
        startingPoint.insert(AlgorithmParameter.SNARE_LOW_FREQ.value(), 8f);
        startingPoint.insert(AlgorithmParameter.SNARE_HIGH_FREQ.value(), 26f);
        startingPoint.insert(AlgorithmParameter.SNARE_BAND_FREQ.value(), 6f);
        assert(startingPoint.size() == (AlgorithmParameter.SNARE_BAND_FREQ.value() + 1));

        StepGenerator stepGenerator = new StepGenerator(startingPoint);
        Song song = new Song(filename.getAbsolutePath());
        String newNotes = "";
        if (TRAIN)
        {
            int STEP_GRANULARITY = 4;
            GeneticOptimizer geneticOptimizer = new GeneticOptimizer();
            TFloatArrayList optimalParameters = geneticOptimizer.optimize(STEP_GRANULARITY, startingPoint);
            ArrayList<ArrayList<Character>> result = stepGenerator.GenerateNotes(song, SimfileDifficulty.HARD, STEP_GRANULARITY,
                    false, optimalParameters);
            newNotes = SmFileParser.EncodeArrowLines(result, STEP_GRANULARITY);
        }
        else
        {
            long jazzMusicStarts = System.currentTimeMillis();
            int STEP_GRANULARITY = 2;
            ArrayList<ArrayList<Character>> result = stepGenerator.GenerateNotes(song, SimfileDifficulty.HARD, STEP_GRANULARITY,
                    false, startingPoint);
            newNotes = SmFileParser.EncodeArrowLines(result, STEP_GRANULARITY);
            System.out.println("Time elapsed: " + (System.currentTimeMillis() - jazzMusicStarts) / 1000f + "s");
        }

        float BPM = stepGenerator.getBPM();
        float startTime = stepGenerator.getStartTime();

        // start making the SM
        BufferedWriter smfile = SMGenerator.GenerateSM(BPM, startTime, filename, outputDir);
        SMGenerator.AddNotes(smfile, SMGenerator.Hard, newNotes);
        SMGenerator.Complete(smfile);

        System.out.println("[--------- SUCCESS ----------]");
    }
}
