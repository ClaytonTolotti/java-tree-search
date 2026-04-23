package com.javatreesearch.tracker;

import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.javatreesearch.model.UsageNode;
import com.javatreesearch.parser.AstParser;
import com.javatreesearch.parser.AstParserImpl;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Locates all usages of a reference across the repository.
 *
 * For regular references: searches NameExpr, MethodCallExpr, ObjectCreationExpr.
 * For CONFIG_KEY references: also searches @Value and @ConfigProperty annotations.
 *
 * Req 4.1, 4.2, 4.3
 */
public class ReferenceTrackerImpl implements ReferenceTracker {

    private final AstParser astParser;

    public ReferenceTrackerImpl(AstParser astParser) {
        this.astParser = astParser;
    }

    public ReferenceTrackerImpl() {
        this(new AstParserImpl());
    }

    @Override
    public List<UsageNode> findUsages(Reference reference, List<Path> javaFiles, List<Path> configFiles) {
        List<UsageNode> usages = new ArrayList<>();
        String name = reference.name();

        for (Path javaFile : javaFiles) {
            Optional<CompilationUnit> cuOpt;
            try {
                cuOpt = astParser.getAst(javaFile, name);
            } catch (AstParserImpl.AstIoException | AstParserImpl.AstSyntaxException e) {
                System.err.println("[WARN] " + e.getMessage());
                continue;
            }
            if (cuOpt.isEmpty()) continue;

            CompilationUnit cu = cuOpt.get();

            // Search NameExpr usages
            cu.findAll(NameExpr.class).stream()
                .filter(n -> n.getNameAsString().equals(name))
                .forEach(n -> usages.add(toUsageNode(n, javaFile)));

            // Search MethodCallExpr usages
            cu.findAll(MethodCallExpr.class).stream()
                .filter(m -> m.getNameAsString().equals(name))
                .forEach(m -> usages.add(toUsageNode(m, javaFile)));

            // Search ObjectCreationExpr usages (new ClassName(...))
            cu.findAll(ObjectCreationExpr.class).stream()
                .filter(o -> o.getTypeAsString().equals(name))
                .forEach(o -> usages.add(toUsageNode(o, javaFile)));

            // For CONFIG_KEY: search @Value and @ConfigProperty annotations
            if (reference.type() == ReferenceType.CONFIG_KEY) {
                findAnnotationUsages(cu, name, javaFile, usages);
            }
        }

        return usages;
    }

    /**
     * Searches for Spring Boot @Value("${key}") and Quarkus @ConfigProperty(name = "key") usages.
     * Req 4.2
     */
    private void findAnnotationUsages(CompilationUnit cu, String key, Path file, List<UsageNode> usages) {
        cu.findAll(NormalAnnotationExpr.class).forEach(ann -> {
            String annName = ann.getNameAsString();

            // Quarkus: @ConfigProperty(name = "key") or @ConfigProperty(name = "key", defaultValue = "...")
            if (annName.equals("ConfigProperty") || annName.endsWith(".ConfigProperty")) {
                ann.getPairs().stream()
                    .filter(p -> p.getNameAsString().equals("name"))
                    .filter(p -> extractStringValue(p.getValue()).equals(key))
                    .findFirst()
                    .ifPresent(p -> usages.add(toUsageNode(ann, file)));
            }
        });

        cu.findAll(SingleMemberAnnotationExpr.class).forEach(ann -> {
            String annName = ann.getNameAsString();

            // Spring Boot: @Value("${key}") or @Value("${key:default}")
            if (annName.equals("Value") || annName.endsWith(".Value")) {
                String memberValue = extractStringValue(ann.getMemberValue());
                if (matchesSpringValueKey(memberValue, key)) {
                    usages.add(toUsageNode(ann, file));
                }
            }
        });
    }

    /**
     * Checks if a Spring @Value expression references the given config key.
     * Matches: ${key} and ${key:default}
     */
    private boolean matchesSpringValueKey(String valueExpr, String key) {
        // valueExpr is like "${key}" or "${key:default}"
        if (valueExpr.startsWith("${") && valueExpr.endsWith("}")) {
            String inner = valueExpr.substring(2, valueExpr.length() - 1);
            int colonIdx = inner.indexOf(':');
            String actualKey = colonIdx >= 0 ? inner.substring(0, colonIdx) : inner;
            return actualKey.equals(key);
        }
        return false;
    }

    private String extractStringValue(Expression expr) {
        if (expr instanceof StringLiteralExpr sle) {
            return sle.asString();
        }
        return expr.toString().replace("\"", "");
    }

    private UsageNode toUsageNode(Node node, Path file) {
        int line = node.getBegin().map(p -> p.line).orElse(1);
        int col = node.getBegin().map(p -> p.column - 1).orElse(0);
        return new UsageNode(node, file, Math.max(1, line), Math.max(0, col));
    }
}
