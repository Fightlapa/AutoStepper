package autostepper.misc;

import java.util.ArrayList;

import gnu.trove.list.array.TFloatArrayList;

public class Averages {
    public static float getMostCommonPhr00t(TFloatArrayList inputArray, float threshold, boolean closestToInteger)
    {
        // Goes through input array
        // then splits it into multiple lists
        // It attempts to add to each list if diff with ANY element from intermediate list
        // is lower than threshold
        ArrayList<TFloatArrayList> intermediateValues = new ArrayList<>();
        for (int i = 0; i < inputArray.size(); i++) {
            float inputElement = inputArray.get(i);
            // check for this value in our current lists
            boolean notFound = true;
            for (int j = 0; j < intermediateValues.size(); j++)
            {
                TFloatArrayList tal = intermediateValues.get(j);
                for (int k = 0; k < tal.size(); k++) {
                    float listValue = tal.get(k);
                    if (Math.abs(listValue - inputElement) < threshold) {
                        notFound = false;
                        tal.add(inputElement);
                        break;
                    }
                }
                if (notFound == false)
                    break;
            }
            // if it wasn't found, start a new list
            if (notFound) {
                TFloatArrayList newList = new TFloatArrayList();
                newList.add(inputElement);
                intermediateValues.add(newList);
            }
        }
        // get the longest list
        int longest = 0;
        TFloatArrayList longestList = null;
        for (int i = 0; i < intermediateValues.size(); i++) {
            TFloatArrayList check = intermediateValues.get(i);
            if (check.size() > longest ||
                    check.size() == longest && Utils.getDifferenceAverage(check) < Utils.getDifferenceAverage(longestList)) {
                longest = check.size();
                longestList = check;
            }
        }
        if (longestList == null)
            return -1f;
        if (longestList.size() == 1 && intermediateValues.size() > 1) {
            // one value only, no average needed.. but what to pick?
            // just pick the smallest one... or integer, if we want that instead
            if (closestToInteger) {
                float closestIntDiff = 1f;
                float result = inputArray.getQuick(0);
                for (int i = 0; i < inputArray.size(); i++) {
                    float diff = Math.abs(Math.round(inputArray.getQuick(i)) - inputArray.getQuick(i));
                    if (diff < closestIntDiff) {
                        closestIntDiff = diff;
                        result = inputArray.getQuick(i);
                    }
                }
                return result;
            } else {
                float smallest = 99999f;
                for (int i = 0; i < inputArray.size(); i++) {
                    if (inputArray.getQuick(i) < smallest)
                        smallest = inputArray.getQuick(i);
                }
                return smallest;
            }
        }
        // calculate average
        float avg = 0f;
        for (int i = 0; i < longestList.size(); i++) {
            avg += longestList.get(i);
        }
        return avg / longestList.size();
    }

    public static float getMostCommonFightlapa(TFloatArrayList inputArray, float threshold, boolean closestToInteger)
    {
        TFloatArrayList inputCopy = new TFloatArrayList(inputArray);
        int inputSize = inputCopy.size();
        inputCopy.sort();

        int itemsToTrim = Math.round(inputSize * 0.1f); // 10%
        for (int i = 0; i < itemsToTrim; i++) {
            inputCopy.removeAt(inputCopy.size() - 1);
        }

        for (int i = 0; i < itemsToTrim; i++) {
            inputCopy.removeAt(0);
        }
        return inputCopy.get(inputCopy.size() / 2);
    }
}
