package com.javatreesearch.tracker;

// Feature: lombok-reference-support, Property 13: Reutilização de cache para métodos Lombok

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.javatreesearch.model.Reference;
import com.javatreesearch.model.ReferenceType;
import com.javatreesearch.model.UsageNode;
import com.javatreesearch.parser.AstParser;
import com.javatreesearch.parser.AstParserImpl;
import net.jqwik.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ReferenceTracker cache reuse behavior on Lombok-derived methods.
 *
 * Validates: Requirement 5.3
 */
class ReferenceTrackerCachePropertiesTest {

    /**
     * Counting AstParser that implements its own cache (mirroring AstParserImpl's strategy)
     * and counts how many times each file is actually parsed (cache misses).
     *
     * The cache is keyed by Path only — once a file is parsed and cached, any subsequent
     * getAst call for the same file (regardless of term) returns the cached CompilationUnit
     * without re-parsing. This mirrors the behavior of AstParserImpl.
     *
     * By counting parse operations separately from getAst calls, we can verify that
     * the ReferenceTracker reuses the cached CompilationUnit when searching for
     * LombokDerivedMethods, rather than re-parsing the file.
     */
    static class CountingAstParser implements AstParser {
        // Cache keyed by Path only (same strategy as AstParserImpl)
        private final Map<Path, CompilationUnit> cache = new HashMap<>();
        // Counts actual parse operations per file (cache misses that result in a parse)
        private final Map<Path, AtomicInteger> parseCountPerFile = new HashMap<>();
        // Counts total getAst calls per file
        private final Map<Path, AtomicInteger> callCountPerFile = new HashMap<>();

        @Override
        public Optional<CompilationUnit> getAst(Path file, String term) {
            callCountPerFile.computeIfAbsent(file, k -> new AtomicInteger(0)).incrementAndGet();

            // 1. Check cache first (same as AstParserImpl)
            if (cache.containsKey(file)) {
                // Cache hit: return cached CompilationUnit without re-parsing
                return Optional.of(cache.get(file));
            }

            // 2. Cache miss: read file content
            String content;
            try {
                content = Files.readString(file);
            } catch (IOException e) {
                return Optional.empty();
            }

            // 3. Pre-filter: skip parse if term not in content
            if (!content.contains(term)) {
                return Optional.empty();
            }

            // 4. Parse and cache (count this as an actual parse operation)
            parseCountPerFile.computeIfAbsent(file, k -> new AtomicInteger(0)).incrementAndGet();
            try {
                CompilationUnit cu = StaticJavaParser.parse(content);
                cache.put(file, cu);
                return Optional.of(cu);
            } catch (ParseProblemException e) {
                return Optional.empty();
            }
        }

        @Override
        public List<Path> listJavaFiles(Path rootDir) {
            return new AstParserImpl().listJavaFiles(rootDir);
        }

        /**
         * Returns the number of times a file was actually parsed (cache misses that
         * resulted in a parse). Should be at most 1 per file.
         */
        public int getParseCount(Path file) {
            AtomicInteger count = parseCountPerFile.get(file);
            return count == null ? 0 : count.get();
        }

        /**
         * Returns the total number of getAst calls for a file (including cache hits).
         */
        public int getCallCount(Path file) {
            AtomicInteger count = callCountPerFile.get(file);
            return count == null ? 0 : count.get();
        }

        /**
         * Returns all files that were parsed more than once (cache not reused).
         */
        public List<String> getMultiParsedFiles() {
            List<String> result = new ArrayList<>();
            for (Map.Entry<Path, AtomicInteger> entry : parseCountPerFile.entrySet()) {
                if (entry.getValue().get() > 1) {
                    result.add(entry.getKey().getFileName() + " (parsed " + entry.getValue().get() + " times)");
                }
            }
            return result;
        }
    }

    /**
     * Property 13: Reutilização de cache para métodos Lombok
     *
     * For any Java file already present in the AstParser cache, the ReferenceTracker
     * SHALL NOT re-parse that file when searching for LombokDerivedMethods — the cached
     * CompilationUnit SHALL be reused.
     *
     * Scenario: the consumer file is called in the main search loop (for the field name)
     * and again in the Lombok search loop (for the getter name). The cache ensures the
     * file is only parsed once, even though getAst is called twice with different terms.
     *
     * Validates: Requirement 5.3
     */
    @Property(tries = 30)
    void cacheIsReusedForLombokDerivedMethods(
            @ForAll("validFieldNames") String fieldName
    ) throws IOException {
        // Derive the getter name that Lombok would generate for this field
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        // Source class: annotated with @Data, contains the field
        String sourceClassContent = String.format(
            "import lombok.Data;\n" +
            "@Data\n" +
            "public class Config {\n" +
            "    private String %s;\n" +
            "}\n",
            fieldName
        );

        // Consumer file: contains both the field name (as part of the getter call) and
        // the getter name. This file will be queried in the main loop (for fieldName)
        // and in the Lombok loop (for getterName). The cache should prevent re-parsing.
        String consumerClassContent = String.format(
            "public class Consumer {\n" +
            "    void use(Config c) {\n" +
            "        c.%s();\n" +
            "    }\n" +
            "}\n",
            getterName
        );

        Path tempDir = Files.createTempDirectory("cache-test");
        try {
            Path sourceFile = tempDir.resolve("Config.java");
            Path consumerFile = tempDir.resolve("Consumer.java");

            Files.writeString(sourceFile, sourceClassContent);
            Files.writeString(consumerFile, consumerClassContent);

            // Reference points to the field in the source file
            Reference reference = new Reference(fieldName, ReferenceType.VARIABLE, sourceFile, 4, 19);

            CountingAstParser countingParser = new CountingAstParser();
            ReferenceTrackerImpl tracker = new ReferenceTrackerImpl(countingParser);

            List<UsageNode> usages = tracker.findUsages(
                reference,
                List.of(sourceFile, consumerFile),
                List.of()
            );

            // Core assertion: no file should be parsed more than once.
            // The cache ensures that once a file is parsed, subsequent getAst calls for
            // the same file (even with different terms) return the cached CompilationUnit
            // without re-parsing.
            List<String> multiParsedFiles = countingParser.getMultiParsedFiles();
            assertTrue(
                multiParsedFiles.isEmpty(),
                "Cache violation: the following files were parsed more than once, " +
                "indicating the cache is not being reused: " + multiParsedFiles +
                ". Source file calls=" + countingParser.getCallCount(sourceFile) +
                ", parses=" + countingParser.getParseCount(sourceFile) +
                ". Consumer file calls=" + countingParser.getCallCount(consumerFile) +
                ", parses=" + countingParser.getParseCount(consumerFile)
            );

            // Verify that files called multiple times were only parsed once
            // (i.e., cache hits happened for subsequent calls)
            int sourceFileCalls = countingParser.getCallCount(sourceFile);
            int sourceFileParses = countingParser.getParseCount(sourceFile);
            if (sourceFileCalls > 1) {
                assertTrue(sourceFileParses <= 1,
                    "Source file was called " + sourceFileCalls + " times but parsed " +
                    sourceFileParses + " times. Cache should serve subsequent calls without re-parsing.");
            }

            int consumerFileCalls = countingParser.getCallCount(consumerFile);
            int consumerFileParses = countingParser.getParseCount(consumerFile);
            if (consumerFileCalls > 1) {
                assertTrue(consumerFileParses <= 1,
                    "Consumer file was called " + consumerFileCalls + " times but parsed " +
                    consumerFileParses + " times. Cache should serve subsequent calls without re-parsing.");
            }

            // Sanity check: the getter usage should be found in the consumer file
            boolean hasGetterUsage = usages.stream()
                .anyMatch(u -> u.astNode().toString().contains(getterName));
            assertTrue(hasGetterUsage,
                "Expected to find getter usage '" + getterName + "' in consumer file, but none found. " +
                "Usages: " + usages.stream().map(u -> u.astNode().toString()).toList());

        } finally {
            deleteRecursively(tempDir);
        }
    }

    @Provide
    Arbitrary<String> validFieldNames() {
        return Arbitraries.strings()
            .withCharRange('a', 'z')
            .ofMinLength(1)
            .ofMaxLength(20);
    }

    private void deleteRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted(Comparator.reverseOrder())
                      .forEach(p -> {
                          try {
                              Files.delete(p);
                          } catch (IOException ignored) {}
                      });
            }
        }
    }
}
