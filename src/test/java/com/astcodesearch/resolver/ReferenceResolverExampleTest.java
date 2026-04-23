package com.javatreesearch.resolver;

import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.NameExpr;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example-based tests for ReferenceResolverImpl.
 * Validates: Requirements 3.2, 3.3
 */
class ReferenceResolverExampleTest {

    private final ReferenceResolverImpl resolver = new ReferenceResolverImpl();
    private final Path testFile = Path.of("Test.java");

    @Test
    void variableNodeResolvesToVariable() {
        CompilationUnit cu = StaticJavaParser.parse(
            "public class Foo { void bar() { int myVar = 42; } }"
        );
        VariableDeclarator vd = cu.findFirst(VariableDeclarator.class,
            v -> v.getNameAsString().equals("myVar")).orElseThrow();

        Optional<Reference> ref = resolver.resolveFromNode(vd, testFile);

        assertTrue(ref.isPresent());
        assertEquals("myVar", ref.get().name());
        assertEquals(ReferenceType.VARIABLE, ref.get().type());
        assertEquals(testFile, ref.get().file());
        assertTrue(ref.get().line() > 0);
        assertTrue(ref.get().column() >= 0);
    }

    @Test
    void methodNodeResolvesToMethod() {
        CompilationUnit cu = StaticJavaParser.parse(
            "public class Foo { public void myMethod() {} }"
        );
        MethodDeclaration md = cu.findFirst(MethodDeclaration.class,
            m -> m.getNameAsString().equals("myMethod")).orElseThrow();

        Optional<Reference> ref = resolver.resolveFromNode(md, testFile);

        assertTrue(ref.isPresent());
        assertEquals("myMethod", ref.get().name());
        assertEquals(ReferenceType.METHOD, ref.get().type());
    }

    @Test
    void classNodeResolvesToClass() {
        CompilationUnit cu = StaticJavaParser.parse(
            "public class MyService { }"
        );
        ClassOrInterfaceDeclaration cd = cu.findFirst(ClassOrInterfaceDeclaration.class).orElseThrow();

        Optional<Reference> ref = resolver.resolveFromNode(cd, testFile);

        assertTrue(ref.isPresent());
        assertEquals("MyService", ref.get().name());
        assertEquals(ReferenceType.CLASS, ref.get().type());
    }

    @Test
    void nonDefinitionNodeResolvesToNearestDefinitionParent() {
        CompilationUnit cu = StaticJavaParser.parse(
            "public class Foo { void bar() { int x = 1; int y = x + 1; } }"
        );
        // Find the NameExpr "x" used in "x + 1"
        NameExpr nameExpr = cu.findAll(NameExpr.class).stream()
            .filter(n -> n.getNameAsString().equals("x"))
            .findFirst().orElseThrow();

        Optional<Reference> ref = resolver.resolveFromNode(nameExpr, testFile);

        // Should resolve to the nearest definition ancestor (VariableDeclarator "y" or method "bar")
        assertTrue(ref.isPresent(), "Non-definition node should resolve to nearest definition ancestor");
        assertNotNull(ref.get().name());
        assertNotNull(ref.get().type());
    }

    @Test
    void finalFieldResolvesToConstant() {
        CompilationUnit cu = StaticJavaParser.parse(
            "public class Foo { private static final int MAX_SIZE = 100; }"
        );
        VariableDeclarator vd = cu.findFirst(VariableDeclarator.class,
            v -> v.getNameAsString().equals("MAX_SIZE")).orElseThrow();

        Optional<Reference> ref = resolver.resolveFromNode(vd, testFile);

        assertTrue(ref.isPresent());
        assertEquals("MAX_SIZE", ref.get().name());
        assertEquals(ReferenceType.CONSTANT, ref.get().type());
    }
}
