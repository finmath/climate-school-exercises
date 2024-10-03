package net.finmath.climateschool.begin;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;

/**
 * Run this class to test your setup.
 */
public class Test2 {

	public static void main(String[] args) {

		RandomVariable randomVariable = new RandomVariableFromDoubleArray(0.0, new double[] { 1.0, 0.0, 0.0, 1.0 });
		
		double expectation = randomVariable.expectation().doubleValue();
		
		if(expectation == 0.5) {
			System.out.println("\n\tLooks as if it works!");
		}
		else {
			System.out.println("\n\tExpectation does not match.");
		}
			
	}

}
