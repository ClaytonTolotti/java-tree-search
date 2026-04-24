package com.javatreesearch.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CloudFormationTag enum and CfnTagValue record.
 * Validates: Requirements 2.1, 2.2, 3.2, 3.3, 3.4, 3.5
 */
class CloudFormationTagAndCfnTagValueTest {

    // --- CloudFormationTag enum tests ---

    @Test
    void allTwelveEnumValuesExist() {
        CloudFormationTag[] values = CloudFormationTag.values();
        assertEquals(12, values.length, "CloudFormationTag must define exactly 12 values");
    }

    @Test
    void enumValuesAreTheExpectedConstants() {
        // Req 2.1: all twelve constants must exist
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("REF"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("SUB"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("GET_ATT"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("IF"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("SELECT"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("JOIN"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("FIND_IN_MAP"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("IMPORT_VALUE"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("BASE64"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("CIDR"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("SPLIT"));
        assertDoesNotThrow(() -> CloudFormationTag.valueOf("TRANSFORM"));
    }

    @Test
    void yamlTagReturnsCorrectPrefixedStrings() {
        // Req 2.2: yamlTag() must return the !-prefixed YAML tag string
        assertEquals("!Ref",         CloudFormationTag.REF.yamlTag());
        assertEquals("!Sub",         CloudFormationTag.SUB.yamlTag());
        assertEquals("!GetAtt",      CloudFormationTag.GET_ATT.yamlTag());
        assertEquals("!If",          CloudFormationTag.IF.yamlTag());
        assertEquals("!Select",      CloudFormationTag.SELECT.yamlTag());
        assertEquals("!Join",        CloudFormationTag.JOIN.yamlTag());
        assertEquals("!FindInMap",   CloudFormationTag.FIND_IN_MAP.yamlTag());
        assertEquals("!ImportValue", CloudFormationTag.IMPORT_VALUE.yamlTag());
        assertEquals("!Base64",      CloudFormationTag.BASE64.yamlTag());
        assertEquals("!Cidr",        CloudFormationTag.CIDR.yamlTag());
        assertEquals("!Split",       CloudFormationTag.SPLIT.yamlTag());
        assertEquals("!Transform",   CloudFormationTag.TRANSFORM.yamlTag());
    }

    @Test
    void yamlTagIsNonNullAndNonEmptyForAllConstants() {
        // Req 2.3
        for (CloudFormationTag tag : CloudFormationTag.values()) {
            assertNotNull(tag.yamlTag(), "yamlTag() must not be null for " + tag);
            assertFalse(tag.yamlTag().isEmpty(), "yamlTag() must not be empty for " + tag);
        }
    }

    // --- CfnTagValue record tests ---

    @Test
    void toStringReturnsTagNameColonValue() {
        // Req 3.2 / 3.5: "Ref:MyBucket"
        CfnTagValue cfn = new CfnTagValue("Ref", "MyBucket");
        assertEquals("Ref:MyBucket", cfn.toString());
    }

    @Test
    void nullTagNameThrowsIllegalArgumentException() {
        // Req 3.3
        assertThrows(IllegalArgumentException.class,
            () -> new CfnTagValue(null, "MyBucket"));
    }

    @Test
    void emptyTagNameThrowsIllegalArgumentException() {
        // Req 3.3
        assertThrows(IllegalArgumentException.class,
            () -> new CfnTagValue("", "MyBucket"));
    }

    @Test
    void nullValueThrowsIllegalArgumentException() {
        // Req 3.4
        assertThrows(IllegalArgumentException.class,
            () -> new CfnTagValue("Ref", null));
    }
}
