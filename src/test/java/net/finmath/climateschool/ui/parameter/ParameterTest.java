package net.finmath.climateschool.ui.parameter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.finmath.climateschool.ui.parameter.BooleanParameter.BooleanParameterSpec;
import net.finmath.climateschool.ui.parameter.DoubleParameter.DoubleParameterSpec;

class ParameterTest {

	@Test
	void doubleParameterRetainsItsInitialSpecificationWhenTheValueChanges() {
		final DoubleParameter parameter = new DoubleParameter("Discount Rate", 0.03, 0.01, 0.05);
		final DoubleParameterSpec spec = parameter.getSpec();

		assertEquals("Discount Rate", parameter.getBindableValue().getName());
		assertEquals(0.03, parameter.getBindableValue().get());
		assertEquals(0.03, spec.initial());
		assertEquals(0.01, spec.min());
		assertEquals(0.05, spec.max());

		parameter.getBindableValue().set(0.04);

		assertEquals(0.04, parameter.getBindableValue().get());
		assertEquals(0.03, spec.initial());
	}

	@Test
	void booleanParameterRetainsItsInitialSpecificationWhenTheValueChanges() {
		final BooleanParameter parameter = new BooleanParameter("Show Cost", false);
		final BooleanParameterSpec spec = parameter.getSpec();

		assertEquals("Show Cost", parameter.getBindableValue().getName());
		assertFalse(parameter.getBindableValue().get());
		assertFalse(spec.initial());

		parameter.getBindableValue().set(true);

		assertTrue(parameter.getBindableValue().get());
		assertFalse(spec.initial());
	}
}
