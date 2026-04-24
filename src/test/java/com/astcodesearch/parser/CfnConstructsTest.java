package com.javatreesearch.parser;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.Tag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CloudFormation Construct classes.
 * Tests each construct by building a minimal Yaml instance with the construct
 * registered, parsing a YAML string, and verifying the output.
 *
 * Validates: Requirements 6.1, 6.2, 7.1, 7.2, 7.3, 8.1, 8.2, 9.1, 10.1, 10.2
 */
class CfnConstructsTest {

    // -------------------------------------------------------------------------
    // Helper: build a Yaml instance with a single construct registered
    // -------------------------------------------------------------------------

    /**
     * Creates a Yaml instance backed by a SafeConstructor that has the given
     * construct registered for the given tag string (e.g. "!Ref").
     */
    private Yaml yamlWith(String tagStr, org.yaml.snakeyaml.constructor.Construct construct) {
        SafeConstructor ctor = new SafeConstructor(new LoaderOptions()) {
            {
                yamlConstructors.put(new Tag(tagStr), construct);
            }
        };
        return new Yaml(ctor);
    }

    /**
     * Creates a Yaml instance backed by a SafeConstructor that has two
     * constructs registered (used for nested-tag tests).
     */
    private Yaml yamlWith(
            String tag1, org.yaml.snakeyaml.constructor.Construct c1,
            String tag2, org.yaml.snakeyaml.constructor.Construct c2) {
        SafeConstructor ctor = new SafeConstructor(new LoaderOptions()) {
            {
                yamlConstructors.put(new Tag(tag1), c1);
                yamlConstructors.put(new Tag(tag2), c2);
            }
        };
        return new Yaml(ctor);
    }

    /**
     * Creates a Yaml instance with a fallback (null-tag) construct registered.
     * Used for GenericCfnConstruct tests.
     */
    private Yaml yamlWithFallback(org.yaml.snakeyaml.constructor.Construct fallback) {
        SafeConstructor ctor = new SafeConstructor(new LoaderOptions()) {
            {
                yamlConstructors.put(null, fallback);
            }
        };
        return new Yaml(ctor);
    }

    // =========================================================================
    // ScalarCfnConstruct tests — Requirements 6.1, 6.2, 6.3
    // =========================================================================

    @Test
    void scalarConstruct_Ref_producesCorrectCfnTagValue() {
        // Req 6.1, 6.2: !Ref MyBucket → CfnTagValue("Ref","MyBucket") → "Ref:MyBucket"
        Yaml yaml = yamlWith("!Ref", new ScalarCfnConstruct("Ref"));

        Object result = yaml.load("!Ref MyBucket");

        assertInstanceOf(CfnTagValue.class, result);
        CfnTagValue cfn = (CfnTagValue) result;
        assertEquals("Ref", cfn.tagName());
        assertEquals("MyBucket", cfn.value());
        assertEquals("Ref:MyBucket", cfn.toString());
    }

    @Test
    void scalarConstruct_Sub_producesCorrectCfnTagValue() {
        // Req 6.3: !Sub "arn:aws:s3:::${BucketName}" → "Sub:arn:aws:s3:::${BucketName}"
        Yaml yaml = yamlWith("!Sub", new ScalarCfnConstruct("Sub"));

        Object result = yaml.load("!Sub \"arn:aws:s3:::${BucketName}\"");

        assertInstanceOf(CfnTagValue.class, result);
        CfnTagValue cfn = (CfnTagValue) result;
        assertEquals("Sub", cfn.tagName());
        assertEquals("arn:aws:s3:::${BucketName}", cfn.value());
        assertEquals("Sub:arn:aws:s3:::${BucketName}", cfn.toString());
    }

    // =========================================================================
    // SequenceCfnConstruct tests — Requirements 7.1, 7.2, 7.3
    // =========================================================================

    @Test
    void sequenceConstruct_Join_producesCommaSeparatedValue() {
        // Req 7.1, 7.2: !Join ["-", ["a","b","c"]] → "Join:-,[a,b,c]"
        SafeConstructor ctor = new SafeConstructor(new LoaderOptions()) {
            {
                yamlConstructors.put(new Tag("!Join"), new SequenceCfnConstruct("Join", this::constructObject));
            }
        };
        Yaml yaml = new Yaml(ctor);

        Object result = yaml.load("!Join [\"-\", [\"a\", \"b\", \"c\"]]");

        assertInstanceOf(CfnTagValue.class, result);
        CfnTagValue cfn = (CfnTagValue) result;
        assertEquals("Join", cfn.tagName());
        // The outer sequence has two elements: "-" and the inner list [a,b,c]
        // Inner list resolves to a java.util.List → String.valueOf → "[a, b, c]"
        assertTrue(cfn.value().startsWith("-,"), "value should start with '-,' but was: " + cfn.value());
        assertEquals("Join:-,[a, b, c]", cfn.toString());
    }

    @Test
    void sequenceConstruct_If_withNestedRefTags_resolvesRecursively() {
        // Req 7.3: !If [Cond, !Ref A, !Ref B] → nested tags resolved recursively
        SafeConstructor ctor = new SafeConstructor(new LoaderOptions()) {
            {
                ScalarCfnConstruct refConstruct = new ScalarCfnConstruct("Ref");
                yamlConstructors.put(new Tag("!Ref"), refConstruct);
                yamlConstructors.put(new Tag("!If"), new SequenceCfnConstruct("If", this::constructObject));
            }
        };
        Yaml yaml = new Yaml(ctor);

        Object result = yaml.load("!If [Cond, !Ref A, !Ref B]");

        assertInstanceOf(CfnTagValue.class, result);
        CfnTagValue cfn = (CfnTagValue) result;
        assertEquals("If", cfn.tagName());
        // Expected: "Cond,Ref:A,Ref:B"
        assertEquals("Cond,Ref:A,Ref:B", cfn.value());
        assertEquals("If:Cond,Ref:A,Ref:B", cfn.toString());
    }

    // =========================================================================
    // ScalarOrSequenceCfnConstruct tests — Requirements 8.1, 8.2
    // =========================================================================

    @Test
    void scalarOrSequenceConstruct_GetAtt_scalarForm() {
        // Req 8.1: !GetAtt MyBucket.Arn → "GetAtt:MyBucket.Arn"
        SafeConstructor ctor = new SafeConstructor(new LoaderOptions()) {
            {
                yamlConstructors.put(new Tag("!GetAtt"),
                        new ScalarOrSequenceCfnConstruct("GetAtt", this::constructObject));
            }
        };
        Yaml yaml = new Yaml(ctor);

        Object result = yaml.load("!GetAtt MyBucket.Arn");

        assertInstanceOf(CfnTagValue.class, result);
        CfnTagValue cfn = (CfnTagValue) result;
        assertEquals("GetAtt", cfn.tagName());
        assertEquals("MyBucket.Arn", cfn.value());
        assertEquals("GetAtt:MyBucket.Arn", cfn.toString());
    }

    @Test
    void scalarOrSequenceConstruct_GetAtt_sequenceForm() {
        // Req 8.2: !GetAtt [MyBucket, Arn] → "GetAtt:MyBucket,Arn"
        SafeConstructor ctor = new SafeConstructor(new LoaderOptions()) {
            {
                yamlConstructors.put(new Tag("!GetAtt"),
                        new ScalarOrSequenceCfnConstruct("GetAtt", this::constructObject));
            }
        };
        Yaml yaml = new Yaml(ctor);

        Object result = yaml.load("!GetAtt [MyBucket, Arn]");

        assertInstanceOf(CfnTagValue.class, result);
        CfnTagValue cfn = (CfnTagValue) result;
        assertEquals("GetAtt", cfn.tagName());
        assertEquals("MyBucket,Arn", cfn.value());
        assertEquals("GetAtt:MyBucket,Arn", cfn.toString());
    }

    // =========================================================================
    // MappingCfnConstruct tests — Requirement 9.1
    // =========================================================================

    @Test
    void mappingConstruct_Transform_producesKeyValuePairs() {
        // Req 9.1: !Transform with a simple mapping → CfnTagValue("Transform","Name=MyMacro,Parameters=...")
        SafeConstructor ctor = new SafeConstructor(new LoaderOptions()) {
            {
                yamlConstructors.put(new Tag("!Transform"),
                        new MappingCfnConstruct("Transform", this::constructObject));
            }
        };
        Yaml yaml = new Yaml(ctor);

        String transformYaml = "!Transform {Name: MyMacro, Parameters: simple}";
        Object result = yaml.load(transformYaml);

        assertInstanceOf(CfnTagValue.class, result);
        CfnTagValue cfn = (CfnTagValue) result;
        assertEquals("Transform", cfn.tagName());
        // The value should contain "Name=MyMacro"
        assertTrue(cfn.value().contains("Name=MyMacro"),
                "Expected value to contain 'Name=MyMacro' but was: " + cfn.value());
        assertFalse(cfn.toString().isEmpty());
    }

    // =========================================================================
    // GenericCfnConstruct tests — Requirements 10.1, 10.2
    // =========================================================================

    @Test
    void genericConstruct_unknownTag_doesNotInstantiateJavaClass() {
        // Req 10.1, 10.2: unknown tag → serialized as string, no Java class instantiated
        Yaml yaml = yamlWithFallback(new GenericCfnConstruct());

        Object result = yaml.load("!CustomTag someValue");

        // Must not be a CfnTagValue (it's a plain string from GenericCfnConstruct)
        assertNotNull(result);
        assertFalse(result instanceof CfnTagValue,
                "GenericCfnConstruct should NOT produce a CfnTagValue");
        // Must be a String
        assertInstanceOf(String.class, result);
        String str = (String) result;
        // Should contain the tag name and the value
        assertTrue(str.contains("CustomTag"),
                "Result should contain the tag name 'CustomTag' but was: " + str);
        assertTrue(str.contains("someValue"),
                "Result should contain the value 'someValue' but was: " + str);
    }

    @Test
    void genericConstruct_unknownTag_resultIsNotAJavaRuntimeInstance() {
        // Req 10.2: verify no arbitrary Java class is instantiated
        Yaml yaml = yamlWithFallback(new GenericCfnConstruct());

        Object result = yaml.load("!UnknownTag anotherValue");

        assertNotNull(result);
        // The result must be a plain String — not any domain or Java class
        assertInstanceOf(String.class, result,
                "GenericCfnConstruct must return a String, not a Java class instance");
    }
}
