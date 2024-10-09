package net.finmath.climateshool.other;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

public class ConvexityExperiments {

	public static void main(String[] args) {

		double mu = 2.0;
		double sigma = 0.01;
		
		long numberOfSamples = 1000;
		List<Double> randomNumbers = getRandomNumbers(numberOfSamples, mu, sigma);

		// 
		double mean = getMean(randomNumbers);

		double variance = getVariance(randomNumbers);

		double standardDeviation = Math.sqrt(variance);
		
		
		System.out.println("mean of the samples X.... = " + mean);
		System.out.println("variance of the samples X = " + variance);

		/*
		 * Explore a linear transformation
		 */
		Function<Double,Double> functionLinear = x -> 6 * x - 4;
		
		List<Double> transformed = randomNumbers.stream().map(functionLinear).toList();
		
		double transformedMean = getMean(transformed);

		double transformedVariance = getVariance(transformed);

		System.out.println("mean of the samples X.... = " + transformedMean);
		System.out.println("variance of the samples X = " + transformedVariance);

		/*
		 * Explore a non-linear transformation
		 */
		Function<Double,Double> functionNonLinear = x -> x*x*x;
		
		List<Double> transformedNonLinear = randomNumbers.stream().map(functionNonLinear).toList();
		
		double transformedNonLinearMean = getMean(transformedNonLinear);

		double transformedNonLinearVariance = getVariance(transformedNonLinear);

		System.out.println("mean of the samples X.... = " + transformedNonLinearMean);
		System.out.println("variance of the samples X = " + transformedNonLinearVariance);
	}

	
	private static double getVariance(List<Double> randomNumbers) {
		double mean = getMean(randomNumbers);

		double sumSquared = 0.0;
		for(int i=0; i<randomNumbers.size(); i++) {
			sumSquared += (randomNumbers.get(i) - mean) * (randomNumbers.get(i) - mean);
		}
		double variance = sumSquared / randomNumbers.size();
		
		return variance;
	}


	private static double getMean(List<Double> randomNumbers) {
		double sum = 0.0;
		for(int i=0; i<randomNumbers.size(); i++) {
			sum += randomNumbers.get(i);
		}
		double mean = sum / randomNumbers.size();
		
		return mean;
	}




	public static double ourLinearFunction(float x) {
		
		float y = 6 * x + 4;
		
		return y;
	}

	/**
	 * Helper function that generates uniform distributed random numbers.
	 * 
	 * 
	 * @param numberOfSamples
	 * @param mu The desired mean
	 * @param sigma The desired standard deviation
	 * @return
	 */
	private static List<Double> getRandomNumbers(long numberOfSamples, double mu, double sigma) {
		long seed = 3141;
		Random random = new Random(seed);		
		List<Double> randomNumbers = random.doubles().limit(numberOfSamples ).map(x -> mu + sigma * Math.sqrt(12) * (x-0.5)).boxed().toList();
		return randomNumbers;
	}
}
