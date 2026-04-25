package com.javatreesearch.lombok;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example-based tests for LombokAnnotationDetector with concrete class and field cases.
 * Validates: Requirements 1.1, 1.2, 1.3, 1.4
 */
class LombokAnnotationDetectorExampleTest {

    private final LombokAnnotationDetector detector = new LombokAnnotationDetectorImpl();

    // -------------------------------------------------------------------------
    // Class-level annotation detection
    // -------------------------------------------------------------------------

    /**
     * Req 1.1: Class annotated with @Data → detectClassAnnotations returns {DATA}.
     */
    @Test
    void classWithDataAnnotationReturnsData() {
        String source = "@Data\npublic class Person {\n    private String name;\n    private int age;\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(source);

        Set<LombokAnnotation> detected = detector.detectClassAnnotations(cu);

        assertEquals(EnumSet.of(LombokAnnotation.DATA), detected,
                "Class annotated with @Data should return {DATA}");
    }

    /**
     * Req 1.1: Class annotated with @Builder → detectClassAnnotations returns {BUILDER}.
     */
    @Test
    void classWithBuilderAnnotationReturnsBuilder() {
        String source = "@Builder\npublic class Order {\n    private String id;\n    private double amount;\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(source);

        Set<LombokAnnotation> detected = detector.detectClassAnnotations(cu);

        assertEquals(EnumSet.of(LombokAnnotation.BUILDER), detected,
                "Class annotated with @Builder should return {BUILDER}");
    }

    /**
     * Req 1.1: Class annotated with @Value → detectClassAnnotations returns {VALUE}.
     */
    @Test
    void classWithValueAnnotationReturnsValue() {
        String source = "@Value\npublic class Coordinates {\n    double latitude;\n    double longitude;\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(source);

        Set<LombokAnnotation> detected = detector.detectClassAnnotations(cu);

        assertEquals(EnumSet.of(LombokAnnotation.VALUE), detected,
                "Class annotated with @Value should return {VALUE}");
    }

    /**
     * Req 1.4: Class annotated with @lombok.Data (qualified name) → detectClassAnnotations returns {DATA}.
     */
    @Test
    void classWithQualifiedLombokDataAnnotationReturnsData() {
        String source = "@lombok.Data\npublic class Config {\n    private String host;\n    private int port;\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(source);

        Set<LombokAnnotation> detected = detector.detectClassAnnotations(cu);

        assertEquals(EnumSet.of(LombokAnnotation.DATA), detected,
                "Class annotated with @lombok.Data (qualified name) should return {DATA}");
    }

    /**
     * Req 1.3: Class with no Lombok annotations → detectClassAnnotations returns empty set.
     */
    @Test
    void classWithNoLombokAnnotationsReturnsEmptySet() {
        String source = "public class PlainClass {\n    private String value;\n    public String getValue() { return value; }\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(source);

        Set<LombokAnnotation> detected = detector.detectClassAnnotations(cu);

        assertTrue(detected.isEmpty(),
                "Class with no Lombok annotations should return an empty set, but got: " + detected);
    }

    // -------------------------------------------------------------------------
    // Field-level annotation detection
    // -------------------------------------------------------------------------

    /**
     * Req 1.2: Field with @Getter → detectFieldAnnotations returns {GETTER}.
     */
    @Test
    void fieldWithGetterAnnotationReturnsGetter() {
        String source = "public class MyClass {\n    @Getter\n    private String name;\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(source);

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        assertFalse(fields.isEmpty(), "Parsed class must contain at least one field");

        Set<LombokAnnotation> detected = detector.detectFieldAnnotations(fields.get(0));

        assertEquals(EnumSet.of(LombokAnnotation.GETTER), detected,
                "Field with @Getter should return {GETTER}");
    }

    /**
     * Req 1.2: Field with @Setter → detectFieldAnnotations returns {SETTER}.
     */
    @Test
    void fieldWithSetterAnnotationReturnsSetter() {
        String source = "public class MyClass {\n    @Setter\n    private int count;\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(source);

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        assertFalse(fields.isEmpty(), "Parsed class must contain at least one field");

        Set<LombokAnnotation> detected = detector.detectFieldAnnotations(fields.get(0));

        assertEquals(EnumSet.of(LombokAnnotation.SETTER), detected,
                "Field with @Setter should return {SETTER}");
    }

    /**
     * Req 1.2: Field with @Getter and @Setter → detectFieldAnnotations returns {GETTER, SETTER}.
     */
    @Test
    void fieldWithGetterAndSetterReturnsBoth() {
        String source = "public class MyClass {\n    @Getter\n    @Setter\n    private boolean active;\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(source);

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        assertFalse(fields.isEmpty(), "Parsed class must contain at least one field");

        Set<LombokAnnotation> detected = detector.detectFieldAnnotations(fields.get(0));

        assertEquals(EnumSet.of(LombokAnnotation.GETTER, LombokAnnotation.SETTER), detected,
                "Field with @Getter and @Setter should return {GETTER, SETTER}");
    }

    /**
     * Req 1.3: Field with no annotations → detectFieldAnnotations returns empty set.
     */
    @Test
    void fieldWithNoAnnotationsReturnsEmptySet() {
        String source = "public class MyClass {\n    private String description;\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(source);

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        assertFalse(fields.isEmpty(), "Parsed class must contain at least one field");

        Set<LombokAnnotation> detected = detector.detectFieldAnnotations(fields.get(0));

        assertTrue(detected.isEmpty(),
                "Field with no annotations should return an empty set, but got: " + detected);
    }
}
