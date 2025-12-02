package app.visualizer;

import app.visualizer.model.UmlModel;
import app.visualizer.parse.JavaExtractor;
import app.visualizer.render.PlantUmlRenderer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

//Old MAIN CLASS: Jose Torres
public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java -jar uml-visualizer.jar <path-to-src> [out.puml]");
            return;
        }

        Path src = Paths.get(args[0]);
        Path out = (args.length > 1) ? Paths.get(args[1]) : Paths.get("diagram.puml");

        JavaExtractor extractor = new JavaExtractor();
        UmlModel model = extractor.extract(src);

        String puml = new PlantUmlRenderer().toPlantUml(model);
        Files.writeString(out, puml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("Wrote " + out.toAbsolutePath());
        System.out.println("Open with PlantUML or render via: plantuml " + out.getFileName());
    }
}


