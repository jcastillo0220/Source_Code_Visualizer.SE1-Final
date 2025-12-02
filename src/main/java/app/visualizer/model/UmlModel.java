package app.visualizer.model;

import java.util.*;

//Created By: Javier Castillo
public class UmlModel {
    public static class UmlType {
        public enum Kind { CLASS, INTERFACE, ENUM, RECORD }
        public String name;
        public Kind kind;
        public List<String> fields = new ArrayList<>();
        public List<String> methods = new ArrayList<>();
        public List<String> extendsTypes = new ArrayList<>();
        public List<String> implementsTypes = new ArrayList<>();
    }

    public Map<String, UmlType> types = new LinkedHashMap<>();
    public List<String[]> associations = new ArrayList<>(); // [fromType, toType, label]
}
