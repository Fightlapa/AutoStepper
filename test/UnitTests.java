import org.junit.jupiter.api.Test;

import autostepper.misc.Averages;
import autostepper.misc.Utils;
import gnu.trove.list.array.TFloatArrayList;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;

class UnitTests {

    TFloatArrayList inputArray = new TFloatArrayList();
    float average;
    float median;

    @BeforeEach
    void setUp()
    {
        inputArray.add(2.61f);
        inputArray.add(7.08f);
        inputArray.add(3.08f);
        inputArray.add(3.37f);
        inputArray.add(5.33f);
        inputArray.add(6.65f);
        inputArray.add(7.29f);
        inputArray.add(9.67f);
        inputArray.add(1.21f);
        inputArray.add(8.45f);
        inputArray.add(4.89f);
        inputArray.add(5.26f);
        inputArray.add(8.12f);
        inputArray.add(1.24f);
        inputArray.add(5.65f);
        inputArray.add(0.34f);
        inputArray.add(8.03f);
        inputArray.add(8.89f);
        inputArray.add(6.05f);
        inputArray.add(7.51f);
        inputArray.add(2.42f);
        inputArray.add(6.78f);
        inputArray.add(7.84f);
        inputArray.add(1.4f);
        inputArray.add(3.98f);
        inputArray.add(0.77f);
        inputArray.add(6.01f);
        inputArray.add(7.87f);
        inputArray.add(4.9f);
        inputArray.add(4.53f);
        inputArray.add(1.36f);
        inputArray.add(8.08f);
        inputArray.add(5.4f);
        inputArray.add(9.92f);
        inputArray.add(5.44f);
        inputArray.add(4.04f);
        inputArray.add(3.22f);
        inputArray.add(7.82f);
        inputArray.add(3.92f);
        inputArray.add(5.6f);
        inputArray.add(4.1f);
        inputArray.add(5.15f);
        inputArray.add(7.45f);
        inputArray.add(8.55f);
        inputArray.add(0.1f);
        inputArray.add(7.98f);
        inputArray.add(1.58f);
        inputArray.add(7.9f);
        inputArray.add(8.3f);
        inputArray.add(0.92f);
        inputArray.add(2.08f);
        inputArray.add(0.8f);
        inputArray.add(8.54f);
        inputArray.add(3.27f);
        inputArray.add(7.19f);
        inputArray.add(8.65f);
        inputArray.add(9.89f);
        inputArray.add(6.36f);
        inputArray.add(2.43f);
        inputArray.add(9.3f);
        inputArray.add(9.53f);
        inputArray.add(1.03f);
        inputArray.add(3.21f);
        inputArray.add(9.13f);
        inputArray.add(2.27f);
        inputArray.add(2.17f);
        inputArray.add(6.96f);
        inputArray.add(3.86f);
        inputArray.add(0.47f);
        inputArray.add(1.46f);
        inputArray.add(3.55f);
        inputArray.add(5.44f);
        inputArray.add(1.03f);
        inputArray.add(3.12f);
        inputArray.add(9.45f);
        inputArray.add(5.71f);
        inputArray.add(6.2f);
        inputArray.add(9.59f);
        inputArray.add(1.08f);
        inputArray.add(6.0f);
        inputArray.add(4.25f);
        inputArray.add(6.71f);
        inputArray.add(1.28f);
        inputArray.add(3.04f);
        inputArray.add(3.47f);
        inputArray.add(8.68f);
        inputArray.add(5.03f);
        inputArray.add(7.39f);
        inputArray.add(0.13f);
        inputArray.add(1.13f);
        inputArray.add(0.36f);
        inputArray.add(6.12f);
        inputArray.add(6.2f);
        inputArray.add(6.51f);
        inputArray.add(4.24f);
        inputArray.add(0.25f);
        inputArray.add(0.09f);
        inputArray.add(6.07f);
        inputArray.add(4.34f);
        inputArray.add(0.1f);

        TFloatArrayList copyForMedian = new TFloatArrayList(inputArray);
        copyForMedian.sort();
        median = copyForMedian.get(copyForMedian.size() / 2);
        average = inputArray.sum() / inputArray.size();
        System.out.println("Median: " + median + " Average: " + average);
    }

    @Test
    void checkOgAlgorithm() {
        float threshold = 0.5f;
        boolean closestToInteger = false;


        float value = Averages.getMostCommonPhr00t(inputArray, threshold, closestToInteger);
        assertEquals(7.511291f, value);
    }

    @Test
    void checkNewAlgorithm() {
        float threshold = 0.5f;
        boolean closestToInteger = false;


        float value = Averages.getMostCommonFightlapa(inputArray, threshold, closestToInteger);
        assertEquals(5.26f, value);
    }
}
