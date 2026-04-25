package com.javatreesearch.lombok;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import java.util.Set;

public interface LombokAnnotationDetector {
    /** Returns all Lombok annotations present on class-level declarations in the CU. */
    Set<LombokAnnotation> detectClassAnnotations(CompilationUnit cu);

    /** Returns all Lombok annotations present on a specific field declaration. */
    Set<LombokAnnotation> detectFieldAnnotations(FieldDeclaration field);
}
