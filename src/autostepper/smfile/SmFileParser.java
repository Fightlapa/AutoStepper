package autostepper.smfile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import autostepper.moveassigners.CStepAssigner;

public class SmFileParser
{
    public static ArrayList<Integer> parseFile(String filePath) {
        ArrayList<ArrayList<Character>> allArrowLines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                ArrayList<Character> arrowLine = new ArrayList<>();
                for (char c : line.toCharArray()) {
                    arrowLine.add(c);
                }
                allArrowLines.add(arrowLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return parseLines(allArrowLines);
    }

    public static ArrayList<Integer> parseLines(ArrayList<ArrayList<Character>> arrowLines)
    {
        ArrayList<Integer> result = new ArrayList<>();
        boolean startCounting = false;
        for (ArrayList<Character> arrayList : arrowLines)
        {                        
            StringBuilder sb = new StringBuilder();
            for (Character v : arrayList) {
                sb.append(v);
            }
            String line = sb.toString();

            // Only process lines of length 4
            if (line.length() == 4) {
                if (!startCounting) {
                    // Skip lines until first non "0000"
                    if (!line.equals("0000")) {
                        startCounting = true;
                        // Count occurrences in this first non-"0000" line
                        result.add(countOnesOrTwos(line));
                    }
                } else {
                    // Count occurrences in subsequent lines
                    result.add(countOnesOrTwos(line));
                }
            }
        }

        return result;
    }

    private static int countOnesOrTwos(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == '1' || c == '2') count++;
        }
        return count;
    }

    public static String EncodeArrowLines(ArrayList<ArrayList<Character>> AllarrowLines, int stepGranularity)
    {
        // ok, put together AllNotes
        int commaSeperatorReset = 4 * stepGranularity;
        String AllNotes = "";
        int commaSeperator = commaSeperatorReset;
        for (ArrayList<Character> arrayList : AllarrowLines) {
            String result = arrayList.stream()
                .map(c -> c == 'W' ? '0' : c) // to replace custom "HOLDING" to empty
                .map(String::valueOf)
                .collect(Collectors.joining());
            AllNotes += result + "\n";
            commaSeperator--;
            if( commaSeperator == 0 ) {
                AllNotes += ",\n";
                commaSeperator = commaSeperatorReset;
            }
        }
        // fill out the last empties
        while( commaSeperator > 0 ) {
            AllNotes += "3333";
            commaSeperator--;
            if( commaSeperator > 0 ) AllNotes += "\n";
        }
        int _stepCount = AllNotes.length() - AllNotes.replace(Character.toString(CStepAssigner.STEP), "").length();
        int _holdCount = AllNotes.length() - AllNotes.replace(Character.toString(CStepAssigner.HOLD), "").length();
        int _mineCount = AllNotes.length() - AllNotes.replace(Character.toString(CStepAssigner.MINE), "").length();
        System.out.println("New algorithm. Steps: " + _stepCount + ", Holds: " + _holdCount);
        return AllNotes;
    }
}
