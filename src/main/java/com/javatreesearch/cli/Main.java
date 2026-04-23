package com.javatreesearch.cli;

import picocli.CommandLine;

/**
 * Entry point for the AST Code Search CLI tool.
 */
public class Main {

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AstSearchCommand()).execute(args);
        System.exit(exitCode);
    }
}
