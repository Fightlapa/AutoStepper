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
import autostepper.soundprocessing.Song;
import autostepper.vibejudges.SoundParameter;
import gnu.trove.list.array.TFloatArrayList;

public class GeneticOptimizer {

    // --- Configuration ---
    static final int POPULATION_SIZE = 8;
    static final int NUM_GENERATIONS = 20;
    static final float MUTATION_RATE = 0.15f;

    static Random random = new Random();

    int numberOfParams;
    int stepGranularity;
    private ArrayList<Integer> song1Reference;
    private ArrayList<Integer> song2Reference;
    private Song song1;
    private Song song2;

    public GeneticOptimizer() {
        song1Reference = SmFileParser.parseFile("Target/AbracadabraOut.sm");
        song2Reference = SmFileParser.parseFile("Target/edamameOut.sm");

        song1 = new Song("samples/Abracadabra.mp3");
        song2 = new Song("samples/edamame.mp3");
    }

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

        ArrayList<float[]> bestIndividuals = new ArrayList<>();
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
                System.out.println("Jump\tTap\tsust\tsusf\tgran\tpgran\tvol1\tvol2\tfftmax\tkicklow\tkickhig\tkickb\tsnarlow\tsnarhig\tsnarb\thatlow\thathig\thatb");
                for (int j = 0; j < population[i].length; j++) {
                    System.out.print(String.format("%.3f", population[i][j]) + "\t");
                }
                System.out.println();
            }

            // Find the best in this generation
            for (int i = 0; i < POPULATION_SIZE; i++) {
                if (scores[i] > bestScore) {
                    bestScore = scores[i];
                    bestIndividuals.add(0, Arrays.copyOf(population[i], numberOfParams));
                    if (bestIndividuals.size() > 3)
                    {
                        bestIndividuals.remove(3);
                    }
                }
            }

            System.out.println("Generation " + gen + " Best score: " + bestScore + " , best score possible: " + (song1Reference.size() + song2Reference.size()));
        }

        System.out.println("Best individuals: ");
        for (float[] fs : bestIndividuals) {
            System.out.println("Best individuals: " + Arrays.toString(fs));
        }
        System.out.println("Best score: " + bestScore);
        return new TFloatArrayList(bestIndividuals.get(0));
    }

    // --- GA Components ---
    int fitness(float[] chromosome) {
        ArrayList<Integer> abracadabraResult = getSongFingerprint(song1, chromosome, 126f);
        ArrayList<Integer> cureResult = getSongFingerprint(song2, chromosome, 106f);

        int numberOfDifferencesSong1 = calculateDifferences(song1Reference, abracadabraResult);
        int numberOfDifferencesSong2 = calculateDifferences(song2Reference, cureResult);

        return abracadabraResult.size() + cureResult.size() - numberOfDifferencesSong1 - numberOfDifferencesSong2;
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

        while (indexRef < abracadabraReference.size() && indexResult < abracadabraResult.size())
        {
            totalDiffs += Math.abs(abracadabraReference.get(indexRef) - abracadabraResult.get(indexResult));
            indexRef++;
            indexResult++;
        }
        return totalDiffs;
    }

    private ArrayList<Integer> getSongFingerprint(Song song, float[] chromosome, float expectedBpm)
    {
        StepGenerator stepGenerator = new StepGenerator(new TFloatArrayList(chromosome));
        ArrayList<ArrayList<Character>> abracadabraArrows = stepGenerator.GenerateNotes(song, SimfileDifficulty.HARD, stepGranularity, false, new TFloatArrayList(chromosome), expectedBpm);
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
            if ((random.nextFloat() < MUTATION_RATE || force) && !mutatedProperly)
            {
                float value = mutateSingleGene(i);
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
                    chromosome[i] = value;
                }
            }
        }
    }

    static public float mutateSingleGene(int i) {
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
