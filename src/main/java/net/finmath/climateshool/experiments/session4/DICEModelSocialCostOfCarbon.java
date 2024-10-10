package net.finmath.climateshool.experiments.session4;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import net.finmath.climate.models.ClimateModel;
import net.finmath.climate.models.dice.DICEModel;
import net.finmath.plots.Plots;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Experiment to calculate the social cost of carbon
 * by applying a finite difference to consumption and emission.
 * 
 * @author Christian Fries
 */
public class DICEModelSocialCostOfCarbon {

	// Contants (fixed model parameters)
	private static final double timeStep = 1.0;
	private static final double timeHorizon = 500.0;

	private static final double abatementInitial = 0.03;
	private static final double abatementMax = 1.00;
	private static final double abatementMaxTime = 150.0;

	public static void main(String[] args) {

		System.out.println("\t Discount Rate \t  SCC");
		System.out.println("_".repeat(79));

		List<Double> discountRates = new ArrayList<>();
		List<Double> socialCostOfCarbons = new ArrayList<>();
		for(double discountRate = 0.005; discountRate <= 0.05; discountRate += 0.001) {

			double scc = getSocialCostOfCarbonForGivenDiscountRate(discountRate);
			
			discountRates.add(discountRate);
			socialCostOfCarbons.add(scc);
			System.out.println(String.format("\t %8.4f \t %8.4f", discountRate, scc));
		}

		Plots
		.createScatter(discountRates, socialCostOfCarbons, 0, 300, 3)
		.setTitle("Social Cost of Carbon (T(\u03BC=1) =" + abatementMaxTime + ")")
		.setXAxisLabel("rate (r)").setXAxisNumberFormat(new DecimalFormat("0.0%")).setYAxisLabel("SCC").show();

		System.out.println("_".repeat(79));
	}

	private static double getSocialCostOfCarbonForGivenDiscountRate(double discountRate) {
		final UnaryOperator<Double> abatementFunction = time -> Math.min(abatementInitial + (abatementMax-abatementInitial)/abatementMaxTime * time, abatementMax);

		final int numberOfTimeSteps = (int)Math.round(timeHorizon / timeStep);
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, numberOfTimeSteps, timeStep);

		// The value of the unshifted model
		final ClimateModel climateModel = new DICEModel(timeDiscretization, abatementFunction, t -> 0.26, discountRate);
		double value = climateModel.getValue().doubleValue();

		// The function that determines at which time index the shift should be applied.
		Predicate<Integer> isTimeIndexToShift = i -> i==1;
		
		// The value of the model with a shift to the consumption
		double valueDC = (new DICEModel(timeDiscretization, abatementFunction, t -> 0.26, discountRate,
				Map.of("isTimeIndexToShift", isTimeIndexToShift, "initialConsumptionShift", 0.01))).getValue().doubleValue();

		// The value of the model with a shift to the emission
		double valueDE = (new DICEModel(timeDiscretization, abatementFunction, t -> 0.26, discountRate,
				Map.of("isTimeIndexToShift", isTimeIndexToShift, "initialEmissionShift", 0.01))).getValue().doubleValue();

		/*
		 * The social cost of carbon
		 * scc = dV/dE / dV/dC
		 */
		double scc = -(valueDE-value) / (valueDC-value) * 1000;
		
		return scc;
	}
}
