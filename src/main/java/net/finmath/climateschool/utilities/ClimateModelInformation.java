package net.finmath.climateschool.utilities;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.finmath.climate.models.ClimateModel;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Calculate some info
 *
 * @author Christian Fries
 */
public class ClimateModelInformation {

	/**
	 * Calculates the dV/dC(t) for a model.
	 *
	 * @param climateModel A <code>ClimateModel</code>. Needs to be an {@link SIAModelWithNonlinearFunding} that is initialized with an AAD {@link RandomVariableFactory}.
	 * @return
	 * @throws Exception
	 */
	public static Map<Double, RandomVariable> costToVaueImpactOf(ClimateModel climateModel) {
		final Map<Double, RandomVariable> costToValueImpact = new HashMap<>();

		final TimeDiscretization timeDiscretization = climateModel.getTimeDiscretization();
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			final double time = timeDiscretization.getTimeIndex(timeIndex);

			final RandomVariableDifferentiable abatementCost	= (RandomVariableDifferentiable) climateModel.getAbatementCosts()[timeIndex];
			final RandomVariableDifferentiable damageCost		= (RandomVariableDifferentiable) climateModel.getDamageCosts()[timeIndex];
			final RandomVariableDifferentiable value = (RandomVariableDifferentiable) climateModel.getValue();

			// Note: For the standard model the derivative is the same for abatementCost and damageCost.
			// For a model with funding of abatementCost the derivative w.r.t. abatementCost becomes different.
			final RandomVariable valueSensitivityToDamageCost = value.getGradient().get(damageCost.getID());

			costToValueImpact.put(time, valueSensitivityToDamageCost);
		}
		return costToValueImpact;
	}

	public static Map<Double, RandomVariable> socialCostOfCarbon(ClimateModel climateModel) {

		final Map<Long, RandomVariable> gradient = ((RandomVariableDifferentiable)(climateModel.getValue())).getGradient();

		final RandomVariableDifferentiable[] emissions = Arrays.stream(climateModel.getEmission()).map(RandomVariableDifferentiable.class::cast).toArray(RandomVariableDifferentiable[]::new);
		final RandomVariableDifferentiable[] damages = Arrays.stream(climateModel.getDamage()).map(RandomVariableDifferentiable.class::cast).toArray(RandomVariableDifferentiable[]::new);
		final RandomVariableDifferentiable[] consumptions = Arrays.stream(climateModel.getConsumptions()).map(RandomVariableDifferentiable.class::cast).toArray(RandomVariableDifferentiable[]::new);

		final Map<Double, RandomVariable> scc = new HashMap<>();
		final double[] asc = new double[climateModel.getTimeDiscretization().getNumberOfTimes()];
		for(int i=0; i<climateModel.getTimeDiscretization().getNumberOfTimes()-3; i++) {
			final RandomVariable dWdE = gradient.get(emissions[i].getID());
			final RandomVariable dWdC = gradient.get(consumptions[i].getID());
			final RandomVariable dWdD = gradient.get(damages[i].getID());
			scc.put(climateModel.getTimeDiscretization().getTime(i), dWdE.div(dWdC).mult(-1000));
		}

		return scc;
	}
}

