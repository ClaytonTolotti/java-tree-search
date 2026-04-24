package com.javatreesearch.parser;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CloudFormationTagRegistryImpl}.
 *
 * Validates: Requirements 4.1, 4.2, 4.5
 */
class CloudFormationTagRegistryImplTest {

    private final CloudFormationTagRegistry registry = new CloudFormationTagRegistryImpl();

    // -------------------------------------------------------------------------
    // Requirement 4.1: buildConstructor() returns a non-null Constructor
    // -------------------------------------------------------------------------

    @Test
    void buildConstructor_returnsNonNull() {
        Constructor constructor = registry.buildConstructor();
        assertNotNull(constructor, "buildConstructor() must return a non-null Constructor");
    }

    // -------------------------------------------------------------------------
    // Requirement 4.5: multiple calls return distinct instances (not singleton)
    // -------------------------------------------------------------------------

    @Test
    void buildConstructor_multipleCallsReturnDistinctInstances() {
        Constructor first = registry.buildConstructor();
        Constructor second = registry.buildConstructor();
        Constructor third = registry.buildConstructor();

        assertNotSame(first, second, "Each call to buildConstructor() must return a new instance");
        assertNotSame(second, third, "Each call to buildConstructor() must return a new instance");
        assertNotSame(first, third, "Each call to buildConstructor() must return a new instance");
    }

    // -------------------------------------------------------------------------
    // Requirement 4.2: all 12 CloudFormationTag values have a registered Construct
    // Verified by parsing a minimal YAML snippet for each tag
    // -------------------------------------------------------------------------

    @Test
    void buildConstructor_allScalarTagsAreRegistered() {
        // REF, SUB, IMPORT_VALUE, BASE64 → ScalarCfnConstruct
        Constructor constructor = registry.buildConstructor();
        Yaml yaml = new Yaml(constructor);

        Object ref = yaml.load("!Ref MyBucket");
        assertInstanceOf(CfnTagValue.class, ref, "!Ref should produce a CfnTagValue");
        assertEquals("Ref:MyBucket", ref.toString());

        Object sub = yaml.load("!Sub \"my-value\"");
        assertInstanceOf(CfnTagValue.class, sub, "!Sub should produce a CfnTagValue");
        assertEquals("Sub:my-value", sub.toString());

        Object importValue = yaml.load("!ImportValue SharedBucket");
        assertInstanceOf(CfnTagValue.class, importValue, "!ImportValue should produce a CfnTagValue");
        assertEquals("ImportValue:SharedBucket", importValue.toString());

        Object base64 = yaml.load("!Base64 someData");
        assertInstanceOf(CfnTagValue.class, base64, "!Base64 should produce a CfnTagValue");
        assertEquals("Base64:someData", base64.toString());
    }

    @Test
    void buildConstructor_allSequenceTagsAreRegistered() {
        // IF, SELECT, JOIN, FIND_IN_MAP, CIDR, SPLIT → SequenceCfnConstruct
        Constructor constructor = registry.buildConstructor();
        Yaml yaml = new Yaml(constructor);

        Object ifTag = yaml.load("!If [Cond, a, b]");
        assertInstanceOf(CfnTagValue.class, ifTag, "!If should produce a CfnTagValue");
        assertTrue(ifTag.toString().startsWith("If:"), "!If value should start with 'If:'");

        Object select = yaml.load("!Select [0, [a, b]]");
        assertInstanceOf(CfnTagValue.class, select, "!Select should produce a CfnTagValue");
        assertTrue(select.toString().startsWith("Select:"), "!Select value should start with 'Select:'");

        Object join = yaml.load("!Join [\"-\", [a, b]]");
        assertInstanceOf(CfnTagValue.class, join, "!Join should produce a CfnTagValue");
        assertTrue(join.toString().startsWith("Join:"), "!Join value should start with 'Join:'");

        Object findInMap = yaml.load("!FindInMap [MapName, Key, SubKey]");
        assertInstanceOf(CfnTagValue.class, findInMap, "!FindInMap should produce a CfnTagValue");
        assertTrue(findInMap.toString().startsWith("FindInMap:"), "!FindInMap value should start with 'FindInMap:'");

        Object cidr = yaml.load("!Cidr [192.168.0.0/24, 6, 5]");
        assertInstanceOf(CfnTagValue.class, cidr, "!Cidr should produce a CfnTagValue");
        assertTrue(cidr.toString().startsWith("Cidr:"), "!Cidr value should start with 'Cidr:'");

        Object split = yaml.load("!Split [\",\", \"a,b,c\"]");
        assertInstanceOf(CfnTagValue.class, split, "!Split should produce a CfnTagValue");
        assertTrue(split.toString().startsWith("Split:"), "!Split value should start with 'Split:'");
    }

    @Test
    void buildConstructor_getAttTagIsRegistered() {
        // GET_ATT → ScalarOrSequenceCfnConstruct
        Constructor constructor = registry.buildConstructor();
        Yaml yaml = new Yaml(constructor);

        Object scalarForm = yaml.load("!GetAtt MyBucket.Arn");
        assertInstanceOf(CfnTagValue.class, scalarForm, "!GetAtt scalar form should produce a CfnTagValue");
        assertEquals("GetAtt:MyBucket.Arn", scalarForm.toString());

        Object sequenceForm = yaml.load("!GetAtt [MyBucket, Arn]");
        assertInstanceOf(CfnTagValue.class, sequenceForm, "!GetAtt sequence form should produce a CfnTagValue");
        assertEquals("GetAtt:MyBucket,Arn", sequenceForm.toString());
    }

    @Test
    void buildConstructor_transformTagIsRegistered() {
        // TRANSFORM → MappingCfnConstruct
        Constructor constructor = registry.buildConstructor();
        Yaml yaml = new Yaml(constructor);

        Object transform = yaml.load("!Transform {Name: MyMacro, Parameters: simple}");
        assertInstanceOf(CfnTagValue.class, transform, "!Transform should produce a CfnTagValue");
        assertTrue(transform.toString().startsWith("Transform:"), "!Transform value should start with 'Transform:'");
        assertTrue(transform.toString().contains("Name=MyMacro"), "!Transform value should contain 'Name=MyMacro'");
    }

    @Test
    void buildConstructor_allTwelveTagsProduceCfnTagValues() {
        // Verify all 12 CloudFormationTag enum values are handled — no tag is missing
        Constructor constructor = registry.buildConstructor();
        Yaml yaml = new Yaml(constructor);

        String[] snippets = {
            "!Ref X",
            "!Sub X",
            "!ImportValue X",
            "!Base64 X",
            "!If [a, b, c]",
            "!Select [0, [a]]",
            "!Join [\"-\", [a]]",
            "!FindInMap [M, K, S]",
            "!Cidr [10.0.0.0/8, 2, 4]",
            "!Split [\",\", \"a,b\"]",
            "!GetAtt Res.Attr",
            "!Transform {Name: M}"
        };

        assertEquals(12, CloudFormationTag.values().length,
                "CloudFormationTag enum must have exactly 12 values");

        for (String snippet : snippets) {
            Object result = assertDoesNotThrow(
                () -> yaml.load(snippet),
                "Parsing '" + snippet + "' should not throw"
            );
            assertNotNull(result, "Parsing '" + snippet + "' should return non-null");
        }
    }
}
