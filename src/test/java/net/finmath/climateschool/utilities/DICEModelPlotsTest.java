package net.finmath.climateschool.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DICEModelPlotsTest {

	@Test
	void rollingStatisticsUseTheSameShortWindowForCostsAndGDP() {
		final DICEModelPlots.RollingCostStatistics statistics = DICEModelPlots.calculateRollingCostStatistics(
				2,
				4,
				index -> new double[] { 10.0, 20.0, 30.0, 40.0 }[index],
				index -> new double[] { 1.0, 2.0, 10.0, 20.0 }[index]);

		assertEquals(35.0, statistics.averageDiscountedCost(), 1E-12);
		assertEquals(70.0 / 30.0, statistics.discountedCostPerGDP(), 1E-12);
	}

	@Test
	void rollingStatisticsRejectAnEmptyWindow() {
		assertThrows(IllegalArgumentException.class,
				() -> DICEModelPlots.calculateRollingCostStatistics(3, 3, index -> 1.0, index -> 1.0));
	}
}
