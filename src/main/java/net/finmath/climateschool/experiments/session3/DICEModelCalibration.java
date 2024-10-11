package net.finmath.climateschool.experiments.session3;

import java.util.Arrays;
import java.util.function.UnaryOperator;

import net.finmath.climate.models.CarbonConcentration;
import net.finmath.climate.models.ClimateModel;
import net.finmath.climate.models.Temperature;
import net.finmath.climate.models.dice.DICEModel;
import net.finmath.climateschool.utilities.AdamOptimizerUsingFiniteDifferences;
import net.finmath.climateschool.utilities.AdamOptimizerUsingFiniteDifferences.GradientMethod;
import net.finmath.plots.Plot2D;
import net.finmath.plots.Plots;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Experiment related to the DICE model.
 * 
 * Calibrates a full abatement pice-wise constant abatement function. With 500 time steps the model has 500 free parameters.
 * The abatement function will be plotted during the calibration, such that you can observe how the optimizer is approaching the optimal
 * abatement paths.
 * 
 * Observation: The optimizer first improves the "early years" and does not care about the far future. This is due to the
 * value function being less sensitive to the far future.
 * 
 * Suggestion: Change the discount rate and observe that with lower discount rate the calibration takes a larger focus on later years.
 */
public class DICEModelCalibration {

	private static final double timeStep = 1.0;
	private static final double timeHorizon = 500.0;

	public static void main(String[] args) {

		/*
		 * Discount rate
		 */
		final double discountRate = 0.03;

		/*
		 * Create a time discretization
		 */
		final int numberOfTimeSteps = (int)Math.round(timeHorizon / timeStep);
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, numberOfTimeSteps, timeStep);

		/*
		 * Create our savings rate model: a constant
		 */
		final UnaryOperator<Double> savingsRateFunction = time -> 0.26;


		// Initial parameters for our abatement function
		final double[] initialParameters = new double[timeDiscretization.getNumberOfTimes()];
		Arrays.fill(initialParameters, 0.03);

		// Create a plot of the abatement function (will be updated during iterations)
		Plot2D plot = Plots
				.createScatter(timeDiscretization.getAsDoubleArray(), timeDiscretization.getAsDoubleArray(), 0, 300, 3)
				.setTitle("Abatement (r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel("Abatement \u03bc");
		plot.show();

		final AdamOptimizerUsingFiniteDifferences optimizer = new AdamOptimizerUsingFiniteDifferences(initialParameters, 800, 0.1, GradientMethod.AVERAGE) {
			private int i = 0;
			@Override
			public RandomVariable setValue(RandomVariable[] parameters) {

				double[] abatementParameter = Arrays.stream(parameters).mapToDouble(RandomVariable::getAverage).map(x -> Math.exp(-Math.exp(-x))).toArray();

				/*
				 * Create our abatement model
				 */
				final UnaryOperator<Double> abatementFunction = time -> abatementParameter[(int)Math.round(time)];

				/*
				 * Create the DICE model
				 */
				final ClimateModel climateModel = new DICEModel(timeDiscretization, abatementFunction, savingsRateFunction, discountRate);

				final double value = climateModel.getValue().expectation().doubleValue();

				// Update the plot every 200 iterations
				if(i%200 == 0) {
					Plots.updateScatter(plot, timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getAbatement()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3);
				}
				i++;

				return Scalar.of(-value);
			}
		};

		optimizer.run();
		System.out.println(Arrays.toString(Arrays.stream(optimizer.getBestFitParameters()).mapToDouble(RandomVariable::getAverage).toArray()));


		// Get optimal value
		final RandomVariable[] bestParameters = optimizer.getBestFitParameters();

		/*
		 * Create our abatement model
		 */
		final UnaryOperator<Double> abatementFunction = time -> bestParameters[(int)Math.round(time)].getAverage();

		/*
		 * Create the DICE model
		 */
		final ClimateModel climateModel = new DICEModel(timeDiscretization, abatementFunction, savingsRateFunction, discountRate);


		/*
		 * Plot
		 */

		Plots
		.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getTemperature()).mapToDouble(Temperature::getExpectedTemperatureOfAtmosphere).toArray(), 0, 300, 3)
		.setTitle("Temperature (T(\u03BC=1) =" + ", r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel("Temperature [°C]").show();

		Plots
		.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getCarbonConcentration()).mapToDouble(CarbonConcentration::getExpectedCarbonConcentrationInAtmosphere).toArray(), 0, 300, 3)
		.setTitle("Carbon Concentration (T(\u03BC=1) =" + ", r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel("Carbon concentration [GtC]").show();

		Plots
		.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getEmission()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
		.setTitle("Emission (T(\u03BC=1) =" + ", r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel("Emission [GtCO2/yr]").show();

		Plots
		.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getGDP()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
		.setTitle("Output (T(\u03BC=1) =" + ", r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel(" Output [Tr$2005]").show();

		Plots
		.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getAbatement()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
		.setTitle("Abatement (scenario =" + ", r = " + discountRate + ")").setXAxisLabel("time (years)").setYAxisLabel("Abatement \u03bc").show();
	}
}
