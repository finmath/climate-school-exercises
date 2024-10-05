package net.finmath.climateschool.begin;

import java.util.ArrayList;
import java.util.List;

import net.finmath.functions.NormalDistribution;
import net.finmath.plots.Plots;
import net.finmath.randomnumbers.MersenneTwister;
import net.finmath.randomnumbers.RandomNumberGenerator1D;

/**
 * Test that generates samples of a normal distribution and shows its density.
 */
public class Test3 {

	private static final long seed = 3141;

	public static void main(String[] args) throws Exception {

		System.out.println("\n\tThis test should show two plots of the density of samples of a normal distribution.");
		
		long numberOfSamples = 1000000;
		plotNormalSamples(numberOfSamples, 0.0 /* mean */, 0.1 /* standard deviation */);
		plotNormalSamples(numberOfSamples, 0.0 /* mean */, 1.0 /* standard deviation */);
	}

	private static void plotNormalSamples(long numberOfSamples, double mean, double standardDeviation) throws Exception {
		
		List<Double> randomNumbersNormal = getNormalDistributedRandomNumbers(numberOfSamples, mean, standardDeviation);
		plotDensity(randomNumbersNormal, "Normal with mean " + mean + " and std. dev. " + standardDeviation);
	}

	private static List<Double> getNormalDistributedRandomNumbers(long numberOfSamples, double mean, double standardDeviation) {
		RandomNumberGenerator1D randomNumberGenerator = new MersenneTwister(seed);
		List<Double> valuesNormal = new ArrayList<>();
		for(int i = 0; i<numberOfSamples; i++) {
			double uniform = randomNumberGenerator.nextDouble();

			double standardNormal = NormalDistribution.inverseCumulativeDistribution(uniform);;

			double normal = standardDeviation * standardNormal + mean;

			valuesNormal.add(normal);
		}
		return valuesNormal;
	}

	private static void plotDensity(List<Double> values, String title) throws Exception {
		Plots.createDensity(values, 300, 4.0)
			.setTitle(title)
			.setXRange(-4, 4)
			.show();
	}
}
