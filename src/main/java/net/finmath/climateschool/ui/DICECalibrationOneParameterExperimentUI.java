package net.finmath.climateschool.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.animation.PauseTransition;
import javafx.util.converter.NumberStringConverter;
import net.finmath.climate.models.CarbonConcentration;
import net.finmath.climate.models.ClimateModel;
import net.finmath.climate.models.Temperature;
import net.finmath.climate.models.dice.DICEModel;
import net.finmath.optimizer.GoldenSectionSearch;
import net.finmath.plots.Plot2D;
import net.finmath.plots.Plots;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class DICECalibrationOneParameterExperimentUI extends ExperimentUI {

	private static final double timeStep = 1.0;
	private static final double timeHorizon = 500.0;

	private final DecimalFormat numberDigit3 = new DecimalFormat("#.000");
	private final DecimalFormat numberPercent2 = new DecimalFormat("#.00%");

	Plot2D plotTemperature = null;
	Plot2D plotCarbon = null;
	Plot2D plotEmission = null;
	Plot2D plotOutput = null;
	Plot2D plotAbatement = null;

	public DICECalibrationOneParameterExperimentUI() {
		super(List.of(
				new Parameter("Discount Rate", 0.03, 0.01, 0.05)
//				new Parameter("Abatement Max Time", 50.0, 10.0, 200.0)
				));
	}
	

	public String getTitle() { return "DICE Model - One Parametric Abatement Model - Optimized Emisison Path (Calibration)"; }

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

		/*
		 * Parameters for the abatement model (abatement = fraction of industrial CO2 reduction; 1.00 ~ 100 % reduction ~ carbon neutral).
		 */
		final double abatementInitial = 0.03;
		final double abatementMax = 1.00;
//		final double abatementMaxTime = currentParameterSet.get("Abatement Max Time");

		/*
		 * Search for the optimal value of abatementMaxTime
		 */
		double searchIntervallLowerBound = 10.0;
		double searchIntervallUpperBound = 300.0;		
		GoldenSectionSearch optimizer = new GoldenSectionSearch(searchIntervallLowerBound, searchIntervallUpperBound);
		while(optimizer.getAccuracy() > 1E-5 && !optimizer.isDone()) {

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
		 * Create our abatement model: it is a piecewise linear funtion: starting in abatementInitial, then reaching abatementMax in abatementMaxTime years, then staying at abatementMax.
		 */
		final UnaryOperator<Double> abatementFunction = time -> Math.min(abatementInitial + (abatementMax-abatementInitial)/abatementMaxTime * time, abatementMax);

		/*
		 * Create the DICE model from the given parameters.
		 */
		final ClimateModel climateModel = new DICEModel(timeDiscretization, abatementFunction, savingsRateFunction, discountRate);
		
		String spec = "T(\u03BC=1) =" + numberDigit3.format(abatementMaxTime) + ", r = " + numberPercent2.format(discountRate);
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
