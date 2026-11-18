package com.zarterstein.magid.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Stats}: compensated sums and what is built on them.
 *
 * @author Mark-Marion Gondelman (Orchestration)
 * @author Code: Claude Code (agentic implementation)
 */
class StatsTest {

    private static double[] tenth(int count) {
        double[] values = new double[count];
        Arrays.fill(values, 0.1);
        return values;
    }

    @Test
    @DisplayName("The sum keeps a small value between two huge ones: 1e100 + 1 - 1e100 is 1")
    void sumCompensates() {
        assertEquals(1, Stats.sum(new double[]{1e100, 1, -1e100}));
    }

    @Test
    @DisplayName("Ten times 0.1 sums to exactly 1 (a plain loop gives 0.9999999999999999)")
    void tenTenths() {
        assertEquals(1.0, Stats.sum(tenth(10)));
    }

    @Test
    @DisplayName("The mean of ten 0.1 is 0.1")
    void meanOfTenths() {
        assertEquals(0.1, Stats.mean(tenth(10)));
    }

    @Test
    @DisplayName("The sum of nothing is 0, the product 1")
    void emptySumAndProduct() {
        assertEquals(0, Stats.sum(new double[0]));
        assertEquals(1, Stats.product(new double[0]));
    }

    @Test
    @DisplayName("The mean of nothing is an error")
    void emptyMean() {
        assertThrows(MathException.class, () -> Stats.mean(new double[0]));
    }

    @Test
    @DisplayName("The median of an odd count is the middle value")
    void medianOdd() {
        assertEquals(3, Stats.median(new double[]{5, 1, 3}));
    }

    @Test
    @DisplayName("The median of an even count is the mean of the two middle values")
    void medianEven() {
        assertEquals(2.5, Stats.median(new double[]{4, 1, 3, 2}));
    }

    @Test
    @DisplayName("The median of two huge values doesn't overflow")
    void medianHuge() {
        assertEquals(Double.MAX_VALUE, Stats.median(new double[]{Double.MAX_VALUE, Double.MAX_VALUE}));
    }

    @Test
    @DisplayName("Population variance of 2,4,4,4,5,5,7,9 is 4")
    void populationVariance() {
        assertEquals(4, Stats.variance(new double[]{2, 4, 4, 4, 5, 5, 7, 9}, false));
    }

    @Test
    @DisplayName("Sample variance of 1,2,3,4 is 5/3")
    void sampleVariance() {
        assertEquals(5.0 / 3, Stats.variance(new double[]{1, 2, 3, 4}, true));
    }

    @Test
    @DisplayName("The variance of equal values is exactly 0, even for values that don't add up exactly")
    void varianceOfEqualValues() {
        assertEquals(0, Stats.variance(tenth(7), false));
    }

    @Test
    @DisplayName("The sample variance of one value is an error")
    void sampleVarianceOfOne() {
        assertThrows(MathException.class, () -> Stats.variance(new double[]{1}, true));
    }

    @Test
    @DisplayName("min and max")
    void minMax() {
        assertEquals(-2, Stats.min(new double[]{3, -2, 7}));
        assertEquals(7, Stats.max(new double[]{3, -2, 7}));
    }
}
