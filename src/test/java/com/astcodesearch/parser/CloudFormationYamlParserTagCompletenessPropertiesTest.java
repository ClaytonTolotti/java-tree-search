package com.astcodesearch.parser;

// Feature: cloudformation-yaml-support, Property 1: Completude de tags

import com.javatreesearch.parser.CloudFormationTag;
import com.javatreesearch.parser.CloudFormationYamlParser;
import com.javatreesearch.parser.CloudFormationYamlParserImpl;
import net.jqwik.api.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for CloudFormationYamlParser tag completeness.
 *
 * **Validates: Requirements 5.1, 5.2**
 */
class CloudFormationYamlParserTagCompletenessPropertiesTest {

    private static final Path SOURCE = Path.of("property-test.yaml");
    private final CloudFormationYamlParser parser = new CloudFormationYamlParserImpl();

    /**
     * Property 1: Completude de tags
     *
     * For any CloudFormationTag combined with a random non-blank scalar value,
     * parseAll() must complete without exception and return a non-null Iterable.
     *
     * **Validates: Requirements 5.1, 5.2**
     */
    @Property(tries = 100)
    void scalarTagWithAnyValue_parsesWithoutException(
            @ForAll("scalarTags") CloudFormationTag tag,
            @ForAll("safeScalarString") String value
    ) {
        String yaml = tag.yamlTag() + " " + value;

        Iterable<Object> result = assertDoesNotThrow(
                () -> parser.parseAll(yaml, SOURCE),
                "parseAll() must not throw for tag " + tag.yamlTag() + " with value: " + value
        );
        assertNotNull(result, "parseAll() must return non-null Iterable");
    }

    /**
     * Property 1 (sequence variant): For any sequence CloudFormation tag with
     * random list elements, parseAll() must complete without exception.
     *
     * **Validates: Requirements 5.1, 5.2**
     */
    @Property(tries = 100)
    void sequenceTagWithAnyElements_parsesWithoutException(
            @ForAll("sequenceTags") CloudFormationTag tag,
            @ForAll("safeStringList") List<String> elements
    ) {
        if (elements.isEmpty()) {
            elements = List.of("a");
        }

        String items = String.join(", ", elements.stream()
                .map(e -> "\"" + e.replaceAll("[\"\\\\]", "") + "\"")
                .toList());
        String yaml = tag.yamlTag() + " [" + items + "]";

        Iterable<Object> result = assertDoesNotThrow(
                () -> parser.parseAll(yaml, SOURCE),
                "parseAll() must not throw for tag " + tag.yamlTag()
        );
        assertNotNull(result, "parseAll() must return non-null Iterable");
    }

    /**
     * Property 1 (full document variant): For any combination of CloudFormation
     * tags embedded in a YAML document, parseAll() must return a non-null Iterable.
     *
     * **Validates: Requirements 5.1, 5.2**
     */
    @Property(tries = 50)
    void fullDocumentWithMultipleTags_parsesWithoutException(
            @ForAll("scalarTags") CloudFormationTag tag1,
            @ForAll("scalarTags") CloudFormationTag tag2,
            @ForAll("safeScalarString") String value1,
            @ForAll("safeScalarString") String value2
    ) {
        String yaml = "Resources:\n"
                + "  Resource1:\n"
                + "    Prop: " + tag1.yamlTag() + " " + value1 + "\n"
                + "  Resource2:\n"
                + "    Prop: " + tag2.yamlTag() + " " + value2 + "\n";

        Iterable<Object> result = assertDoesNotThrow(
                () -> parser.parseAll(yaml, SOURCE),
                "parseAll() must not throw for document with tags " + tag1 + " and " + tag2
        );
        assertNotNull(result);

        // Materialize to confirm it's non-null and iterable
        List<Object> docs = new ArrayList<>();
        for (Object obj : result) {
            docs.add(obj);
        }
        assertFalse(docs.isEmpty(), "Document should produce at least one parsed object");
    }

    // -------------------------------------------------------------------------
    // Providers
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<CloudFormationTag> scalarTags() {
        // Tags that accept a scalar (string) argument
        return Arbitraries.of(
                CloudFormationTag.REF,
                CloudFormationTag.SUB,
                CloudFormationTag.IMPORT_VALUE,
                CloudFormationTag.BASE64
        );
    }

    @Provide
    Arbitrary<CloudFormationTag> sequenceTags() {
        // Tags that accept a sequence argument
        return Arbitraries.of(
                CloudFormationTag.IF,
                CloudFormationTag.SELECT,
                CloudFormationTag.JOIN,
                CloudFormationTag.FIND_IN_MAP,
                CloudFormationTag.CIDR,
                CloudFormationTag.SPLIT
        );
    }

    @Provide
    Arbitrary<List<String>> safeStringList() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(10)
                .list()
                .ofMinSize(1)
                .ofMaxSize(4);
    }

    /**
     * Generates strings that are safe to use as YAML scalar values after a tag.
     * Restricted to alphanumeric characters only to avoid any YAML special
     * characters (!, :, {, }, [, ], #, &, *, |, >, ', ", @, `, ,, -, .).
     */
    @Provide
    Arbitrary<String> safeScalarString() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(20);
    }
}
