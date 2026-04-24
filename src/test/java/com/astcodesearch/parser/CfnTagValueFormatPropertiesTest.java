package com.astcodesearch.parser;

// Feature: cloudformation-yaml-support, Property 3: Formato de valor de tag

import com.javatreesearch.parser.CfnTagValue;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.NotEmpty;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for CfnTagValue toString format.
 *
 * **Validates: Requirements 3.2, 6.1, 6.2, 6.3**
 */
class CfnTagValueFormatPropertiesTest {

    /**
     * Property 3: Formato de valor de tag
     *
     * For any non-null, non-empty (tagName, value) pair,
     * CfnTagValue.toString() must return "tagName:value".
     *
     * **Validates: Requirements 3.2, 6.1, 6.2, 6.3**
     */
    @Property(tries = 200)
    void toStringReturnsTagNameColonValue(
            @ForAll @NotBlank String tagName,
            @ForAll @NotEmpty String value
    ) {
        CfnTagValue cfnTagValue = new CfnTagValue(tagName, value);
        String expected = tagName + ":" + value;
        assertEquals(expected, cfnTagValue.toString(),
                "CfnTagValue.toString() must return tagName + \":\" + value");
    }
}
