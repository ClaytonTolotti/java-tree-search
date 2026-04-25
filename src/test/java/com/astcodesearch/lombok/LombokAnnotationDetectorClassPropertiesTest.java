package com.javatreesearch.lombok;

// Feature: lombok-reference-support, Property 1: Detecção completa de anotações de classe

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import net.jqwik.api.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for LombokAnnotationDetector class annotation detection.
 * Validates: Requirements 1.1, 1.3
 */
class LombokAnnotationDetectorClassPropertiesTest {

    private final LombokAnnotationDetector detector = new LombokAnnotationDetectorImpl();

    /**
     * Property 1: Detecção completa de anotações de classe
     *
     * For any CompilationUnit containing an arbitrary subset of Lombok annotations on class
     * declarations, the LombokAnnotationDetector SHALL return exactly the set of LombokAnnotations
     * present — neither more nor less.
     *
     * Validates: Requirements 1.1, 1.3
     */
    @Property(tries = 100)
    void classAnnotationDetectionIsComplete(
            @ForAll("lombokAnnotationSubsets") Set<LombokAnnotation> annotationSubset
    ) {
        String classSource = buildClassWithAnnotations(annotationSubset);
        CompilationUnit cu = StaticJavaParser.parse(classSource);

        Set<LombokAnnotation> detected = detector.detectClassAnnotations(cu);

        assertEquals(annotationSubset, detected,
                "Detected annotations must equal exactly the annotations present in the class. " +
                "Expected: " + annotationSubset + ", but got: " + detected);
    }

    /**
     * Edge case: a class with no Lombok annotations should return an empty set.
     * Validates: Requirement 1.3
     */
    @Example
    void classWithNoLombokAnnotationsReturnsEmptySet() {
        String classSource = "public class MyClass { private String name; }";
        CompilationUnit cu = StaticJavaParser.parse(classSource);

        Set<LombokAnnotation> detected = detector.detectClassAnnotations(cu);

        assertTrue(detected.isEmpty(),
                "Class with no Lombok annotations should return an empty set, but got: " + detected);
    }

    /**
     * Edge case: empty CompilationUnit (no class declarations) should return an empty set.
     * Validates: Requirement 1.3
     */
    @Example
    void emptyCompilationUnitReturnsEmptySet() {
        String classSource = "// just a comment\n";
        CompilationUnit cu = StaticJavaParser.parse(classSource);

        Set<LombokAnnotation> detected = detector.detectClassAnnotations(cu);

        assertTrue(detected.isEmpty(),
                "Empty CompilationUnit should return an empty set");
    }

    /**
     * Edge case: class with all Lombok annotations should return all of them.
     * Validates: Requirement 1.1
     */
    @Example
    void classWithAllLombokAnnotationsReturnsAll() {
        Set<LombokAnnotation> allAnnotations = EnumSet.allOf(LombokAnnotation.class);
        String classSource = buildClassWithAnnotations(allAnnotations);
        CompilationUnit cu = StaticJavaParser.parse(classSource);

        Set<LombokAnnotation> detected = detector.detectClassAnnotations(cu);

        assertEquals(allAnnotations, detected,
                "Class with all Lombok annotations should return all of them");
    }

    /**
     * Generates random subsets of LombokAnnotation values, including the empty set.
     */
    @Provide
    Arbitrary<Set<LombokAnnotation>> lombokAnnotationSubsets() {
        List<LombokAnnotation> allValues = Arrays.asList(LombokAnnotation.values());
        return Arbitraries.subsetOf(allValues)
                .map(subset -> {
                    if (subset.isEmpty()) {
                        return Collections.<LombokAnnotation>emptySet();
                    }
                    return EnumSet.copyOf(subset);
                });
    }

    /**
     * Builds a Java class source string annotated with the given Lombok annotations
     * using their simple names (e.g., @Data, @Getter).
     */
    private String buildClassWithAnnotations(Set<LombokAnnotation> annotations) {
        StringBuilder sb = new StringBuilder();
        for (LombokAnnotation annotation : annotations) {
            sb.append("@").append(annotation.simpleName()).append("\n");
        }
        sb.append("public class TestClass {\n");
        sb.append("    private String field;\n");
        sb.append("}\n");
        return sb.toString();
    }
}
