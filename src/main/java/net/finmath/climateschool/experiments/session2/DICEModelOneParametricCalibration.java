package net.finmath.climateschool.experiments.session2;

import java.util.Arrays;
import java.util.function.UnaryOperator;

import net.finmath.climate.models.CarbonConcentration;
import net.finmath.climate.models.ClimateModel;
import net.finmath.climate.models.Temperature;
import net.finmath.climate.models.dice.DICEModel;
import net.finmath.optimizer.GoldenSectionSearch;
import net.finmath.plots.Plots;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Experiment related to the DICE model.
 * 
 * Calibrates a one parametric abatement function for a model with given values of the discount rate.
 * 
 * Plots the optimal abatement function.
 * 
 * Suggestion: Change the discount rate to smaller values: 0.02, 0.01, ...
 * 
 * @author Christian Fries
 */
public class DICEModelOneParametricCalibration {

	private static final double timeStep = 1.0;
	private static final double timeHorizon = 500.0;

	public static void main(String[] args) {

		/*
		 * Discount rate
		 */
		final double discountRate = 0.03;

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

		/*
		 * Search for the optimal value of abatementMaxTime
		 */
		double searchIntervallLowerBound = 10.0;
		double searchIntervallUpperBound = 300.0;		
		GoldenSectionSearch optimizer = new GoldenSectionSearch(searchIntervallLowerBound, searchIntervallUpperBound);
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

			System.out.println(String.format("Time: %5.2f \t Value: %10.3f", abatementMaxTime, value));
			
			// Note: The GoldenSectionSearch optimizer is a minimizer. But we like to maximize the value. Hence we pass -value to the optimzer.
			optimizer.setValue(-value);
		}
		
		// Get optimal value
		final double abatementMaxTime = optimizer.getBestPoint();

		/*
		 * Create our abatement model
		 */
		final UnaryOperator<Double> abatementFunction = time -> Math.min(abatementInitial + (abatementMax-abatementInitial)/abatementMaxTime * time, abatementMax);
		
		/*
		 * Create the DICE model
		 */
		final ClimateModel climateModel = new DICEModel(timeDiscretization, abatementFunction, savingsRateFunction, discountRate);


		/*
		 * Plot
		 */
		Plots
		.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getTemperature()).mapToDouble(Temperature::getExpectedTemperatureOfAtmosphere).toArray(), 0, 300, 3)
		.setTitle("Temperature (T =" + abatementMaxTime + ", r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel("Temperature [°C]").show();

		Plots
		.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getCarbonConcentration()).mapToDouble(CarbonConcentration::getExpectedCarbonConcentrationInAtmosphere).toArray(), 0, 300, 3)
		.setTitle("Carbon Concentration (T =" + abatementMaxTime + ", r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel("Carbon concentration [GtC]").show();

		Plots
		.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getEmission()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
		.setTitle("Emission (T =" + abatementMaxTime + ", r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel("Emission [GtCO2/yr]").show();

		Plots
		.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getGDP()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
		.setTitle("Output (T =" + abatementMaxTime + ", r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel(" Output [Tr$2005]").show();

		Plots
		.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getAbatement()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
		.setTitle("Abatement (T =" + abatementMaxTime + ", r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel("Abatement \u03bc").show();
	}
}
