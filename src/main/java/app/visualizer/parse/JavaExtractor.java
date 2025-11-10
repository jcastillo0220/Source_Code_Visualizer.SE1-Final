package app.visualizer.parse;

import app.visualizer.model.UmlModel;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class JavaExtractor {
    public UmlModel extract(Path srcRoot) throws IOException {
        UmlModel model = new UmlModel();
        Files.walk(srcRoot)
             .filter(p -> p.toString().endsWith(".java"))
             .forEach(p -> parseFile(p, model));
        inferAssociations(model);
        return model;
    }

    private void parseFile(Path file, UmlModel model) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);
            cu.findAll(TypeDeclaration.class).forEach(td -> {
                if (!(td instanceof ClassOrInterfaceDeclaration || td instanceof EnumDeclaration || td instanceof RecordDeclaration))
                    return;

                UmlModel.UmlType t = new UmlModel.UmlType();
                t.name = td.getNameAsString();
                if (td instanceof ClassOrInterfaceDeclaration ci) {
                    t.kind = ci.isInterface() ? UmlModel.UmlType.Kind.INTERFACE : UmlModel.UmlType.Kind.CLASS;
                    ci.getExtendedTypes().forEach(et -> t.extendsTypes.add(simpleName(et)));
                    ci.getImplementedTypes().forEach(it -> t.implementsTypes.add(simpleName(it)));
                    ci.getFields().forEach(f -> t.fields.add(f.getVariables().get(0).getNameAsString() + " : " + f.getElementType()));
                    ci.getMethods().forEach(m -> t.methods.add(m.getNameAsString() + "(" + String.join(", ",
                            m.getParameters().stream().map(p -> p.getNameAsString() + " : " + p.getType()).toList()) + ") : " + m.getType()));
                } else if (td instanceof EnumDeclaration) {
                    t.kind = UmlModel.UmlType.Kind.ENUM;
                } else {
                    t.kind = UmlModel.UmlType.Kind.RECORD;
                }
                model.types.put(t.name, t);
            });
        } catch (Exception e) {
            System.err.println("Failed to parse " + file + ": " + e.getMessage());
        }
    }

    private String simpleName(ClassOrInterfaceType t) {
        return t.getNameWithScope();
    }

    /** Very light association heuristic: field types referencing other known types */
    private void inferAssociations(UmlModel model) {
        for (UmlModel.UmlType t : model.types.values()) {
            for (String f : t.fields) {
                String rhs = f.substring(f.indexOf(':') + 1).trim();
                String base = rhs.replaceAll("<.*>", "").trim();
                for (String candidate : model.types.keySet()) {
                    if (base.endsWith(candidate) && !candidate.equals(t.name)) {
                        model.associations.add(new String[]{t.name, candidate, ""});
                    }
                }
            }
        }
    }
}
