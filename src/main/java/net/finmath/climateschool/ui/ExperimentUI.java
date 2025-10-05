package net.finmath.climateschool.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import javafx.animation.PauseTransition;
import javafx.util.converter.NumberStringConverter;
import net.finmath.climate.models.CarbonConcentration;
import net.finmath.climate.models.ClimateModel;
import net.finmath.climate.models.Temperature;
import net.finmath.climate.models.dice.DICEModel;
import net.finmath.plots.Plot2D;
import net.finmath.plots.Plots;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public abstract class ExperimentUI extends Application {

	// A parameter
	public record Parameter(SimpleDoubleProperty value, double initial, double min, double max) {
		public Parameter(String name, double value, double min, double max) {
			this(new SimpleDoubleProperty(null, name, value), value, min, max);
		};
	}

	// The parameters
	private final List<Parameter> parameters;

    private final ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "compute-thread");
        t.setDaemon(true);
        return t;
    });
    private Future<?> currentJob;
    
	private final PauseTransition debounce = new PauseTransition(Duration.millis(300));
	private final DecimalFormat df = new DecimalFormat("#.####");

	public ExperimentUI(List<Parameter> parameters) {
		this.parameters = parameters;
	}

	abstract public void runCalculation();
	
	public void runCalculationAsync() {
		System.out.println("Starting calculation.");
		   // laufende Berechnung abbrechen
        if (currentJob != null && !currentJob.isDone()) {
    		System.out.println("Cancel previous calculation.");
        	currentJob.cancel(true);
        }

        Task<Double> task = new Task<>() {
            @Override
            protected Double call() throws Exception {
            	runCalculation();
            	return 0.0;
            }
        };

        currentJob = pool.submit(task);
	}
	
	public List<Parameter> getExperimentParameters() {
		return parameters;
	}

	/*
	 * UI Part
	 */

    // Für eigenständigen Start (Fallback):
    @Override
    public void start(Stage stage) {
        stage.setTitle("MyStandaloneDemo (Fenster)");
        stage.setScene(new Scene(createContent()));
        // Achtung: Hier KEIN Platform.exit() im onCloseRequest!
        stage.show();
    }

    // Für EMBEDDING im Host:
    public Parent createContent() {
        GridPane grid = new GridPane();
		grid.setHgap(12);
		grid.setVgap(10);
		grid.setPadding(new Insets(16));

		// Headder
		addHeader(grid);

		// Parameters
		for(int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
			addParameterRow(grid, parameterIndex+1, parameters.get(parameterIndex));
		}

		// Berechnungs-Trigger (entprellt auf 300 ms nach letzter Änderung)
		debounce.setOnFinished(e -> runCalculationAsync());

		// Buttons
		HBox buttons = new HBox(10);
		Button btnReset = new Button("Reset");
		btnReset.setOnAction(e -> resetToDefaults());
		Button btnCompute = new Button("Calculate");
		btnCompute.setOnAction(e -> runCalculationAsync());
		buttons.getChildren().addAll(btnReset, btnCompute);

		VBox root = new VBox(12, grid, buttons);
		root.setPadding(new Insets(14));
		buttons.setAlignment(Pos.CENTER_LEFT);
        
		return root;
	}

	/**
	 * Add a header row to the grid
	 * 
	 * @param grid
	 */
	private void addHeader(GridPane grid) {
		Label hName   = new Label("Parameter");
		Label hSlider = new Label("Value");
		Label hMin    = new Label("Min");
		Label hMax    = new Label("Max");

		hName.getStyleClass().add("header");
		hSlider.getStyleClass().add("header");
		hMin.getStyleClass().add("header");
		hMax.getStyleClass().add("header");

		grid.add(hName,   0, 0);
		grid.add(hSlider, 1, 0);
		grid.add(hMin,    2, 0);
		grid.add(hMax,    3, 0);

		ColumnConstraints c0 = new ColumnConstraints();
		c0.setPercentWidth(25);
		ColumnConstraints c1 = new ColumnConstraints();
		c1.setPercentWidth(55);
		ColumnConstraints c2 = new ColumnConstraints();
		c2.setPercentWidth(10);
		ColumnConstraints c3 = new ColumnConstraints();
		c3.setPercentWidth(10);
		grid.getColumnConstraints().addAll(c0, c1, c2, c3);
	}

	/**
	 * Add a parameter row to the grid
	 * @param grid The grid.
	 * @param row The row index.
	 * @param p The parameter.
	 * @return The next row index.
	 */
	private int addParameterRow(GridPane grid, int row, Parameter p) {
		double lo = Math.min(p.min(), p.max());
		double hi = Math.max(p.min(), p.max());
		SimpleDoubleProperty value = p.value();

		Label name = new Label(value.getName());
		name.setMinWidth(160);

		// Slider + Value-Textfeld gebündelt
		Slider slider = new Slider(lo, hi, value.get());
		slider.setShowTickLabels(true);
		slider.setShowTickMarks(true);
		slider.setMajorTickUnit((hi - lo) / 4.0);
		slider.setMinorTickCount(4);
		slider.setBlockIncrement((hi - lo) / 100.0);

		TextField valueField = new TextField(df.format(value.get()));
		valueField.setPrefColumnCount(8);
		HBox sliderBox = new HBox(8, slider, valueField);
		sliderBox.setAlignment(Pos.CENTER_LEFT);

		// min/max Labels
		Label minLabel = new Label(df.format(lo));
		Label maxLabel = new Label(df.format(hi));

		// Bidirektionales Binding (mit robuster Konvertierung)
		StringConverter<Number> conv = new NumberStringConverter(df);
		// Slider -> Textfeld
		valueField.textProperty().bindBidirectional(slider.valueProperty(), conv);
		// Property <-> Slider (bleibt Quelle der Wahrheit)
		value.bindBidirectional(slider.valueProperty());

		// Änderungen entprellt berechnen
		slider.valueProperty().addListener((obs, oldV, newV) -> debounce.playFromStart());
		valueField.setOnAction(e -> debounce.playFromStart());
		valueField.focusedProperty().addListener((obs, was, isNow) -> {
			if (!isNow) debounce.playFromStart(); // bei Fokus-Verlust
		});

		// Tooltip mit aktuellem Wert
		Tooltip tip = new Tooltip();
		tip.textProperty().bind(Bindings.createStringBinding(
				() -> value.getName() + ": " + df.format(slider.getValue()), slider.valueProperty()));
		Tooltip.install(slider, tip);

		grid.add(name,      0, row);
		grid.add(sliderBox, 1, row);
		grid.add(minLabel,  2, row);
		grid.add(maxLabel,  3, row);

		return row + 1;
	}

	/** Setzt alle Werte auf die ursprünglich übergebenen Startwerte (geklammert auf min/max). */
	private void resetToDefaults() {
		for (Parameter p : parameters) {
			double lo = Math.min(p.min(), p.max());
			double hi = Math.max(p.min(), p.max());
			double value = clamp(p.initial, lo, hi);
			p.value().set(value);
		}
		debounce.playFromStart();
	}

	private static double clamp(double v, double lo, double hi) {
		return Math.max(lo, Math.min(hi, v));
	}

	public static void main(String[] args) {
		// Falls du das aus einer IDE mit Modular-Setup startest,
		// achte darauf, die JavaFX-Module (controls, graphics, base) korrekt zu laden.
		launch(args);
	}
}
