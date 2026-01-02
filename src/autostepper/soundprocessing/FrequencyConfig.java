package autostepper.soundprocessing;

public class FrequencyConfig {
    int minimalFrequency;
    int maximumFrequency;
    int bandwithThreshold;

    public FrequencyConfig(int minimalFrequency,
        int maximumFrequency,
        int bandwithThreshold)
    {
        this.minimalFrequency = minimalFrequency;
        this.maximumFrequency = maximumFrequency;
        this.bandwithThreshold = bandwithThreshold;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + minimalFrequency;
        result = prime * result + maximumFrequency;
        result = prime * result + bandwithThreshold;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FrequencyConfig other = (FrequencyConfig) obj;
        if (minimalFrequency != other.minimalFrequency)
            return false;
        if (maximumFrequency != other.maximumFrequency)
            return false;
        if (bandwithThreshold != other.bandwithThreshold)
            return false;
        return true;
    }

    public int getMinimalFrequency() {
        return minimalFrequency;
    }

    public int getMaximumFrequency() {
        return maximumFrequency;
    }

    public int getBandwithThreshold() {
        return bandwithThreshold;
    }
    
}
