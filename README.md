# UML Visualizer (Java → PlantUML)

A minimal Java CLI that parses a Java `src/` folder, extracts classes/interfaces/enums/records and relationships, and emits a PlantUML class diagram file (`diagram.puml`).

## Build
```bash
cd uml-visualizer
mvn -q -DskipTests package
```

## Run
```bash
# Example: parse a local src folder and write diagram.puml
java -cp target/uml-visualizer-0.1.0.jar:$(mvn -q -Dexec.classpathScope=runtime -DincludeScope=runtime -Dexpression=project.runtimeClasspathElements --non-recursive -DforceStdout org.codehaus.mojo:exec-maven-plugin:3.1.0:evaluate) app.visualizer.Main ./example-src diagram.puml
```

Or simpler if you install PlantUML and just want the .puml to open in an IDE:

```bash
java -jar target/uml-visualizer-0.1.0.jar ./example-src diagram.puml
```

> If the shaded JAR isn't built, you can run from your IDE with Program Args: `<path-to-src> diagram.puml`.

## Render PNG/SVG
Use PlantUML:
```bash
plantuml diagram.puml
# Produces diagram.png
```

## Notes
- Associations are inferred when a field type references another known type (very lightweight heuristic).
- Improve accuracy by expanding `inferAssociations` and adding visibility/static/abstract parsing.


## IntelliJ IDEA quick start
1. File → Open → select the `uml-visualizer` folder (Maven project).
2. Ensure Project SDK is set to JDK 17 (File → Project Structure → Project).
3. Build: View → Tool Windows → Maven → Lifecycle → package (produces `target/uml-visualizer-0.1.0-all.jar`).
4. Run: Run → Edit Configurations… → + Application → Main class: `app.visualizer.Main`; Program arguments: `./example-src diagram.puml`.
5. View the `.puml`: install the PlantUML plugin (Settings → Plugins → Marketplace → "PlantUML Integration"), then open `diagram.puml`.
