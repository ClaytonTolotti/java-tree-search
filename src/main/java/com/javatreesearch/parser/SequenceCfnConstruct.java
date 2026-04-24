package com.javatreesearch.parser;

import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Construct for CloudFormation sequence tags: !If, !Select, !Join, !FindInMap, !Cidr, !Split.
 * Iterates over child nodes, recursively resolves each via the provided resolver function,
 * joins results with "," and wraps in a CfnTagValue.
 */
public class SequenceCfnConstruct implements Construct {

    private final String tagName;
    private final Function<Node, Object> nodeResolver;

    public SequenceCfnConstruct(String tagName, Function<Node, Object> nodeResolver) {
        this.tagName = tagName;
        this.nodeResolver = nodeResolver;
    }

    @Override
    public Object construct(Node node) {
        List<Node> children = ((SequenceNode) node).getValue();
        List<String> parts = new ArrayList<>(children.size());
        for (Node child : children) {
            Object resolved = nodeResolver.apply(child);
            parts.add(String.valueOf(resolved));
        }
        String joined = String.join(",", parts);
        return new CfnTagValue(tagName, joined);
    }

    @Override
    public void construct2ndStep(Node node, Object object) {
        // No two-step construction needed for sequence CFN tags
    }
}
