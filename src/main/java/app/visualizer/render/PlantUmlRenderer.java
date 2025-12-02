package app.visualizer.render;

import app.visualizer.model.UmlModel;

//Created by: Joaquin Castillo
public class PlantUmlRenderer {
    public String toPlantUml(UmlModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam classAttributeIconSize 0\n");
        model.types.values().forEach(t -> {
            switch (t.kind) {
                case INTERFACE -> sb.append("interface ").append(t.name).append(" {\n");
                case ENUM      -> sb.append("enum ").append(t.name).append(" {\n");
                case RECORD    -> sb.append("class ").append(t.name).append(" <<record>> {\n");
                default        -> sb.append("class ").append(t.name).append(" {\n");
            }
            t.fields.forEach(f -> sb.append("  ").append(f).append("\n"));
            t.methods.forEach(m -> sb.append("  ").append(m).append("\n"));
            sb.append("}\n");
            t.extendsTypes.forEach(x -> sb.append(x).append(" <|-- ").append(t.name).append("\n"));
            t.implementsTypes.forEach(x -> sb.append(x).append(" <|.. ").append(t.name).append("\n"));
        });
        model.associations.forEach(a -> sb.append(a[0]).append(" --> ").append(a[1]).append("\n"));
        sb.append("@enduml\n");
        return sb.toString();
    }
}
