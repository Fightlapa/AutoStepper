package autostepper.genetic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

import autostepper.AutoStepper;
import autostepper.StepGenerator;
import autostepper.misc.Averages;
import autostepper.misc.Utils;
import autostepper.moveassigners.SimfileDifficulty;
import autostepper.smfile.SmFileParser;
import autostepper.soundprocessing.CExperimentalSoundProcessor;
import autostepper.soundprocessing.ISoundProcessor;
import autostepper.vibejudges.SoundParameter;
import gnu.trove.list.array.TFloatArrayList;

public class GeneticOptimizer {

    // --- Configuration ---
    static final int POPULATION_SIZE = 3;
    static final int NUM_GENERATIONS = 3;
    static final float MUTATION_RATE = 0.1f;

    static Random random = new Random();

    int numberOfParams;
    int stepGranularity;

    public TFloatArrayList optimize(int stepGranularity, TFloatArrayList startingPoint)
    {
        float[] chromosome = startingPoint.toArray();
        numberOfParams = startingPoint.size();
        this.stepGranularity = stepGranularity;

        // Initialize population
        float[][] population = new float[POPULATION_SIZE][numberOfParams];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = Arrays.copyOf(chromosome, chromosome.length);
        }

        float[] bestIndividual = null;
        int bestScore = Integer.MIN_VALUE;

        // Run generations
        for (int gen = 0; gen < NUM_GENERATIONS; gen++) {
            int[] scores = new int[POPULATION_SIZE];
            for (int i = 0; i < POPULATION_SIZE; i++) {
                scores[i] = 0;
            }
            // Selection & Crossover
            float[][] newPopulation = new float[POPULATION_SIZE][numberOfParams];
            for (int i = 0; i < POPULATION_SIZE; i++) {
                float[] parent1 = select(population, scores);
                float[] parent2 = select(population, scores);
                newPopulation[i] = crossover(parent1, parent2);
                mutate(newPopulation[i], false);
            }
            population = newPopulation;

            // Evaluate fitness
            for (int i = 0; i < POPULATION_SIZE; i++) {
                scores[i] = fitness(population[i]);
            }

            // Find the best in this generation
            for (int i = 0; i < POPULATION_SIZE; i++) {
                if (scores[i] > bestScore) {
                    bestScore = scores[i];
                    bestIndividual = Arrays.copyOf(population[i], numberOfParams);
                }
            }

            System.out.println("Generation " + gen + " Best score: " + bestScore);
        }

        System.out.println("Best individual: " + Arrays.toString(bestIndividual));
        System.out.println("Best score: " + bestScore);
        return new TFloatArrayList(bestIndividual);
    }

    // --- GA Components ---
    int fitness(float[] chromosome) {
        ArrayList<Integer> abracadabraReference = SmFileParser.parseFile("Target/AbracadabraOut.sm");
        ArrayList<Integer> cheapThrillsReference = SmFileParser.parseFile("Target/CheapThrillsOut.sm");

        ArrayList<Integer> abracadabraResult = getSongFingerprint("samples/Abracadabra.mp3", chromosome);
        ArrayList<Integer> cheapResult = getSongFingerprint("samples/CheapThrills.mp3", chromosome);
        // float cheapDuration = Utils.getSongTime("samples/CheapThrills.mp3");
        // float cheapBPM = soundProcessor.ProcessMusic(AutoStepper.minimLib, "samples/CheapThrills.mp3", cheapDuration, manyTimes, fewTimes);
        // ArrayList<ArrayList<Character>> abracadabraResult = newStepGenerator.GenerateNotes("samples/CheapThrills.mp3", SimfileDifficulty.HARD, stepGranularity,
        //     fewTimes, MidFFTAmount, MidFFTMaxes, timePerSample, timePerBeat, startTime, cheapDuration, false, volume);
        int numberOfDifferencesAbracadabra = -calculateDifferences(abracadabraReference, abracadabraResult);
        int numberOfDifferencesCheap = -calculateDifferences(cheapThrillsReference, cheapResult);

        return numberOfDifferencesAbracadabra + numberOfDifferencesCheap;
    }

    private int calculateDifferences(ArrayList<Integer> abracadabraReference, ArrayList<Integer> abracadabraResult) {
        int indexRef = 0;
        int indexResult = 0;
        int totalDiffs = 0;
        boolean anyFound = false;
        while (!anyFound)
        {
            if (abracadabraReference.get(indexRef) != 0)
                anyFound = true;
        }
        anyFound = false;
        while (!anyFound && indexResult < abracadabraResult.size())
        {
            if (abracadabraResult.get(indexResult) != 0)
                anyFound = true;
        }

        if (anyFound == false)
        {
            return 999999; // There was not even single tap
        }

        int groupedIndex = 0;
        while ((groupedIndex + indexRef) < abracadabraReference.size() && (groupedIndex + indexResult) < abracadabraResult.size())
        {
            totalDiffs += Math.abs(abracadabraReference.get(groupedIndex + indexRef) - abracadabraResult.get(groupedIndex + indexResult));
            indexRef++;
            indexResult++;
        }
        return totalDiffs;
    }

    private ArrayList<Integer> getSongFingerprint(String inputFileName, float[] chromosome)
    {
        StepGenerator stepGenerator = new StepGenerator();
        ArrayList<ArrayList<Character>> abracadabraArrows = stepGenerator.GenerateNotes(inputFileName, SimfileDifficulty.HARD, stepGranularity, false, new TFloatArrayList(chromosome));
        return SmFileParser.parseLines(abracadabraArrows);
    }

    float[] select(float[][] population, int[] scores) {
        // Roulette wheel selection
        float sum = 0;
        for (int s : scores) sum += s;
        float r = random.nextFloat() * sum;
        int accum = 0;
        for (int i = 0; i < population.length; i++) {
            accum += scores[i];
            if (accum >= r) return population[i];
        }
        return population[population.length - 1];
    }

    float[] crossover(float[] parent1, float[] parent2) {
        float[] child = new float[numberOfParams];
        for (int i = 0; i < numberOfParams; i++) {
            child[i] = random.nextBoolean() ? parent1[i] : parent2[i];
        }
        return child;
    }

    void 
    
    mutate(float[] chromosome, boolean force) {
        for (int i = 0; i < numberOfParams; i++) {
            boolean mutatedProperly = false;
            if ((random.nextFloat() < MUTATION_RATE || force) && mutatedProperly)
            {
                float value = mutateSingleGene(chromosome, i);
                // Guards for frequency params to avoid lower freq to be higher than high freq
                // Could be done by randomizing pairs, TODO
                if (AlgorithmParameter.fromValue(i) == AlgorithmParameter.KICK_LOW_FREQ)
                {
                    if (chromosome[AlgorithmParameter.KICK_HIGH_FREQ.value()] < value)
                    {
                        // higher has to be higher than low...
                    }
                }
                else if (AlgorithmParameter.fromValue(i) == AlgorithmParameter.KICK_HIGH_FREQ)
                {
                    if (chromosome[AlgorithmParameter.KICK_LOW_FREQ.value()] > value)
                    {
                        // higher has to be higher than low...
                    }
                }
                else if (AlgorithmParameter.fromValue(i) == AlgorithmParameter.SNARE_LOW_FREQ)
                {
                    if (chromosome[AlgorithmParameter.SNARE_HIGH_FREQ.value()] < value)
                    {
                        // higher has to be higher than low...
                    }
                }
                else if (AlgorithmParameter.fromValue(i) == AlgorithmParameter.SNARE_HIGH_FREQ)
                {
                    if (chromosome[AlgorithmParameter.SNARE_LOW_FREQ.value()] > value)
                    {
                        // higher has to be higher than low...
                    }
                }
                else
                {
                    mutatedProperly = true;
                }
            }
        }
    }

    private float mutateSingleGene(float[] chromosome, int i) {
        Optional<Integer> maxValInt = AlgorithmParameter.maxValueForIntParameter(AlgorithmParameter.fromValue(i));
        Optional<Float> maxValFloat = AlgorithmParameter.maxValueForFloatParameter(AlgorithmParameter.fromValue(i));
        float newValue;
        if (maxValInt.isPresent())
        {
            Optional<Integer> minValInt = AlgorithmParameter.minValueForIntParameter(AlgorithmParameter.fromValue(i));
            newValue = (float)(minValInt.get() + random.nextInt(maxValInt.get() + 1 - minValInt.get()));
        }
        else
        {
            Optional<Float> minValFloat = AlgorithmParameter.minValueForFloatParameter(AlgorithmParameter.fromValue(i));
            newValue = minValFloat.get() + random.nextFloat() * (maxValFloat.get() - minValFloat.get());
        }
        return newValue;
    }

    // --- Replace with your actual scoring function ---
    float yourScoringFunction(float[] params) {
        // Example: max score if sum of params is close to 5
        float sum = 0;
        for (float p : params) sum += p;
        return 1.0f - Math.min(Math.abs(5 - sum) / 5.0f, 1.0f);
    }
}
