package com.javatreesearch.lombok;

// Feature: lombok-reference-support, Property 2: Detecção de anotações em campos

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import net.jqwik.api.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for LombokAnnotationDetector field annotation detection.
 *
 * **Validates: Requirements 1.2**
 */
class LombokAnnotationDetectorFieldPropertiesTest {

    private final LombokAnnotationDetector detector = new LombokAnnotationDetectorImpl();

    /**
     * Property 2: Detecção de anotações em campos
     *
     * For any FieldDeclaration containing any combination of @Getter and @Setter,
     * the LombokAnnotationDetector SHALL return exactly the annotations present on that field.
     *
     * **Validates: Requirements 1.2**
     */
    @Property(tries = 100)
    void fieldAnnotationDetectionIsExact(
            @ForAll("fieldLevelAnnotationSubsets") Set<LombokAnnotation> annotationSubset
    ) {
        String classSource = buildClassWithAnnotatedField(annotationSubset);
        CompilationUnit cu = StaticJavaParser.parse(classSource);

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        assertFalse(fields.isEmpty(), "Parsed class must contain at least one field");

        FieldDeclaration field = fields.get(0);
        Set<LombokAnnotation> detected = detector.detectFieldAnnotations(field);

        assertEquals(annotationSubset, detected,
                "Detected field annotations must equal exactly the annotations present on the field. " +
                "Expected: " + annotationSubset + ", but got: " + detected);
    }

    /**
     * Edge case: field with no annotations returns empty set.
     * Validates: Requirement 1.2
     */
    @Example
    void fieldWithNoAnnotationsReturnsEmptySet() {
        String classSource = "public class MyClass { private String name; }";
        CompilationUnit cu = StaticJavaParser.parse(classSource);

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        assertFalse(fields.isEmpty(), "Parsed class must contain at least one field");

        FieldDeclaration field = fields.get(0);
        Set<LombokAnnotation> detected = detector.detectFieldAnnotations(field);

        assertTrue(detected.isEmpty(),
                "Field with no Lombok annotations should return an empty set, but got: " + detected);
    }

    /**
     * Edge case: field with both @Getter and @Setter returns both.
     * Validates: Requirement 1.2
     */
    @Example
    void fieldWithGetterAndSetterReturnsBoth() {
        String classSource =
                "@Getter\n" +
                "@Setter\n" +
                "private String name;\n";
        String wrappedSource = "public class MyClass {\n" + classSource + "}\n";
        CompilationUnit cu = StaticJavaParser.parse(wrappedSource);

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        assertFalse(fields.isEmpty(), "Parsed class must contain at least one field");

        FieldDeclaration field = fields.get(0);
        Set<LombokAnnotation> detected = detector.detectFieldAnnotations(field);

        assertEquals(EnumSet.of(LombokAnnotation.GETTER, LombokAnnotation.SETTER), detected,
                "Field with @Getter and @Setter should return both annotations");
    }

    /**
     * Edge case: field with only @Getter returns only GETTER.
     * Validates: Requirement 1.2
     */
    @Example
    void fieldWithOnlyGetterReturnsGetter() {
        String classSource = "public class MyClass {\n@Getter\nprivate String name;\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(classSource);

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        FieldDeclaration field = fields.get(0);
        Set<LombokAnnotation> detected = detector.detectFieldAnnotations(field);

        assertEquals(EnumSet.of(LombokAnnotation.GETTER), detected,
                "Field with only @Getter should return only GETTER");
    }

    /**
     * Edge case: field with only @Setter returns only SETTER.
     * Validates: Requirement 1.2
     */
    @Example
    void fieldWithOnlySetterReturnsSetter() {
        String classSource = "public class MyClass {\n@Setter\nprivate int count;\n}\n";
        CompilationUnit cu = StaticJavaParser.parse(classSource);

        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        FieldDeclaration field = fields.get(0);
        Set<LombokAnnotation> detected = detector.detectFieldAnnotations(field);

        assertEquals(EnumSet.of(LombokAnnotation.SETTER), detected,
                "Field with only @Setter should return only SETTER");
    }

    /**
     * Generates random subsets of field-level Lombok annotations: {GETTER, SETTER}.
     * Only @Getter and @Setter are valid field-level annotations per the spec.
     */
    @Provide
    Arbitrary<Set<LombokAnnotation>> fieldLevelAnnotationSubsets() {
        List<LombokAnnotation> fieldLevelAnnotations = Arrays.asList(
                LombokAnnotation.GETTER,
                LombokAnnotation.SETTER
        );
        return Arbitraries.subsetOf(fieldLevelAnnotations)
                .map(subset -> {
                    if (subset.isEmpty()) {
                        return Collections.<LombokAnnotation>emptySet();
                    }
                    return EnumSet.copyOf(subset);
                });
    }

    /**
     * Builds a Java class source string with a single field annotated with the given annotations.
     */
    private String buildClassWithAnnotatedField(Set<LombokAnnotation> annotations) {
        StringBuilder sb = new StringBuilder();
        sb.append("public class TestClass {\n");
        for (LombokAnnotation annotation : annotations) {
            sb.append("    @").append(annotation.simpleName()).append("\n");
        }
        sb.append("    private String field;\n");
        sb.append("}\n");
        return sb.toString();
    }
}
