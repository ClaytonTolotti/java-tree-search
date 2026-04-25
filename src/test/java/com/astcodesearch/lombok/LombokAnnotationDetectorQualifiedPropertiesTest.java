package com.javatreesearch.lombok;

// Feature: lombok-reference-support, Property 3: Reconhecimento de nomes simples e qualificados

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import net.jqwik.api.*;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for LombokAnnotationDetector recognition of simple and qualified annotation names.
 *
 * **Validates: Requirements 1.4**
 */
class LombokAnnotationDetectorQualifiedPropertiesTest {

    private final LombokAnnotationDetector detector = new LombokAnnotationDetectorImpl();

    /**
     * Property 3: Reconhecimento de nomes simples e qualificados
     *
     * For any supported Lombok annotation, both the simple name form (e.g., @Data) and the
     * qualified name form (e.g., @lombok.Data) SHALL be recognized as the same LombokAnnotation.
     *
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    void simpleAndQualifiedNamesAreRecognizedAsTheSameAnnotation(
            @ForAll LombokAnnotation annotation
    ) {
        String simpleNameSource = buildClassWithSimpleName(annotation);
        String qualifiedNameSource = buildClassWithQualifiedName(annotation);

        CompilationUnit cuSimple = StaticJavaParser.parse(simpleNameSource);
        CompilationUnit cuQualified = StaticJavaParser.parse(qualifiedNameSource);

        Set<LombokAnnotation> detectedSimple = detector.detectClassAnnotations(cuSimple);
        Set<LombokAnnotation> detectedQualified = detector.detectClassAnnotations(cuQualified);

        assertTrue(detectedSimple.contains(annotation),
                "Simple name @" + annotation.simpleName() + " should be recognized as " + annotation +
                ", but detected: " + detectedSimple);

        assertTrue(detectedQualified.contains(annotation),
                "Qualified name @" + annotation.qualifiedName() + " should be recognized as " + annotation +
                ", but detected: " + detectedQualified);

        assertEquals(detectedSimple, detectedQualified,
                "Simple name and qualified name should produce the same detection result for " + annotation);
    }

    /**
     * Builds a Java class source string annotated with the simple name form (e.g., @Data).
     */
    private String buildClassWithSimpleName(LombokAnnotation annotation) {
        return "@" + annotation.simpleName() + "\n" +
               "public class TestClass {\n" +
               "    private String field;\n" +
               "}\n";
    }

    /**
     * Builds a Java class source string annotated with the qualified name form (e.g., @lombok.Data).
     */
    private String buildClassWithQualifiedName(LombokAnnotation annotation) {
        return "@" + annotation.qualifiedName() + "\n" +
               "public class TestClass {\n" +
               "    private String field;\n" +
               "}\n";
    }
}
