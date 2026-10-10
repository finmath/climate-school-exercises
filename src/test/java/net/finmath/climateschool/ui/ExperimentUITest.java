package net.finmath.climateschool.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import net.finmath.climateschool.ui.parameter.BooleanParameter;

class ExperimentUITest {

	@Test
	void resetParameterRestoresBooleanInitialValue() {
		final BooleanParameter parameter = new BooleanParameter("Show Cost", false);
		parameter.getBindableValue().set(true);

		ExperimentUI.resetParameter(parameter);

		assertFalse(parameter.getBindableValue().get());
	}
}
