package com.javatreesearch.parser;

import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Construct for CloudFormation tags that accept either a scalar or a sequence: !GetAtt.
 * - Scalar form:   !GetAtt MyBucket.Arn   → CfnTagValue("GetAtt", "MyBucket.Arn")
 * - Sequence form: !GetAtt [MyBucket, Arn] → CfnTagValue("GetAtt", "MyBucket,Arn")
 */
public class ScalarOrSequenceCfnConstruct implements Construct {

    private final String tagName;
    @SuppressWarnings("unused")
    private final Function<Node, Object> nodeResolver;

    public ScalarOrSequenceCfnConstruct(String tagName, Function<Node, Object> nodeResolver) {
        this.tagName = tagName;
        this.nodeResolver = nodeResolver;
    }

    @Override
    public Object construct(Node node) {
        if (node instanceof ScalarNode scalarNode) {
            return new CfnTagValue(tagName, scalarNode.getValue());
        }
        // SequenceNode: children are plain scalars (no special tags), cast directly
        List<Node> children = ((SequenceNode) node).getValue();
        List<String> parts = new ArrayList<>(children.size());
        for (Node child : children) {
            parts.add(((ScalarNode) child).getValue());
        }
        return new CfnTagValue(tagName, String.join(",", parts));
    }

    @Override
    public void construct2ndStep(Node node, Object object) {
        // No two-step construction needed
    }
}
