package app.visualizer.gui;

import app.visualizer.model.UmlModel;
import app.visualizer.parse.JavaExtractor;
import app.visualizer.render.PlantUmlRenderer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import net.sourceforge.plantuml.SourceStringReader;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

//To run it in the terminal
//mvn clean javafx:run


//Setting up Application: Mario Rodriguez
public class UmlGuiApp extends Application {

    private TextField sourcePathField;
    private TextField outputPumlField;
    private TextArea pumlArea;
    private ListView<String> legendList;
    private ImageView preview;
    private Label status;
    private CheckBox useElkLayout;
    private Button generateBtn;
    private Button exportPngBtn;
    private Path tempWorkDir;
    private String lastSymbolClicked = null;
    private int lastMatchIndex = -1;

    //Fixing buttons into place: Mario Rodriguez
    @Override
    public void start(Stage stage) throws Exception {
        tempWorkDir = Files.createTempDirectory("umlviz-");
        stage.setTitle("UML Visualizer");

        // === Top bar ===
        Button chooseBtn = new Button("Choose Folder…");
        chooseBtn.setOnAction(e -> chooseFolder(stage));

        sourcePathField = new TextField();
        sourcePathField.setPromptText("Path to Java source folder (or drop a folder/.zip here)");
        HBox.setHgrow(sourcePathField, Priority.ALWAYS);

        outputPumlField = new TextField("diagram.puml");
        outputPumlField.setPrefWidth(220);

        useElkLayout = new CheckBox("Use ELK layout (no Graphviz)");
        useElkLayout.setSelected(true);

        generateBtn = new Button("Generate UML");
        generateBtn.setDefaultButton(true);
        generateBtn.setOnAction(e -> generateUml());

        exportPngBtn = new Button("Export PNG…");
        exportPngBtn.setDisable(true);
        exportPngBtn.setOnAction(e -> exportPng(stage));

        HBox top = new HBox(8, chooseBtn, sourcePathField, new Label("Out:"), outputPumlField, useElkLayout, generateBtn, exportPngBtn);
        top.setPadding(new Insets(8));

        // === Center split: PlantUML text | Preview image ===
        pumlArea = new TextArea();
        pumlArea.setPromptText("Generated PlantUML will appear here…");
        pumlArea.setWrapText(false);
        pumlArea.setMinHeight(500);

        // === Legend ===
        legendList = new ListView<>();
        legendList.setPrefHeight(150);
        legendList.setStyle("-fx-font-family: 'monospace'; -fx-font-size: 12px;");
        legendList.setOnMouseClicked(event -> {
            String selected = legendList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                String symbol = selected.split(":")[0].trim();
                if (symbol.equals("C") || symbol.equals("I")) {
                    setStatus("Legend item '" + symbol + "' is informational only.");
                    return;
                }
                highlightNextSymbolInPuml(symbol);
            }
        });

        preview = new ImageView();
        preview.setPreserveRatio(true);
        preview.setSmooth(true);

        Group zoomGroup = new Group(preview);
        ScrollPane scrollPane = new ScrollPane(zoomGroup);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);

        preview.setOnScroll(event -> {
            double delta = event.getDeltaY();
            double scale = preview.getScaleX() + delta / 400;
            scale = Math.max(0.1, Math.min(scale, 10)); // clamp scale

            preview.setScaleX(scale);
            preview.setScaleY(scale);

            // Scroll bars will shrink automatically as content grows
            event.consume();
        });

        scrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.isControlDown()) {
                double zoomFactor = (event.getDeltaY() > 0) ? 1.1 : 0.9;

                Bounds viewportBounds = scrollPane.getViewportBounds();
                Bounds contentBounds = zoomGroup.localToScene(zoomGroup.getBoundsInLocal());

                double mouseX = event.getSceneX();
                double mouseY = event.getSceneY();

                double offsetX = mouseX - contentBounds.getMinX();
                double offsetY = mouseY - contentBounds.getMinY();

                zoomGroup.setScaleX(zoomGroup.getScaleX() * zoomFactor);
                zoomGroup.setScaleY(zoomGroup.getScaleY() * zoomFactor);

                scrollPane.layout();

                double newWidth = zoomGroup.getBoundsInParent().getWidth();
                double newHeight = zoomGroup.getBoundsInParent().getHeight();

                double dx = offsetX * (zoomFactor - 1);
                double dy = offsetY * (zoomFactor - 1);

                double newH = (scrollPane.getHvalue() * (newWidth - viewportBounds.getWidth()) + dx)
                        / (newWidth - viewportBounds.getWidth());
                double newV = (scrollPane.getVvalue() * (newHeight - viewportBounds.getHeight()) + dy)
                        / (newHeight - viewportBounds.getHeight());

                scrollPane.setHvalue(Math.max(0, Math.min(newH, 1)));
                scrollPane.setVvalue(Math.max(0, Math.min(newV, 1)));

                event.consume();
            }
        });

        VBox left = new VBox(new Label("PlantUML"), pumlArea, new Label("Legend"), legendList);
        left.setSpacing(4);
        VBox right = new VBox(new Label("Preview"), scrollPane);
        left.setSpacing(4);
        right.setSpacing(4);

        SplitPane split = new SplitPane(left, right);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.45);

        // === Bottom status bar ===
        status = new Label("Ready.");
        HBox bottom = new HBox(status);
        bottom.setPadding(new Insets(6));

        // === Root layout ===
        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(split);
        root.setBottom(bottom);

        // Drag & Drop (folder or .zip)
        root.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        root.setOnDragDropped(event -> {
            var db = event.getDragboard();
            if (db.hasFiles()) {
                Path f = db.getFiles().get(0).toPath();
                try {
                    if (Files.isDirectory(f)) {
                        sourcePathField.setText(f.toAbsolutePath().toString());
                        setStatus("Selected: " + f.toAbsolutePath());
                    } else if (f.toString().toLowerCase().endsWith(".zip")) {
                        Path unzipped = unzipToTemp(f, tempWorkDir);
                        sourcePathField.setText(unzipped.toAbsolutePath().toString());
                        setStatus("Unzipped to: " + unzipped.toAbsolutePath());
                    } else {
                        setStatus("Unsupported drop: please drop a folder or .zip");
                    }
                } catch (IOException ex) {
                    setStatus("Failed to process drop: " + ex.getMessage());
                }
            }
            event.setDropCompleted(true);
            event.consume();
        });

        stage.setScene(new Scene(root, 1200, 800));
        stage.show();
    }

    private void chooseFolder(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Source Folder (contains .java files)");
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            sourcePathField.setText(dir.getAbsolutePath());
            setStatus("Selected: " + dir.getAbsolutePath());
        }
    }

    private void generateUml() {
        Path src = Paths.get(sourcePathField.getText().trim());
        if (sourcePathField.getText().trim().isEmpty()) {
            setStatus("Please choose a source folder (or drop one).");
            return;
        }
        if (!Files.exists(src) || !Files.isDirectory(src)) {
            setStatus("Source path does not exist or is not a directory.");
            return;
        }
        String outName = outputPumlField.getText().trim().isEmpty() ? "diagram.puml" : outputPumlField.getText().trim();

        generateBtn.setDisable(true);
        exportPngBtn.setDisable(true);
        setStatus("Parsing…");

        Task<Void> task = new Task<>() {
            String pumlText;
            Image image;

            @Override
            protected Void call() throws Exception {
                // 1) Extract model
                JavaExtractor extractor = new JavaExtractor();
                UmlModel model = extractor.extract(src);

                // 2) Render PlantUML text
                PlantUmlRenderer renderer = new PlantUmlRenderer();
                pumlText = renderer.toPlantUml(model);

                // Optional ELK layout to avoid Graphviz dependency
                if (useElkLayout.isSelected()) {
                    pumlText = pumlText.replace("@startuml", "@startuml\n!pragma layout elk");
                }

                // 3) Save .puml
                Files.writeString(Paths.get(outName), pumlText,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                // 4) Render PNG in-memory (no getMetadata() calls)
                try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                    SourceStringReader reader = new SourceStringReader(pumlText);
                    reader.outputImage(os); // write PNG bytes into the stream
                    byte[] bytes = os.toByteArray();
                    if (bytes.length > 0) {
                        try (ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
                            image = new Image(is);
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("Preview rendering failed: " + t.getMessage());
                }
                return null;
            }

            @Override
            protected void succeeded() {
                pumlArea.setText(pumlText);
                ObservableList<String> legendItems = FXCollections.observableArrayList();
                for (Map.Entry<String, String> entry : UML_SYMBOLS.entrySet()) {
                    if (pumlText.contains(entry.getKey())) {
                        legendItems.add(String.format("%-6s : %s", entry.getKey(), entry.getValue()));
                    }
                }
                legendList.setItems(legendItems);
                if (image != null) {
                    preview.setImage(image);
                    preview.setFitWidth(800);
                    preview.setFitHeight(800);
                    preview.setPreserveRatio(true);
                    preview.setSmooth(true);
                    preview.setCache(true);
                    exportPngBtn.setDisable(false);
                    setStatus("Done. Saved " + Paths.get(outName).toAbsolutePath());
                } else {
                    setStatus("Done. (Preview unavailable; PUML saved to " + Paths.get(outName).toAbsolutePath() + ")");
                }
                generateBtn.setDisable(false);
            }

            @Override
            protected void failed() {
                setStatus("Failed: " + getException().getMessage());
                generateBtn.setDisable(false);
            }
        };

        Thread t = new Thread(task, "uml-generate-thread");
        t.setDaemon(true);
        t.start();
    }

    private void exportPng(Stage stage) {
        if (preview.getImage() == null) {
            setStatus("No preview image to export.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fc.setInitialFileName("diagram.png");
        File f = fc.showSaveDialog(stage);
        if (f != null) {
            try (OutputStream os = new FileOutputStream(f)) {
                // Re-render from current PlantUML text to the chosen file location
                SourceStringReader reader = new SourceStringReader(pumlArea.getText());
                reader.outputImage(os); // no getMetadata()
                setStatus("Exported: " + f.getAbsolutePath());
            } catch (IOException ex) {
                setStatus("Export failed: " + ex.getMessage());
            }
        }
    }

    private static Path unzipToTemp(Path zip, Path tempDir) throws IOException {
        Path dest = tempDir.resolve(zip.getFileName().toString().replaceFirst("\\.zip$", ""));
        Files.createDirectories(dest);
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = dest.resolve(entry.getName()).normalize();
                if (!out.startsWith(dest)) continue; // guard against zip-slip
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        return dest;
    }

    private static final Map<String, String> UML_SYMBOLS = Map.ofEntries(
            Map.entry("-->", "Association"),
            Map.entry("<--", "Association (reverse)"),
            Map.entry("<|--", "Inheritance (extends)"),
            Map.entry("<|..", "Interface implementation"),
            Map.entry("..>", "Dependency"),
            Map.entry("*--", "Composition"),
            Map.entry("o--", "Aggregation"),
            Map.entry("C", "Concrete class"),
            Map.entry("I", "Interface")
    );

    private void highlightNextSymbolInPuml(String symbol) {
        String text = pumlArea.getText();
        int startIndex = 0;

        if (symbol.equals(lastSymbolClicked)) {
            startIndex = lastMatchIndex + 1;
        } else {
            lastSymbolClicked = symbol;
            lastMatchIndex = -1;
        }

        int nextIndex = text.indexOf(symbol, startIndex);
        if (nextIndex >= 0) {
            pumlArea.selectRange(nextIndex, nextIndex + symbol.length());
            pumlArea.requestFocus();
            lastMatchIndex = nextIndex;

            String relationship = extractRelationship(text, symbol, nextIndex);
            setStatus(relationship != null ? relationship : "Found '" + symbol + "' in diagram.");
        } else {
            nextIndex = text.indexOf(symbol);
            if (nextIndex >= 0) {
                pumlArea.selectRange(nextIndex, nextIndex + symbol.length());
                pumlArea.requestFocus();
                lastMatchIndex = nextIndex;

                String relationship = extractRelationship(text, symbol, nextIndex);
                setStatus(relationship != null ? relationship : "Wrapped to first '" + symbol + "' in diagram.");
            } else {
                setStatus("Symbol '" + symbol + "' not found.");
                lastMatchIndex = -1;
            }
        }
    }

    private String extractRelationship(String text, String symbol, int index) {
        int lineStart = text.lastIndexOf("\n", index);
        int lineEnd = text.indexOf("\n", index);
        if (lineStart == -1) lineStart = 0;
        if (lineEnd == -1) lineEnd = text.length();

        String line = text.substring(lineStart, lineEnd).trim();

        Pattern pattern = Pattern.compile("(\\w+)\\s*(<\\|--|<\\|\\.\\.|<--|-->|\\*--|o--|<\\.\\.|\\.\\.>|--\\*|--o)\\s*(\\w+)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String left = matcher.group(1);
            String arrow = matcher.group(2);
            String right = matcher.group(3);
            return describeRelationship(left, arrow, right);
        }

        return null;
    }

    private String describeRelationship(String left, String arrow, String right) {
        switch (arrow) {
            case "-->": case "<--": return left + " is associated with " + right;
            case "<|--": return right + " inherits from " + left;
            case "<|..": return right + " implements interface " + left;
            case "..>": case "<..": return right + " depends on " + left;
            case "*--": case "--*": return left + " is composed of " + right;
            case "o--": case "--o": return left + " aggregates " + right;
            default: return left + " relates to " + right;
        }
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> status.setText(msg));
    }

    public static void main(String[] args) {
        launch(args);
    }
}