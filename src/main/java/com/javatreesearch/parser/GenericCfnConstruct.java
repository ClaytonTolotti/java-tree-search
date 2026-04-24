package com.javatreesearch.parser;

import org.yaml.snakeyaml.constructor.AbstractConstruct;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

/**
 * Fallback construct for unknown CloudFormation tags.
 * Serializes any YAML node as a plain string prefixed with the tag name,
 * without instantiating any Java class from the YAML content.
 *
 * <p>Registered with {@code addConstruct(null, ...)} in {@code CloudFormationTagRegistry}
 * to handle tags not listed in {@link CloudFormationTag}.
 *
 * <p>Requirements: 10.1, 10.2, 10.3
 */
public class GenericCfnConstruct extends AbstractConstruct {

    @Override
    public Object construct(Node node) {
        String tagName = node.getTag().getValue();
        String value;
        if (node instanceof ScalarNode) {
            value = ((ScalarNode) node).getValue();
        } else {
            value = node.toString();
        }
        return tagName + ":" + value;
    }
}
