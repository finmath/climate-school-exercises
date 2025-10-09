package net.finmath.climateschool.ui;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

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

public class DICECalibrationExperimentUI extends ExperimentUI {

	private static final double timeStep = 1.0;
	private static final double timeHorizon = 500.0;

	private final DecimalFormat numberDigit3 = new DecimalFormat("#.000");
	private final DecimalFormat numberPercent2 = new DecimalFormat("#.00%");

	double[] initialParameters;

	Plot2D plotTemperature = null;
	Plot2D plotCarbon = null;
	Plot2D plotEmission = null;
	Plot2D plotOutput = null;
	Plot2D plotAbatement = null;

	public DICECalibrationExperimentUI() {
		super(List.of(
				new Parameter("Discount Rate", 0.03, 0.01, 0.05)
//				new Parameter("Abatement Max Time", 50.0, 10.0, 200.0)
				));
	}
	
	public String getTitle() { return "DICE Model - Full Abatement Model - Optimized Emisison Path (Calibration)"; }

	public void runCalculation() {
		Map<String, Double> currentParameterSet = getExperimentParameters().stream().collect(Collectors.toMap(p -> p.value().getName(), p -> p.value().get()));

		System.out.println("Calculation with Parameters: " + currentParameterSet);

		/*
		 * Create a time discretization
		 */
		final int numberOfTimeSteps = (int)Math.round(timeHorizon / timeStep);
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, numberOfTimeSteps, timeStep);

		/*
		 * Create our savings rate model: a constant.
		 */
		final UnaryOperator<Double> savingsRateFunction = time -> 0.26;

		/*
		 * Discount rate
		 */
		final double discountRate = currentParameterSet.get("Discount Rate");

		// Initial parameters for our abatement function
		if(initialParameters == null) {
			initialParameters = new double[timeDiscretization.getNumberOfTimes()];
			Arrays.fill(initialParameters, -Math.log(-Math.log(0.8)));
		}

		final AdamOptimizerUsingFiniteDifferences optimizer = new AdamOptimizerUsingFiniteDifferences(initialParameters, 800, 0.05, GradientMethod.AVERAGE) {
			private int iteration = 0;
			@Override
			public RandomVariable setValue(RandomVariable[] parameters) {
				if(Thread.interrupted()) {
					this.stop();
				}
				
				double[] abatementParameter = Arrays.stream(parameters).mapToDouble(RandomVariable::getAverage).map(x -> Math.exp(-Math.exp(-x))).toArray();
				abatementParameter[0] = 0.03;
				initialParameters = Arrays.stream(parameters).mapToDouble(RandomVariable::getAverage).toArray();
				
				/*
				 * Create our abatement model
				 */
				final UnaryOperator<Double> abatementFunction = time -> abatementParameter[(int)Math.round(time/timeStep)];

				/*
				 * Create the DICE model
				 */
				final ClimateModel climateModel = new DICEModel(timeDiscretization, abatementFunction, savingsRateFunction, discountRate);

				final double value = climateModel.getValue().expectation().doubleValue();

				// Penalty for non-smoothness - it works without this, but this helps the optimizer to avoid onszillations (that are la
				double roughness = 0.0;
				for(int i=1; i<abatementParameter.length-2; i++) {
					roughness += Math.pow(abatementParameter[i+1] - abatementParameter[i], 2.0);
				}
				roughness = Math.sqrt(roughness);
				roughness /= abatementParameter.length;
				roughness *= 0.1;

				// Update the plot every 200 iterations
				if(iteration%200 == 0) {
					String spec = "(r = " + discountRate + ")";
					plot(climateModel, spec);
				}
				iteration++;

				return Scalar.of(-value);// + roughness);
			}
		};

		optimizer.run();

		// Get optimal value
		final RandomVariable[] bestParameters = optimizer.getBestFitParameters();
		double[] abatementParameter = Arrays.stream(bestParameters).mapToDouble(RandomVariable::getAverage).map(x -> Math.exp(-Math.exp(-x))).toArray();
		abatementParameter[0] = 0.03;
		System.out.println(Arrays.toString(abatementParameter));

		/*
		 * Create our abatement model
		 */
		final UnaryOperator<Double> abatementFunction = time -> abatementParameter[(int)Math.round(time/timeStep)];

		/*
		 * Create the DICE model
		 */
		final ClimateModel climateModel = new DICEModel(timeDiscretization, abatementFunction, savingsRateFunction, discountRate);
		
		String spec = "r = " + numberPercent2.format(discountRate) + "";
		plot(climateModel, spec);
	}

	private void plot(ClimateModel climateModel, String spec) {
		/*
		 * Plots
		 */

		TimeDiscretization timeDiscretization = climateModel.getTimeDiscretization();
		
		if(plotTemperature == null) {
			plotTemperature = Plots
					.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getTemperature()).mapToDouble(Temperature::getExpectedTemperatureOfAtmosphere).toArray(), 0, 300, 3)
					.setTitle("Temperature (" + spec + ")").setXAxisLabel("time (years)").setYAxisLabel("Temperature [°C]");
			plotTemperature.show();
		}
		else {
			Plots
			.updateScatter(plotTemperature, timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getTemperature()).mapToDouble(Temperature::getExpectedTemperatureOfAtmosphere).toArray(), 0, 300, 3)
			.setTitle("Temperature (" + spec + ")").setXAxisLabel("time (years)").setYAxisLabel("Temperature [°C]");

		}


		if(plotCarbon == null) {
			plotCarbon = Plots
					.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getCarbonConcentration()).mapToDouble(CarbonConcentration::getExpectedCarbonConcentrationInAtmosphere).toArray(), 0, 300, 3)
					.setTitle("Carbon Concentration (" + spec + ")").setXAxisLabel("time (years)").setYAxisLabel("Carbon concentration [GtC]");
			plotCarbon.show();
		}
		else {
			Plots
			.updateScatter(plotCarbon, timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getCarbonConcentration()).mapToDouble(CarbonConcentration::getExpectedCarbonConcentrationInAtmosphere).toArray(), 0, 300, 3)
			.setTitle("Carbon Concentration (" + spec + ")").setXAxisLabel("time (years)").setYAxisLabel("Carbon concentration [GtC]");
		}

		if(plotEmission == null) {
			plotEmission = Plots
					.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getEmission()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
					.setTitle("Emission (" + spec + ")").setXAxisLabel("time (years)").setYAxisLabel("Emission [GtCO2/yr]");
			plotEmission.show();
		}
		else {
			Plots
			.updateScatter(plotEmission, timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getEmission()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
			.setTitle("Emission (" + spec + ")").setXAxisLabel("time (years)").setYAxisLabel("Emission [GtCO2/yr]");

		}

		if(plotOutput == null) {
			plotOutput = Plots
					.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getGDP()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
					.setTitle("Output (" + spec + ")").setXAxisLabel("time (years)").setYAxisLabel(" Output [Tr$2005]");			
			plotOutput.show();
		}
		else {
			Plots
			.updateScatter(plotOutput, timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getGDP()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
			.setTitle("Output (" + spec + ")").setXAxisLabel("time (years)").setYAxisLabel(" Output [Tr$2005]");
		}

		if(plotAbatement == null) {
			plotAbatement = Plots
					.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getAbatement()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
					.setTitle("Abatement (" + spec + ")").setXAxisLabel("time (years)").setYAxisLabel("Abatement \u03bc");
			plotAbatement.show();
		}
		else {
			Plots
			.updateScatter(plotAbatement, timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getAbatement()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
			.setTitle("Abatement (" + spec + ")").setXAxisLabel("time (years)").setYAxisLabel("Abatement \u03bc");
		}
	}
}
