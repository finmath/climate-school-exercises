module net.finmath.climateschool {

	exports net.finmath.climateschool.ui;
	exports net.finmath.climateschool.ui.parameter;

	requires net.finmath.lib;
	requires net.finmath.plots;

	requires transitive javafx.controls;
	requires javafx.graphics;
	requires java.prefs;
	requires javafx.base;
}
