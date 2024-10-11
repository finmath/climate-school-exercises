package net.finmath.climateschool.experiments.session2;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import net.finmath.climate.models.ClimateModel;
import net.finmath.climate.models.dice.DICEModel;
import net.finmath.optimizer.GoldenSectionSearch;
import net.finmath.plots.Plots;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Experiment related to the DICE model: How does the time to reach maximum abatement depend on the interest rate.
 * 
 * @author Christian Fries
 */
public class DICEModelOneParametricDependencyOnInterestRate {

	private static final double timeStep = 1.0;
	private static final double timeHorizon = 500.0;

	public static void main(String[] args) {

		List<Double> discountRates = new ArrayList<>();
		List<Double> timeToReachMaxAbatement = new ArrayList<>();
		for(double discountRate = 0.001; discountRate <= 0.04; discountRate += 0.001) {

			double maxAbatementTime = getTimeToReachMaxAbatement(discountRate);
			
			discountRates.add(discountRate);
			timeToReachMaxAbatement.add(maxAbatementTime);
			System.out.println(String.format("\t %8.4f \t %8.4f", discountRate, maxAbatementTime));
		}

		Plots
		.createScatter(discountRates, timeToReachMaxAbatement, 0, 300, 3)
		.setTitle("Time to Reach Maximium Abatement (T(\u03BC=1) in the One Parametric Model")
		.setXAxisLabel("rate (r)").setXAxisNumberFormat(new DecimalFormat("0.0%")).setYAxisLabel("T(\u03BC=1)").show();

		System.out.println("_".repeat(79));
	}

	private static double getTimeToReachMaxAbatement(double discountRate) {
		/*
		 * Parameters for the abatement model
		 */
		final double abatementInitial = 0.03;
		final double abatementMax = 1.00;

		/*
		 * Create a time discretization
		 */
		final int numberOfTimeSteps = (int)Math.round(timeHorizon / timeStep);
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, numberOfTimeSteps, timeStep);

		/*
		 * Create our savings rate model: a constant
		 */
		final UnaryOperator<Double> savingsRateFunction = time -> 0.26;

		GoldenSectionSearch optimizer = new GoldenSectionSearch(10.0, 300.0);
		while(optimizer.getAccuracy() > 1E-11 && !optimizer.isDone()) {

			final double abatementMaxTime = optimizer.getNextPoint();	// Free parameter

			/*
			 * Create our abatement model
			 */
			final UnaryOperator<Double> abatementFunction = time -> Math.min(abatementInitial + (abatementMax-abatementInitial)/abatementMaxTime * time, abatementMax);
			
			/*
			 * Create the DICE model
			 */
			final ClimateModel climateModel = new DICEModel(timeDiscretization, abatementFunction, savingsRateFunction, discountRate);

			final double value = climateModel.getValue().expectation().doubleValue();

			optimizer.setValue(-value);
		}
		
		// Get optimal value
		final double abatementMaxTime = optimizer.getBestPoint();
		
		return abatementMaxTime;
	}
	


}
