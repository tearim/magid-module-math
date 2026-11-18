package com.zarterstein.magid.math;

import java.util.Arrays;

/**
 * The aggregates: sums with Neumaier's compensation, so a long list of values doesn't drift
 * ({@code sum(1e100, 1, -1e100)} is {@code 1}, not {@code 0}); means, medians, variances and standard deviations built
 * on them.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
final class Stats {

    private Stats() {
    }

    /**
     * @param values the values
     * @return their sum, compensated (Neumaier); {@code 0} for none
     */
    static double sum(double[] values) {
        double sum = 0;
        double compensation = 0;
        for (double value : values) {
            double next = sum + value;
            if (Math.abs(sum) >= Math.abs(value)) {
                compensation += (sum - next) + value;
            } else {
                compensation += (value - next) + sum;
            }
            sum = next;
        }
        return sum + compensation;
    }

    /**
     * @param values the values
     * @return their product; {@code 1} for none
     */
    static double product(double[] values) {
        double product = 1;
        for (double value : values) product *= value;
        return product;
    }

    /**
     * @param values at least one value
     * @return the arithmetic mean: the compensated sum divided by the count
     */
    static double mean(double[] values) {
        requireAtLeast(values, 1, "mean");
        return sum(values) / values.length;
    }

    /**
     * @param values at least one value
     * @return the middle value, or the mean of the two middle values
     */
    static double median(double[] values) {
        requireAtLeast(values, 1, "median");
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        if (sorted.length % 2 == 1) return sorted[middle];
        double low = sorted[middle - 1];
        double high = sorted[middle];
        double half = (low + high) / 2;   // exact halving; overflows only for huge values
        return Double.isFinite(half) ? half : low / 2 + high / 2;
    }

    /**
     * @param values at least one value
     * @return the smallest
     */
    static double min(double[] values) {
        requireAtLeast(values, 1, "min");
        double min = values[0];
        for (double value : values) min = Math.min(min, value);
        return min;
    }

    /**
     * @param values at least one value
     * @return the largest
     */
    static double max(double[] values) {
        requireAtLeast(values, 1, "max");
        double max = values[0];
        for (double value : values) max = Math.max(max, value);
        return max;
    }

    /**
     * @param values the values
     * @param sample {@code true} for the sample variance (divided by n - 1), {@code false} for the population
     *               variance (divided by n)
     * @return the variance, from the compensated two-pass formula
     */
    static double variance(double[] values, boolean sample) {
        requireAtLeast(values, sample ? 2 : 1, sample ? "vars" : "varp");
        double mean = mean(values);
        double[] deviations = new double[values.length];
        double[] squares = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            deviations[i] = values[i] - mean;
            squares[i] = deviations[i] * deviations[i];
        }
        double drift = sum(deviations);   // zero in exact arithmetic; corrects the rounding of the mean
        double squared = sum(squares) - drift * drift / values.length;
        return Math.max(0, squared) / (sample ? values.length - 1 : values.length);
    }

    private static void requireAtLeast(double[] values, int count, String name) {
        if (values.length < count) {
            throw new MathException(name + " needs at least " + count + (count == 1 ? " value" : " values")
                    + ", got " + values.length);
        }
    }
}
