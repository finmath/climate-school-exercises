package net.finmath.climateschool.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;

class RandomOperatorsTest {

	private final RandomVariable sample = new RandomVariableFromDoubleArray(0.0, new double[] { -2.0, 1.0, 5.0 });

	@Test
	void expectedShortfallUsesFiniteLimitValuesAtBoundaries() {
		assertEquals(-2.0, RandomOperators.leftTailExpectedShortFall(sample, 0.0).doubleValue(), 1E-12);
		assertEquals(5.0, RandomOperators.rightTailExpectedShortFall(sample, 1.0).doubleValue(), 1E-12);
	}

	@Test
	void leftTailExpectedShortfallIncludesTheValueAtRiskObservation() {
		assertEquals(-2.0, RandomOperators.leftTailExpectedShortFall(sample, 1.0 / 3.0).doubleValue(), 1E-12);
	}

	@Test
	void tailRiskRejectsLevelsOutsideTheUnitInterval() {
		assertThrows(IllegalArgumentException.class,
				() -> RandomOperators.leftTailExpectedShortFall(sample, -0.01));
		assertThrows(IllegalArgumentException.class,
				() -> RandomOperators.rightTailExpectedShortFall(sample, 1.01));
		assertThrows(IllegalArgumentException.class,
				() -> RandomOperators.valueAtRisk(sample, Double.NaN));
	}
}
