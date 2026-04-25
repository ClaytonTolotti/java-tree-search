package com.javatreesearch.lombok;

import java.util.Optional;

public enum LombokAnnotation {
    DATA("Data", "lombok.Data"),
    GETTER("Getter", "lombok.Getter"),
    SETTER("Setter", "lombok.Setter"),
    BUILDER("Builder", "lombok.Builder"),
    VALUE("Value", "lombok.Value"),
    ALL_ARGS_CONSTRUCTOR("AllArgsConstructor", "lombok.AllArgsConstructor"),
    NO_ARGS_CONSTRUCTOR("NoArgsConstructor", "lombok.NoArgsConstructor"),
    REQUIRED_ARGS_CONSTRUCTOR("RequiredArgsConstructor", "lombok.RequiredArgsConstructor");

    private final String simpleName;
    private final String qualifiedName;

    LombokAnnotation(String simpleName, String qualifiedName) {
        this.simpleName = simpleName;
        this.qualifiedName = qualifiedName;
    }

    public String simpleName() { return simpleName; }
    public String qualifiedName() { return qualifiedName; }

    /** Returns true if the given annotation name (simple or qualified) matches this constant. */
    public boolean matches(String annotationName) {
        return simpleName.equals(annotationName) || qualifiedName.equals(annotationName);
    }

    /** Finds the LombokAnnotation matching the given name, or empty. */
    public static Optional<LombokAnnotation> fromName(String name) {
        for (LombokAnnotation a : values()) {
            if (a.matches(name)) return Optional.of(a);
        }
        return Optional.empty();
    }
}
