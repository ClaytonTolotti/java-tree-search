package com.javatreesearch.parser;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

/**
 * Construct for CloudFormation scalar tags: !Ref, !Sub, !ImportValue, !Base64.
 * Extracts the raw scalar value and wraps it in a CfnTagValue.
 */
public class ScalarCfnConstruct extends AbstractConstruct {

    private final String tagName;

    public ScalarCfnConstruct(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public Object construct(Node node) {
        String rawValue = ((ScalarNode) node).getValue();
        return new CfnTagValue(tagName, rawValue);
    }
}
