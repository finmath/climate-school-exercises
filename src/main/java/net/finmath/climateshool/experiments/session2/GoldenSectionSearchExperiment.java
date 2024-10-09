package net.finmath.climateshool.experiments.session2;

import java.util.function.DoubleUnaryOperator;

import net.finmath.optimizer.GoldenSectionSearch;

/**
 * Small demonstration of how to use the GoldenSectionSearch minimizer.
 * 
 * @author Chrisitan Fries
 */
public class GoldenSectionSearchExperiment {

	public static void main(String[] args) {

		double x0 = 2.0;
		
		// Has a minimum in x0
		DoubleUnaryOperator givenObjectiveFunction = x -> (x-x0)*(x-x0);
		
		// Search interval
		double lowerBound = -1;
		double upperBound = 10.0;
		
		// Create optimizer to search this interval
		GoldenSectionSearch optimizer = new GoldenSectionSearch(lowerBound, upperBound);
		while(optimizer.getAccuracy() > 1E-5 && !optimizer.isDone()) {

			final double argumentTry = optimizer.getNextPoint();	// Free parameter

			// Apply our function
			final double value = givenObjectiveFunction.applyAsDouble(argumentTry);

			// Set the value
			optimizer.setValue(value);
			
			System.out.println(String.format("x = %5.2f \t y = %10.3f", argumentTry, value));
		}
		
		// Get optimal value
		final double argumentBestPoint = optimizer.getBestPoint();

		System.out.println("Optimizer found minimum in....: " + argumentBestPoint);
		System.out.println("Analytic solution.............: " + x0);
	}
}
