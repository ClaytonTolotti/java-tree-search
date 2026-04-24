package com.javatreesearch.parser;

import java.nio.file.Path;

/**
 * Exception thrown when YAML content is syntactically invalid.
 * Wraps the underlying SnakeYAML exception and includes the source Path for error context.
 */
public class CloudFormationYamlParseException extends RuntimeException {

    private final Path source;

    public CloudFormationYamlParseException(Path source, Throwable cause) {
        super("Failed to parse YAML file: " + source, cause);
        this.source = source;
    }

    public Path getSource() {
        return source;
    }
}
