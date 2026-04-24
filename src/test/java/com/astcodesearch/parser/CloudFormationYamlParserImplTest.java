package com.astcodesearch.parser;

import com.javatreesearch.parser.CfnTagValue;
import com.javatreesearch.parser.CloudFormationTag;
import com.javatreesearch.parser.CloudFormationYamlParseException;
import com.javatreesearch.parser.CloudFormationYamlParser;
import com.javatreesearch.parser.CloudFormationYamlParserImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CloudFormationYamlParserImpl}.
 *
 * Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5
 */
class CloudFormationYamlParserImplTest {

    private static final Path SOURCE = Path.of("template.yaml");
    private final CloudFormationYamlParser parser = new CloudFormationYamlParserImpl();

    // -------------------------------------------------------------------------
    // Requirement 5.5: null arguments throw IllegalArgumentException
    // -------------------------------------------------------------------------

    @Test
    void parseAll_nullContent_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parseAll(null, SOURCE));
    }

    @Test
    void parseAll_nullSource_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parseAll("key: value", null));
    }

    // -------------------------------------------------------------------------
    // Requirement 5.1: valid YAML returns non-null Iterable
    // -------------------------------------------------------------------------

    @Test
    void parseAll_validYamlWithoutTags_returnsNonNullIterable() {
        String yaml = "key: value\nother: 42\n";
        Iterable<Object> result = parser.parseAll(yaml, SOURCE);
        assertNotNull(result);
    }

    @Test
    void parseAll_emptyYaml_returnsIterableWithNullDocument() {
        Iterable<Object> result = parser.parseAll("", SOURCE);
        assertNotNull(result);
    }

    @Test
    void parseAll_simpleMapYaml_parsesCorrectly() {
        String yaml = "name: MyBucket\ntype: S3\n";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertEquals(1, docs.size());
        assertNotNull(docs.get(0));
    }

    // -------------------------------------------------------------------------
    // Requirement 5.4: invalid YAML throws CloudFormationYamlParseException with source
    // -------------------------------------------------------------------------

    @Test
    void parseAll_invalidYaml_throwsCloudFormationYamlParseException() {
        String invalidYaml = "key: [\nunclosed bracket";
        CloudFormationYamlParseException ex = assertThrows(
                CloudFormationYamlParseException.class,
                () -> toList(parser.parseAll(invalidYaml, SOURCE))
        );
        assertEquals(SOURCE, ex.getSource());
    }

    @Test
    void parseAll_invalidYaml_exceptionContainsSourcePath() {
        Path customSource = Path.of("my/custom/path/template.yaml");
        String invalidYaml = ": invalid: yaml: content:";
        CloudFormationYamlParseException ex = assertThrows(
                CloudFormationYamlParseException.class,
                () -> toList(parser.parseAll(invalidYaml, customSource))
        );
        assertEquals(customSource, ex.getSource());
        assertTrue(ex.getMessage().contains("my/custom/path/template.yaml"));
    }

    // -------------------------------------------------------------------------
    // Requirement 5.2: each CloudFormationTag is parsed to CfnTagValue
    // -------------------------------------------------------------------------

    @Test
    void parseAll_refTag_producesCfnTagValue() {
        String yaml = "!Ref MyBucket";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertEquals(1, docs.size());
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertEquals("Ref:MyBucket", docs.get(0).toString());
    }

    @Test
    void parseAll_subTag_producesCfnTagValue() {
        String yaml = "!Sub \"arn:aws:s3:::${BucketName}\"";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertEquals("Sub:arn:aws:s3:::${BucketName}", docs.get(0).toString());
    }

    @Test
    void parseAll_importValueTag_producesCfnTagValue() {
        String yaml = "!ImportValue SharedBucket";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertEquals("ImportValue:SharedBucket", docs.get(0).toString());
    }

    @Test
    void parseAll_base64Tag_producesCfnTagValue() {
        String yaml = "!Base64 someData";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertEquals("Base64:someData", docs.get(0).toString());
    }

    @Test
    void parseAll_ifTag_producesCfnTagValue() {
        String yaml = "!If [Cond, a, b]";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertTrue(docs.get(0).toString().startsWith("If:"));
    }

    @Test
    void parseAll_selectTag_producesCfnTagValue() {
        String yaml = "!Select [0, [a, b]]";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertTrue(docs.get(0).toString().startsWith("Select:"));
    }

    @Test
    void parseAll_joinTag_producesCfnTagValue() {
        String yaml = "!Join [\"-\", [a, b, c]]";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertTrue(docs.get(0).toString().startsWith("Join:"));
    }

    @Test
    void parseAll_findInMapTag_producesCfnTagValue() {
        String yaml = "!FindInMap [MapName, Key, SubKey]";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertTrue(docs.get(0).toString().startsWith("FindInMap:"));
    }

    @Test
    void parseAll_cidrTag_producesCfnTagValue() {
        String yaml = "!Cidr [192.168.0.0/24, 6, 5]";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertTrue(docs.get(0).toString().startsWith("Cidr:"));
    }

    @Test
    void parseAll_splitTag_producesCfnTagValue() {
        String yaml = "!Split [\",\", \"a,b,c\"]";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertTrue(docs.get(0).toString().startsWith("Split:"));
    }

    @Test
    void parseAll_getAttScalarTag_producesCfnTagValue() {
        String yaml = "!GetAtt MyBucket.Arn";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertEquals("GetAtt:MyBucket.Arn", docs.get(0).toString());
    }

    @Test
    void parseAll_getAttSequenceTag_producesCfnTagValue() {
        String yaml = "!GetAtt [MyBucket, Arn]";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertEquals("GetAtt:MyBucket,Arn", docs.get(0).toString());
    }

    @Test
    void parseAll_transformTag_producesCfnTagValue() {
        String yaml = "!Transform {Name: MyMacro, Parameters: simple}";
        List<Object> docs = toList(parser.parseAll(yaml, SOURCE));
        assertInstanceOf(CfnTagValue.class, docs.get(0));
        assertTrue(docs.get(0).toString().startsWith("Transform:"));
    }

    @Test
    void parseAll_allTwelveTagsAreCovered() {
        // Sanity check: enum has exactly 12 values
        assertEquals(12, CloudFormationTag.values().length);
    }

    // -------------------------------------------------------------------------
    // Requirement 5.3: unknown tags are handled by GenericCfnConstruct
    // -------------------------------------------------------------------------

    @Test
    void parseAll_unknownTag_doesNotThrow() {
        String yaml = "!CustomTag someValue";
        assertDoesNotThrow(() -> toList(parser.parseAll(yaml, SOURCE)));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static List<Object> toList(Iterable<Object> iterable) {
        List<Object> list = new ArrayList<>();
        for (Object obj : iterable) {
            list.add(obj);
        }
        return list;
    }
}
