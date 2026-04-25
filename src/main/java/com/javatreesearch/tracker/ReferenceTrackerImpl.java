package com.javatreesearch.tracker;

import com.javatreesearch.lombok.LombokAnnotation;
import com.javatreesearch.lombok.LombokAnnotationDetector;
import com.javatreesearch.lombok.LombokAnnotationDetectorImpl;
import com.javatreesearch.lombok.LombokMethodNameDeriver;
import com.javatreesearch.lombok.LombokMethodNameDeriverImpl;
import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.javatreesearch.model.UsageNode;
import com.javatreesearch.parser.AstParser;
import com.javatreesearch.parser.AstParserImpl;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final LombokAnnotationDetector lombokDetector;
    private final LombokMethodNameDeriver lombokDeriver;

    public ReferenceTrackerImpl(AstParser astParser, LombokAnnotationDetector lombokDetector, LombokMethodNameDeriver lombokDeriver) {
        this.astParser = astParser;
        this.lombokDetector = lombokDetector;
        this.lombokDeriver = lombokDeriver;
    }

    public ReferenceTrackerImpl(AstParser astParser) {
        this(astParser, new LombokAnnotationDetectorImpl(), new LombokMethodNameDeriverImpl());
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

        findLombokUsages(reference, javaFiles, usages);

        if (reference.type() == ReferenceType.CONFIG_KEY) {
            findConfigKeyLombokUsages(reference, javaFiles, usages);
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

    /**
     * For CONFIG_KEY references, finds Lombok-derived method usages by locating Java fields
     * annotated with @Value("${key}") or @ConfigProperty(name = "key") that belong to classes
     * with Lombok annotations, then searching for calls to the derived methods.
     * Req 3.3
     */
    private void findConfigKeyLombokUsages(Reference reference, List<Path> javaFiles, List<UsageNode> usages) {
        if (reference.type() != ReferenceType.CONFIG_KEY) return;

        String configKey = reference.name();

        try {
            for (Path javaFile : javaFiles) {
                try {
                    // Pre-filter: skip if file doesn't contain the config key
                    if (!Files.readString(javaFile).contains(configKey)) continue;
                } catch (IOException e) {
                    continue;
                }

                // Get the AST
                Optional<CompilationUnit> cuOpt;
                try {
                    cuOpt = astParser.getAst(javaFile, configKey);
                } catch (Exception e) {
                    continue;
                }
                if (cuOpt.isEmpty()) continue;

                CompilationUnit cu = cuOpt.get();

                // Search for @Value("${key}") annotations
                cu.findAll(SingleMemberAnnotationExpr.class).forEach(ann -> {
                    try {
                        String annName = ann.getNameAsString();
                        if (annName.equals("Value") || annName.endsWith(".Value")) {
                            String memberValue = extractStringValue(ann.getMemberValue());
                            if (matchesSpringValueKey(memberValue, configKey)) {
                                processAnnotationForLombok(ann, cu, javaFiles, usages);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[WARN] CONFIG_KEY Lombok detection failed for annotation in " + javaFile + ": " + e.getMessage());
                    }
                });

                // Search for @ConfigProperty(name = "key") annotations
                cu.findAll(NormalAnnotationExpr.class).forEach(ann -> {
                    try {
                        String annName = ann.getNameAsString();
                        if (annName.equals("ConfigProperty") || annName.endsWith(".ConfigProperty")) {
                            ann.getPairs().stream()
                                .filter(p -> p.getNameAsString().equals("name"))
                                .filter(p -> extractStringValue(p.getValue()).equals(configKey))
                                .findFirst()
                                .ifPresent(p -> processAnnotationForLombok(ann, cu, javaFiles, usages));
                        }
                    } catch (Exception e) {
                        System.err.println("[WARN] CONFIG_KEY Lombok detection failed for annotation in " + javaFile + ": " + e.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[WARN] CONFIG_KEY Lombok usage tracking failed for " + configKey + ": " + e.getMessage());
        }
    }

    /**
     * Given an annotation node, walks up the AST to find the enclosing FieldDeclaration,
     * then applies Lombok detection and method name derivation, searching for usages.
     */
    private void processAnnotationForLombok(AnnotationExpr annotation, CompilationUnit cu,
                                             List<Path> javaFiles, List<UsageNode> usages) {
        try {
            // Walk up the AST to find the enclosing FieldDeclaration
            Optional<Node> parent = annotation.getParentNode();
            FieldDeclaration field = null;
            while (parent.isPresent()) {
                Node node = parent.get();
                if (node instanceof FieldDeclaration fd) {
                    field = fd;
                    break;
                }
                parent = node.getParentNode();
            }

            if (field == null) return;

            // Detect class-level and field-level Lombok annotations
            Set<LombokAnnotation> classAnnotations = lombokDetector.detectClassAnnotations(cu);
            Set<LombokAnnotation> fieldAnnotations = lombokDetector.detectFieldAnnotations(field);

            Set<LombokAnnotation> annotations = new HashSet<>();
            annotations.addAll(classAnnotations);
            annotations.addAll(fieldAnnotations);

            if (annotations.isEmpty()) return;

            // Collect existing method names from the CU
            Set<String> existingMethods = cu.findAll(MethodDeclaration.class).stream()
                .map(MethodDeclaration::getNameAsString)
                .collect(Collectors.toSet());

            // Get the field name and type
            final FieldDeclaration finalField = field;
            String fieldName = finalField.getVariables().stream()
                .findFirst()
                .map(v -> v.getNameAsString())
                .orElse(null);
            if (fieldName == null) return;

            String fieldType = finalField.getElementType().asString();

            // Derive method names
            Set<String> derivedNames = lombokDeriver.deriveMethodNames(
                fieldName, fieldType, annotations, existingMethods);

            // For each derived name, search all java files for MethodCallExpr
            for (String derivedName : derivedNames) {
                for (Path javaFile : javaFiles) {
                    try {
                        if (!Files.readString(javaFile).contains(derivedName)) continue;
                    } catch (IOException e) {
                        continue;
                    }

                    Optional<CompilationUnit> fileCuOpt;
                    try {
                        fileCuOpt = astParser.getAst(javaFile, derivedName);
                    } catch (Exception e) {
                        continue;
                    }
                    if (fileCuOpt.isEmpty()) continue;

                    CompilationUnit fileCu = fileCuOpt.get();
                    fileCu.findAll(MethodCallExpr.class).stream()
                        .filter(m -> m.getNameAsString().equals(derivedName))
                        .forEach(m -> usages.add(toUsageNode(m, javaFile)));
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Lombok processing for CONFIG_KEY annotation failed: " + e.getMessage());
        }
    }

    /**
     * Searches for usages of Lombok-derived method names across all Java files.
     * Req 3.1, 3.2, 3.4, 3.5, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3
     */
    private void findLombokUsages(Reference reference, List<Path> javaFiles, List<UsageNode> usages) {
        try {
            // Step 1: Get the source file of the reference
            Path sourceFile = reference.file();

            // Step 2: Try to get the CompilationUnit of the source file
            Optional<CompilationUnit> sourceCuOpt;
            try {
                sourceCuOpt = astParser.getAst(sourceFile, reference.name());
            } catch (Exception e) {
                System.err.println("[WARN] Lombok detection skipped for " + sourceFile + ": " + e.getMessage());
                return;
            }
            if (sourceCuOpt.isEmpty()) return;

            CompilationUnit cu = sourceCuOpt.get();

            // Step 4: Detect class-level Lombok annotations
            Set<LombokAnnotation> classAnnotations = lombokDetector.detectClassAnnotations(cu);

            // Step 5: Find the FieldDeclaration matching reference.name()
            Optional<FieldDeclaration> fieldOpt = cu.findAll(FieldDeclaration.class).stream()
                .filter(f -> f.getVariables().stream()
                    .anyMatch(v -> v.getNameAsString().equals(reference.name())))
                .findFirst();

            // Step 6: Detect field-level annotations (if field found)
            Set<LombokAnnotation> fieldAnnotations = fieldOpt
                .map(lombokDetector::detectFieldAnnotations)
                .orElse(Set.of());

            // Step 7: Union class and field annotations
            Set<LombokAnnotation> annotations = new HashSet<>();
            annotations.addAll(classAnnotations);
            annotations.addAll(fieldAnnotations);

            // Step 8: If union is empty, return early
            if (annotations.isEmpty()) return;

            // Step 9: Collect existing method names from the CU
            Set<String> existingMethods = cu.findAll(MethodDeclaration.class).stream()
                .map(MethodDeclaration::getNameAsString)
                .collect(Collectors.toSet());

            // Step 10: Get the field type
            String fieldType = fieldOpt
                .map(f -> f.getElementType().asString())
                .orElse("Object");

            // Step 11: Derive method names
            Set<String> derivedNames = lombokDeriver.deriveMethodNames(
                reference.name(), fieldType, annotations, existingMethods);

            // Step 12: For each derived name, search all java files
            for (String derivedName : derivedNames) {
                for (Path javaFile : javaFiles) {
                    // Pre-filter: skip if file doesn't contain the derived name
                    try {
                        if (!Files.readString(javaFile).contains(derivedName)) continue;
                    } catch (IOException e) {
                        continue;
                    }

                    // Get the AST
                    Optional<CompilationUnit> cuOpt;
                    try {
                        cuOpt = astParser.getAst(javaFile, derivedName);
                    } catch (Exception e) {
                        continue;
                    }
                    if (cuOpt.isEmpty()) continue;

                    CompilationUnit fileCu = cuOpt.get();

                    // Find all MethodCallExpr matching the derived name
                    fileCu.findAll(MethodCallExpr.class).stream()
                        .filter(m -> m.getNameAsString().equals(derivedName))
                        .forEach(m -> usages.add(toUsageNode(m, javaFile)));
                }
            }
        } catch (Exception e) {
            System.err.println("[WARN] Lombok usage tracking failed for " + reference.name() + ": " + e.getMessage());
        }
    }
}
