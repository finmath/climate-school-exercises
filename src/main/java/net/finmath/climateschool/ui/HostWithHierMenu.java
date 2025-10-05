package net.finmath.climateschool.ui;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Supplier;

public class HostWithHierMenu extends Application {

	class ExperimentApplication {
		private Supplier<Parent> constructor;
		private int cacheDepth;
		
		
	}

	// --- Dein Modell: Blatt kann Runnable, Supplier<Parent> oder Class<? extends Application> sein
    private final Map<String, Object> model = Map.of(
        "DICE Model (Climate School)", Map.of(
            // 1) Einbetten via Supplier<Parent>
            "Embedded Demo", (Supplier<Parent>) () -> new StackPane(new Label("Hallo eingebettet!")),
            // 2) Standalone-Application-Klasse – Host versucht createContent(), sonst neues Fenster
            "One Parametric Abatemet Model", (Supplier<Parent>) () -> new DICEAbatementTimeExperimentUI().createContent(),
            // 2) Standalone-Application-Klasse – Host versucht createContent(), sonst neues Fenster
            "One Parametric Abatemet Model (new Window)", DICEAbatementTimeExperimentUI.class,
            // 2) Standalone-Application-Klasse – Host versucht createContent(), sonst neues Fenster
            "One Parametric Abatemet Model, Calibrated", (Supplier<Parent>) () -> new DICECalibrationOneParameterExperimentUI().createContent(),
            // 2) Standalone-Application-Klasse – Host versucht createContent(), sonst neues Fenster
            "Full Abatemet Model, Calibrated", (Supplier<Parent>) () -> new DICECalibrationExperimentUI().createContent()
        ),
        "Info", Map.of(
            "Beispiel-Action", (Runnable) () -> System.out.println("Action ausgeführt"))
    );

    private final BorderPane root = new BorderPane(new Label("…Hauptinhalt…"));

    // Eintrag hält Label + beliebige Payload
    private record Entry(String label, Object payload) {}

    @Override public void start(Stage stage) {
        // Tree links
        TreeItem<Entry> rootItem = toTree("ROOT", model);
        TreeView<Entry> tree = new TreeView<>(rootItem);
        tree.setShowRoot(false);
        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override protected void updateItem(Entry e, boolean empty) {
                super.updateItem(e, empty);
                setText(empty || e == null ? null : e.label());
            }
        });
        rootItem.getChildren().forEach(it -> it.setExpanded(true));

        tree.setOnMouseClicked(e -> runIfLeaf(tree.getSelectionModel().getSelectedItem(), stage));

        root.setLeft(tree);

        Scene scene = new Scene(root, 900, 520);
        stage.setScene(scene);
        stage.setTitle("Host mit sichtbarem hierarchischem Menü");
        stage.show();
    }

    // --- Aktionen je nach Payload -------------------------------------------
    private void runIfLeaf(TreeItem<Entry> item, Stage owner) {
        if (item == null) return;
        boolean isLeaf = item.getChildren().isEmpty();
        if (!isLeaf) { item.setExpanded(!item.isExpanded()); return; }

        Object payload = item.getValue().payload();
        try {
            if (payload instanceof Runnable r) {
                r.run();
            } else if (payload instanceof Supplier<?> s) {
                Object obj = s.get();
                if (obj instanceof Parent p) showInCenter(p);
                else throw new IllegalArgumentException("Supplier must return Parent: " + obj);
            } else if (payload instanceof Class<?> c && Application.class.isAssignableFrom(c)) {
                // 1) Versuch: statische createContent() finden und einbetten
                Parent embedded = tryStaticContent((Class<?>) c);
                if (embedded != null) {
                    showInCenter(embedded);
                } else {
                    // 2) Fallback: neues Fenster starten
                    @SuppressWarnings("unchecked")
                    Class<? extends Application> appClass = (Class<? extends Application>) c;
                    Application app = appClass.getDeclaredConstructor().newInstance();
                    Stage s = new Stage();
                    s.initOwner(owner);
                    app.start(s);
                    s.show();
                }
            } else {
                throw new IllegalArgumentException("Unsupported payload: " + payload);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Fehler beim Laden: " + ex.getMessage()).showAndWait();
        }
    }

    private void showInCenter(Parent content) {
        root.setCenter(content);
        BorderPane.setMargin(content, new javafx.geometry.Insets(8));
    }

    // Sucht zero-arg static Methode, die Parent zurückgibt (z.B. createContent())
    private Parent tryStaticContent(Class<?> cls) {
        for (String name : new String[]{"createContent", "content", "buildUI"}) {
            try {
                Method m = cls.getDeclaredMethod(name);
                if (Parent.class.isAssignableFrom(m.getReturnType())) {
                    Object res = m.invoke(null);
                    return (Parent) res;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                throw new RuntimeException("Fehler beim Aufruf von " + name + "()", e);
            }
        }
        return null;
    }

    // --- Map -> Tree ---------------------------------------------------------
    @SuppressWarnings("unchecked")
    private TreeItem<Entry> toTree(String label, Object node) {
        if (node instanceof Map<?,?> m) {
            TreeItem<Entry> parent = new TreeItem<>(new Entry(label, null));
            m.forEach((k, v) -> parent.getChildren().add(toTree(String.valueOf(k), v)));
            return parent;
        }
        // Blatt: alles andere
        return new TreeItem<>(new Entry(label, node));
    }

    // --- Beispiel-Standalone-Demo -------------------------------------------
    public static class MyStandaloneDemo extends Application {
        // Für EMBEDDING im Host:
        public static Parent createContent() {
            StackPane p = new StackPane();
            p.setPrefSize(600, 400);
            Rectangle r = new Rectangle(320, 180, Color.web("#eaf2ff"));
            r.setStroke(Color.web("#6aa0ff")); r.setArcWidth(18); r.setArcHeight(18);
            p.getChildren().addAll(r, new Label("Ich bin eingebettet :-)"));
            return p;
        }
        // Für eigenständigen Start (Fallback):
        @Override public void start(Stage stage) {
            stage.setTitle("MyStandaloneDemo (Fenster)");
            stage.setScene(new Scene(createContent()));
            // Achtung: Hier KEIN Platform.exit() im onCloseRequest!
            stage.show();
        }
    }

    public static void main(String[] args) { launch(args); }
}
