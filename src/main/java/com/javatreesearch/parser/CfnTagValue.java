package com.javatreesearch.parser;

/**
 * Representa o valor serializado de uma tag CloudFormation após parsing.
 * Ex: !Ref MyBucket → CfnTagValue("Ref", "MyBucket")
 */
public record CfnTagValue(String tagName, String value) {

    public CfnTagValue {
        if (tagName == null || tagName.isEmpty()) {
            throw new IllegalArgumentException("tagName must not be null or empty");
        }
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
    }

    @Override
    public String toString() {
        return tagName + ":" + value;
    }
}
