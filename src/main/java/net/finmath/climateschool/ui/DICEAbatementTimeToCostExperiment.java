package net.finmath.climateschool.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.finmath.climate.models.ClimateModel;
import net.finmath.climate.models.dice.DICEModel;
import net.finmath.plots.GraphStyle;
import net.finmath.plots.Plot2D;
import net.finmath.plots.PlotablePoints2D;
import net.finmath.plots.Plots;
import net.finmath.plots.Point2D;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

public class DICEAbatementTimeToCostExperiment extends ExperimentUI {

	private static final double timeStep = 1.0;
	private static final double timeHorizon = 500.0;

	private final DecimalFormat numberDigit3 = new DecimalFormat("#.000");
	private final DecimalFormat numberPercent2 = new DecimalFormat("#.00%");

	Plot2D plotCost = null;
	Plot2D plotCostDiscounted = null;
	Plot2D plotCostPerGDP = null;
	Plot2D plotAbatement = null;

	public DICEAbatementTimeToCostExperiment() {
		super(List.of(
				new Parameter("Discount Rate", 0.03, 0.01, 0.05),
				new Parameter("Abatement Max Time", 100.0, 10.0, 200.0)
				));
	}


	public String getTitle() { return "DICE Model - One Parametric Abatement Model - NOT CALIBRATED"; }

	/**
	 * run the re-calculation 
	 */
	public void runCalculation() {
		Map<String, Double> currentParameterSet = getExperimentParameters().stream().collect(Collectors.toMap(p -> p.value().getName(), p -> p.value().get()));

		System.out.println("Calculation with Parameters: " + currentParameterSet);

		/*
		 * Discount rate
		 */
		final double discountRate = currentParameterSet.get("Discount Rate");

		/*
		 * Parameters for the abatement model (abatement = fraction of industrial CO2 reduction; 1.00 ~ 100 % reduction ~ carbon neutral).
		 */
		final double abatementInitial = 0.03;
		final double abatementMax = 1.00;
		final double abatementMaxTime = currentParameterSet.get("Abatement Max Time");

		/*
		 * Create a time discretization
		 */
		final int numberOfTimeSteps = (int)Math.round(timeHorizon / timeStep);
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, numberOfTimeSteps, timeStep);

		/*
		 * Create our abatement model: it is a piecewise linear funtion: starting in abatementInitial, then reaching abatementMax in abatementMaxTime years, then staying at abatementMax.
		 */
		final UnaryOperator<Double> abatementFunction = time -> Math.min(abatementInitial + (abatementMax-abatementInitial)/abatementMaxTime * time, abatementMax);

		/*
		 * Create our savings rate model: a constant.
		 */
		final UnaryOperator<Double> savingsRateFunction = time -> 0.26;

		/*
		 * Create the DICE model from the given parameters.
		 */
		final ClimateModel climateModel = new DICEModel(timeDiscretization, abatementFunction, savingsRateFunction, discountRate);

		System.out.println("DICEModel initialized");

		/*
		 * Plots
		 */

		plot(climateModel, abatementMaxTime, discountRate);
	}

	private void plot(ClimateModel climateModel, double abatementMaxTime, double discountRate) {
		System.out.println("Generating plots for " + climateModel);
		
		final double timeHorizonInPlot = 400;

		final TimeDiscretization timeDiscretization = climateModel.getTimeDiscretization();

		final RandomVariable[] abatement = climateModel.getAbatement();
		final RandomVariable[] damage = climateModel.getDamage();
		final RandomVariable[] damageCosts = climateModel.getDamageCosts();
		final RandomVariable[] abatementCosts = climateModel.getAbatementCosts();
		final RandomVariable[] gdp = climateModel.getGDP();
		final RandomVariable[] emission = climateModel.getEmission();

		final List<Point2D> costDamage			= new ArrayList<Point2D>();
		final List<Point2D> costDamageDiscounted	= new ArrayList<Point2D>();
		final List<Point2D> costDamagePerGDP	= new ArrayList<Point2D>();

		final List<Point2D> costAbatement			= new ArrayList<Point2D>();
		final List<Point2D> costAbatementDiscounted	= new ArrayList<Point2D>();
		final List<Point2D> costAbatementPerGDP	= new ArrayList<Point2D>();

		final List<Point2D> costTotal			= new ArrayList<Point2D>();
		final List<Point2D> costTotalDiscounted	= new ArrayList<Point2D>();
		final List<Point2D> costTotalPerGDP	= new ArrayList<Point2D>();

		final List<Point2D> costAveraged			= new ArrayList<Point2D>();
		final List<Point2D> costAveragedDiscounted	= new ArrayList<Point2D>();
		final List<Point2D> costAveragedPerGDP	= new ArrayList<Point2D>();

		for(int i=0; i<damageCosts.length-1; i+=1) {
			final double time = timeDiscretization.getTime(i);
			final RandomVariable numeraire = Scalar.of(Math.exp(discountRate * time));

			costDamage.add(new Point2D(timeDiscretization.getTime(i),damageCosts[i].getAverage()));
			costDamageDiscounted.add(new Point2D(timeDiscretization.getTime(i),damageCosts[i].div(numeraire).getAverage()));
			costDamagePerGDP.add(new Point2D(timeDiscretization.getTime(i),damageCosts[i].div(gdp[i]).getAverage()));

			costAbatement.add(new Point2D(timeDiscretization.getTime(i),abatementCosts[i].getAverage()));
			costAbatementDiscounted.add(new Point2D(timeDiscretization.getTime(i),abatementCosts[i].div(numeraire).getAverage()));
			costAbatementPerGDP.add(new Point2D(timeDiscretization.getTime(i),abatementCosts[i].div(gdp[i]).getAverage()));

			costTotal.add(new Point2D(timeDiscretization.getTime(i), damageCosts[i].add(abatementCosts[i]).getAverage()));
			costTotalDiscounted.add(new Point2D(timeDiscretization.getTime(i),damageCosts[i].add(abatementCosts[i]).div(numeraire).getAverage()));
			costTotalPerGDP.add(new Point2D(timeDiscretization.getTime(i),damageCosts[i].add(abatementCosts[i]).div(gdp[i]).getAverage()));

			costAveragedDiscounted.add(new Point2D(timeDiscretization.getTime(i),IntStream.range(i, Math.min(i+100, damageCosts.length-1)).mapToDouble(j -> damageCosts[j].add(abatementCosts[j]).div(climateModel.getNumeraire(timeDiscretization.getTime(j))).div(100.0).getAverage()).sum()));
			costAveragedPerGDP.add(new Point2D(timeDiscretization.getTime(i),IntStream.range(i, Math.min(i+100, damageCosts.length-1)).mapToDouble(j -> damageCosts[j].add(abatementCosts[j]).div(climateModel.getNumeraire(timeDiscretization.getTime(j))).getAverage()).sum() / IntStream.range(i, Math.min(i+100, damageCosts.length)).mapToDouble(j -> gdp[j].div(climateModel.getNumeraire(timeDiscretization.getTime(j))).getAverage()).sum()));
		}

		String paramSpec = "T(\u03BC=1) =" + numberDigit3.format(abatementMaxTime) + ", r = " + numberPercent2.format(discountRate);

		if(plotCost == null) {
			plotCost = new Plot2D(
					List.of(
							new PlotablePoints2D("damage", costDamage, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.RED)),
							new PlotablePoints2D("abatement", costAbatement, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.GREEN)),
							new PlotablePoints2D("total", costTotal, new GraphStyle(new Rectangle(3, 3), new BasicStroke(), Color.BLUE))))
					.setXAxisLabel("time t")
					.setYAxisLabel("cost")
					.setXRange(0, timeHorizonInPlot)
					.setTitle("Cost");
			plotCost.show();
		}
		else {
			plotCost.update(
					List.of(
							new PlotablePoints2D("damage", costDamage, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.RED)),
							new PlotablePoints2D("abatement", costAbatement, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.GREEN)),
							new PlotablePoints2D("total", costTotal, new GraphStyle(new Rectangle(3, 3), new BasicStroke(), Color.BLUE))));
		}

		if(plotCostDiscounted == null) {
			plotCostDiscounted = new Plot2D(
					List.of(
							new PlotablePoints2D("damage", costDamageDiscounted, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.RED)),
							new PlotablePoints2D("abatement", costAbatementDiscounted, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.GREEN)),
							new PlotablePoints2D("total", costTotalDiscounted, new GraphStyle(new Rectangle(3, 3), new BasicStroke(), Color.BLUE))))
//							new PlotablePoints2D("total averaged", costAveragedDiscounted, new GraphStyle(new Rectangle(3, 3), new BasicStroke(), Color.BLACK))))
					.setXAxisLabel("time t")
					.setYAxisLabel("cost (discounted)")
					.setXRange(0, timeHorizonInPlot)
					.setIsLegendVisible(true)
					.setTitle("Cost Discounted");
			plotCostDiscounted.show();
		}
		else {
			plotCostDiscounted.update(
					List.of(
							new PlotablePoints2D("damage", costDamageDiscounted, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.RED)),
							new PlotablePoints2D("abatement", costAbatementDiscounted, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.GREEN)),
							new PlotablePoints2D("total", costTotalDiscounted, new GraphStyle(new Rectangle(3, 3), new BasicStroke(), Color.BLUE))));
//							new PlotablePoints2D("total averaged", costAveragedDiscounted, new GraphStyle(new Rectangle(3, 3), new BasicStroke(), Color.BLACK))));
		}

		if(plotCostPerGDP == null) {
			plotCostPerGDP = new Plot2D(
					List.of(
							new PlotablePoints2D("damage", costDamagePerGDP, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.RED)),
							new PlotablePoints2D("abatement", costAbatementPerGDP, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.GREEN)),
							new PlotablePoints2D("total", costTotalPerGDP, new GraphStyle(new Rectangle(3, 3), new BasicStroke(), Color.BLUE)),
							new PlotablePoints2D("total averaged", costAveragedPerGDP, new GraphStyle(new Rectangle(3, 3), new BasicStroke(), Color.BLACK))))
					.setXAxisLabel("time t")
					.setYAxisLabel("cost/gdp")
					.setXRange(0, timeHorizonInPlot)
					.setYAxisNumberFormat(new DecimalFormat("0.0%"))
					.setIsLegendVisible(true)
					.setTitle("Cost per GDP");
			plotCostPerGDP.show();
		}
		else {
			plotCostPerGDP.update(
					List.of(
							new PlotablePoints2D("damage", costDamagePerGDP, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.RED)),
							new PlotablePoints2D("abatement", costAbatementPerGDP, new GraphStyle(new Rectangle(2, 2), new BasicStroke(), Color.GREEN)),
							new PlotablePoints2D("total", costTotalPerGDP, new GraphStyle(new Rectangle(3, 3), new BasicStroke(), Color.BLUE)),
							new PlotablePoints2D("total averaged", costAveragedPerGDP, new GraphStyle(new Rectangle(3, 3), new BasicStroke(), Color.BLACK))));
		}

		if(plotAbatement == null) {
			plotAbatement = Plots
					.createScatter(timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getAbatement()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
					.setTitle("Abatement (" + paramSpec + ")").setXAxisLabel("time (years)").setYAxisLabel("Abatement \u03bc");
			plotAbatement.show();
		}
		else {
			Plots
			.updateScatter(plotAbatement, timeDiscretization.getAsDoubleArray(), Arrays.stream(climateModel.getAbatement()).mapToDouble(RandomVariable::getAverage).toArray(), 0, 300, 3)
			.setTitle("Abatement (" + paramSpec + ")").setXAxisLabel("time (years)").setYAxisLabel("Abatement \u03bc");
		}
}
}
