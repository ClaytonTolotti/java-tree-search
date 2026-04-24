package com.javatreesearch.parser;

import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Construct for CloudFormation mapping tags: !Transform.
 * Iterates over key-value pairs in the mapping node, serializes them as
 * "key1=val1,key2=val2" and wraps the result in a CfnTagValue.
 */
public class MappingCfnConstruct implements Construct {

    private final String tagName;
    private final Function<Node, Object> nodeResolver;

    public MappingCfnConstruct(String tagName, Function<Node, Object> nodeResolver) {
        this.tagName = tagName;
        this.nodeResolver = nodeResolver;
    }

    @Override
    public Object construct(Node node) {
        MappingNode mappingNode = (MappingNode) node;
        List<String> parts = new ArrayList<>();
        for (NodeTuple tuple : mappingNode.getValue()) {
            String key = resolveAsString(tuple.getKeyNode());
            String value = resolveAsString(tuple.getValueNode());
            parts.add(key + "=" + value);
        }
        String mappingStr = String.join(",", parts);
        return new CfnTagValue(tagName, mappingStr);
    }

    @Override
    public void construct2ndStep(Node node, Object object) {
        // No two-step construction needed for mapping CFN tags
    }

    private String resolveAsString(Node node) {
        if (node instanceof ScalarNode) {
            return ((ScalarNode) node).getValue();
        }
        Object resolved = nodeResolver.apply(node);
        return String.valueOf(resolved);
    }
}
