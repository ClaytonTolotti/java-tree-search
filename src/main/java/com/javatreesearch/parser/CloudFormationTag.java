package com.javatreesearch.parser;

public enum CloudFormationTag {
    REF("!Ref"),
    SUB("!Sub"),
    GET_ATT("!GetAtt"),
    IF("!If"),
    SELECT("!Select"),
    JOIN("!Join"),
    FIND_IN_MAP("!FindInMap"),
    IMPORT_VALUE("!ImportValue"),
    BASE64("!Base64"),
    CIDR("!Cidr"),
    SPLIT("!Split"),
    TRANSFORM("!Transform");

    private final String yamlTag;

    CloudFormationTag(String yamlTag) { this.yamlTag = yamlTag; }

    public String yamlTag() { return yamlTag; }
}
