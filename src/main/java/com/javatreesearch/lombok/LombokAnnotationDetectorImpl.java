package com.javatreesearch.lombok;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;

import java.util.HashSet;
import java.util.Set;

public class LombokAnnotationDetectorImpl implements LombokAnnotationDetector {

    @Override
    public Set<LombokAnnotation> detectClassAnnotations(CompilationUnit cu) {
        Set<LombokAnnotation> result = new HashSet<>();

        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            for (var annotation : classDecl.getAnnotations()) {
                LombokAnnotation.fromName(annotation.getNameAsString())
                        .ifPresent(result::add);
            }
        }

        return result.isEmpty() ? Set.of() : Set.copyOf(result);
    }

    @Override
    public Set<LombokAnnotation> detectFieldAnnotations(FieldDeclaration field) {
        Set<LombokAnnotation> result = new HashSet<>();

        for (var annotation : field.getAnnotations()) {
            LombokAnnotation.fromName(annotation.getNameAsString())
                    .ifPresent(result::add);
        }

        return result.isEmpty() ? Set.of() : Set.copyOf(result);
    }
}
