package com.javatreesearch.resolver;

import com.javatreesearch.model.ConfigEntry;
import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Extracts Reference definitions from AST nodes or config entries.
 *
 * Supported definition node types:
 * - VariableDeclarator → VARIABLE or CONSTANT (if parent is FieldDeclaration with final modifier)
 * - FieldDeclaration → VARIABLE or CONSTANT
 * - MethodDeclaration → METHOD
 * - ClassOrInterfaceDeclaration → CLASS
 * - EnumConstantDeclaration → CONSTANT
 * - EnumDeclaration → CLASS
 *
 * Req 3.2, 3.3, 2a.4, 2a.5
 */
public class ReferenceResolverImpl implements ReferenceResolver {

    @Override
    public Optional<Reference> resolveFromNode(Node node, Path file) {
        if (node instanceof VariableDeclarator vd) {
            String name = vd.getNameAsString();
            ReferenceType type = isConstant(vd) ? ReferenceType.CONSTANT : ReferenceType.VARIABLE;
            int line = vd.getBegin().map(p -> p.line).orElse(1);
            int col = vd.getBegin().map(p -> p.column - 1).orElse(0);
            return Optional.of(new Reference(name, type, file, Math.max(1, line), Math.max(0, col)));
        }

        if (node instanceof FieldDeclaration fd) {
            // Use the first variable name
            return fd.getVariables().stream().findFirst()
                .map(vd -> {
                    String name = vd.getNameAsString();
                    ReferenceType type = fd.isFinal() ? ReferenceType.CONSTANT : ReferenceType.VARIABLE;
                    int line = fd.getBegin().map(p -> p.line).orElse(1);
                    int col = fd.getBegin().map(p -> p.column - 1).orElse(0);
                    return new Reference(name, type, file, Math.max(1, line), Math.max(0, col));
                });
        }

        if (node instanceof MethodDeclaration md) {
            String name = md.getNameAsString();
            int line = md.getBegin().map(p -> p.line).orElse(1);
            int col = md.getBegin().map(p -> p.column - 1).orElse(0);
            return Optional.of(new Reference(name, ReferenceType.METHOD, file, Math.max(1, line), Math.max(0, col)));
        }

        if (node instanceof ClassOrInterfaceDeclaration cd) {
            String name = cd.getNameAsString();
            int line = cd.getBegin().map(p -> p.line).orElse(1);
            int col = cd.getBegin().map(p -> p.column - 1).orElse(0);
            return Optional.of(new Reference(name, ReferenceType.CLASS, file, Math.max(1, line), Math.max(0, col)));
        }

        if (node instanceof EnumConstantDeclaration ec) {
            String name = ec.getNameAsString();
            int line = ec.getBegin().map(p -> p.line).orElse(1);
            int col = ec.getBegin().map(p -> p.column - 1).orElse(0);
            return Optional.of(new Reference(name, ReferenceType.CONSTANT, file, Math.max(1, line), Math.max(0, col)));
        }

        if (node instanceof EnumDeclaration ed) {
            String name = ed.getNameAsString();
            int line = ed.getBegin().map(p -> p.line).orElse(1);
            int col = ed.getBegin().map(p -> p.column - 1).orElse(0);
            return Optional.of(new Reference(name, ReferenceType.CLASS, file, Math.max(1, line), Math.max(0, col)));
        }

        // Non-definition node: find nearest definition ancestor
        return findNearestDefinitionAncestor(node, file);
    }

    @Override
    public Reference resolveFromConfigEntry(ConfigEntry entry) {
        return new Reference(
            entry.key(),
            ReferenceType.CONFIG_KEY,
            entry.file(),
            entry.line(),
            0
        );
    }

    /**
     * Walks up the AST to find the nearest ancestor that is a definition node.
     * Req 3.3: non-definition nodes use the context of the nearest definition parent.
     */
    private Optional<Reference> findNearestDefinitionAncestor(Node node, Path file) {
        Optional<Node> parent = node.getParentNode();
        while (parent.isPresent()) {
            Node p = parent.get();
            if (isDefinitionNode(p)) {
                return resolveFromNode(p, file);
            }
            parent = p.getParentNode();
        }
        return Optional.empty();
    }

    private boolean isDefinitionNode(Node node) {
        return node instanceof VariableDeclarator
            || node instanceof FieldDeclaration
            || node instanceof MethodDeclaration
            || node instanceof ClassOrInterfaceDeclaration
            || node instanceof EnumConstantDeclaration
            || node instanceof EnumDeclaration;
    }

    private boolean isConstant(VariableDeclarator vd) {
        return vd.getParentNode()
            .filter(p -> p instanceof FieldDeclaration)
            .map(p -> ((FieldDeclaration) p).isFinal())
            .orElse(false);
    }
}
