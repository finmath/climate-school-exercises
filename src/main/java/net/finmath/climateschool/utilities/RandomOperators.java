package net.finmath.climateschool.utilities;

import net.finmath.stochastic.RandomOperator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Operators on <code>RandomVariable</code>.
 *
 * @author Christian Fries
 * @author Lennart Quante
 */
public class RandomOperators {

	/**
	 * X &mapsto; E(X)
	 *
	 * @return The operator X &mapsto; E(X)
	 */
	public static RandomOperator expectation() {
		return x -> x.average();
	}

	/**
	 * X &mapsto; ES_\alpha(X) where ES_\alpha(X) = E(X \cdot 1(x \leq VaR_\alpha(X)) / \alpha)
	 *
	 * This is the same as <code>leftTailExpectedShortFall</code>.
	 *
	 * @param percentageLevel the percentage \alpha level of the expected shortfall.
	 * @return The operator X &mapsto; ES_\alpha(X)
	 */
	public static RandomOperator expectedShortFall(Double percentageLevel) {
		return x -> expectedShortFall(x, percentageLevel);
	}

	/**
	 * ES_\alpha(X) = E(X \cdot 1(x \leq VaR_\alpha(X)) / \alpha)
	 *
	 * This is the same as <code>leftTailExpectedShortFall</code>.
	 *
	 * @param percentageLevel the percentage \alpha level of the expected shortfall.
	 * @return ES_\alpha(X)
	 */
	public static RandomVariable expectedShortFall(RandomVariable x, Double percentageLevel) {
		return leftTailExpectedShortFall(x, percentageLevel);
	}

	/**
	 * X &mapsto; E(X) - alpha ES_\alpha(X)
	 *
	 * @param percentageLevel the percentage \alpha level of the expected shortfall.
	 * @return The E(X) - alpha ES_\alpha(X)
	 */
	public static RandomOperator expectedShortFallComplement(Double percentageLevel) {
		return x -> x.average().sub(RandomOperators.expectedShortFall(percentageLevel).apply(x).mult(percentageLevel));
	}

	/**
	 * X &mapsto; ES_\alpha(X)
	 *
	 * @param percentageLevel the percentage \alpha level of the expected shortfall.
	 * @return The operator X &mapsto; ES_\alpha(X)
	 */
	public static RandomOperator rightTailExpectedShortFall(Double percentageLevel) {
		return x -> rightTailExpectedShortFall(x, percentageLevel);
	}

	/**
	 * X &mapsto; ES_\alpha(X)
	 *
	 * @param percentageLevel the percentage \alpha level of the expected shortfall.
	 * @return The operator X &mapsto; ES_\alpha(X)
	 */
	public static RandomOperator leftTailExpectedShortFall(Double percentageLevel) {
		return x -> leftTailExpectedShortFall(x, percentageLevel);
	}


	/**
	 * X &mapsto; VaR_\alpha(X)
	 *
	 * @param percentageLevel the percentage \alpha level of the value at risk.
	 * @return The operator X &mapsto; VaR_\alpha(X)
	 */
	public static RandomOperator valueAtRisk(Double percentageLevel) {
		return x -> valueAtRisk(x,percentageLevel);
	}

	/**
	 * X &mapsto; VaR_\alpha(X)
	 *
	 * @param percentageLevel the percentage \alpha level of the value at risk.
	 * @return The VaR_\alpha(X)
	 */
	public static RandomVariable valueAtRisk(RandomVariable x, Double percentageLevel) {
		final double level = validatePercentageLevel(percentageLevel);

		final double valueAtRisk = x.getQuantile(level);
		return Scalar.of(valueAtRisk);
	}


	/**
	 * ES_\alpha(X)
	 *
	 * @param percentageLevel the percentage \alpha level of the expected shortfall.
	 * @return ES_\alpha(X)
	 */
	public static RandomVariable rightTailExpectedShortFall(RandomVariable x, Double percentageLevel) {
		final double level = validatePercentageLevel(percentageLevel);
		if(x.isDeterministic() || x.getVariance() == 0) {
			return x;
		}
		if(level == 1.0) {
			return Scalar.of(x.getMax());
		}

		final double valueAtRisk = x.getQuantile(level);
		// 1(x >= VaR)
		final RandomVariable indicator = x.sub(valueAtRisk).choose(Scalar.of(1.0), Scalar.of(0.0));
		final RandomVariable averageBiggerThanVar = x.mult(indicator).average().div(1-level);

		return averageBiggerThanVar;
	}

	/**
	 * ES_\alpha(X) for a value RandomVariable where lower values are worse outcomes, i.e. we have to invert the percentage level and average all values below the percentile
	 *
	 * @param percentageLevel the percentage \alpha level of the expected shortfall.
	 * @return ES_\alpha(X)
	 */
	public static RandomVariable leftTailExpectedShortFall(RandomVariable x, Double percentageLevel) {
		final double level = validatePercentageLevel(percentageLevel);
		if(x.isDeterministic() || x.getVariance() == 0) {
			return x;
		}
		if(level == 0.0) {
			return Scalar.of(x.getMin());
		}
		if(level == 1.0) {
			return x.average(); // just return expectation
		}

		final double valueAtRisk = x.getQuantile(level);
		// 1(x <= VaR)
		final RandomVariable indicatorSmallerThanVar = Scalar.of(valueAtRisk).sub(x).choose(Scalar.of(1.0), Scalar.of(0.0));
		final RandomVariable averageSmallerThanVar = x.mult(indicatorSmallerThanVar).average().div(level);

		return averageSmallerThanVar;
	}

	private static double validatePercentageLevel(Double percentageLevel) {
		if(percentageLevel == null || !Double.isFinite(percentageLevel) || percentageLevel < 0.0 || percentageLevel > 1.0) {
			throw new IllegalArgumentException("percentageLevel must be finite and in [0, 1].");
		}
		return percentageLevel;
	}
}
