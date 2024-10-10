package net.finmath.climateshool.experiments.session6;

import java.text.DecimalFormat;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

import net.finmath.climate.models.utils.RandomOperators;
import net.finmath.climateschool.utilities.ModelFactory;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.plots.DoubleToRandomVariableFunction;
import net.finmath.plots.Plot;
import net.finmath.plots.PlotProcess2D;
import net.finmath.stochastic.RandomOperator;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Creates a stochastic interest rate model (short rate model) and plots the short rate.
 * 
 * It is possible to play with the parameters volatility and mean reversion.
 *
 * @author Christian Fries
 */
public class StochasticRatesExperiment {

	public static void main(String[] args) throws Exception {

		(new StochasticRatesExperiment()).plotInterestRatesForVolAndMR(0.001, 0.005);
		(new StochasticRatesExperiment()).plotInterestRatesForVolAndMR(0.0030, 0.01);
		(new StochasticRatesExperiment()).plotInterestRatesForVolAndMR(0.0040, 0.02);
	}

	private void plotInterestRatesForVolAndMR(double shortRateVolatility, double shortRateMeanreversion) throws Exception {

		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, 500, 1);
		final TermStructureMonteCarloSimulationModel interestRateModel = ModelFactory.getInterestRateModel(
				timeDiscretization,
				timeDiscretization,
				new double[] { 0.0, 500.0 }, new double[] { 0.02, 0.02 },
				shortRateVolatility, shortRateMeanreversion, 10000);

		final DoubleToRandomVariableFunction numeraire = t -> interestRateModel.getNumeraire(Math.min(t, 500));
		final DoubleToRandomVariableFunction shortRate = t -> numeraire.apply(t+1).div(numeraire.apply(t)).log();
		//		final DoubleToRandomVariableFunction shortRate = t-> interestRateModel.getProcess().getProcessValue((int) t, 0);
		final Function<Double, DoubleUnaryOperator> rateAtQuantile = q -> {
			return t -> {
				try {
					final RandomOperator es;
					if(q >= 0) {
						es = RandomOperators.leftTailExpectedShortFall(q);
					} else {
						es = RandomOperators.rightTailExpectedShortFall(1+q);
					}
					return es.apply(numeraire.apply(t+1).invert()).log().mult(-1).div(t+1).getAverage();
				} catch (final Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return 0;
			};
		};

		final String titleSpec = "\u03c3="
				+String.format("%-10.3f",shortRateVolatility*100).trim() + "%, "
				+"a="
				+String.format("%-10.3f",shortRateMeanreversion*100).trim() + "%, "
				+ "";

		final Plot plot1 = new PlotProcess2D(timeDiscretization, shortRate, 100)
				.setTitle("Simulated Short Rate Process " + titleSpec)
				.setXAxisLabel("time")
				.setYAxisLabel("r (short rate)")
				.setYAxisNumberFormat(new DecimalFormat("0.0%"));
		plot1.show();

	}
}
