package com.javatreesearch.integration;

import com.javatreesearch.engine.SearchEngineImpl;
import com.javatreesearch.model.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for error handling in SearchEngineImpl with CloudFormation YAML files.
 * Validates: Requirements 14.2, 14.3
 */
class CloudFormationErrorHandlingIntegrationTest {

    @TempDir
    Path repoRoot;

    /**
     * When a directory contains one invalid YAML file and one valid YAML file,
     * SearchEngineImpl.execute() should still find results from the valid file.
     * The invalid file triggers a ConfigIoException which is logged as [WARN] and skipped.
     * Req 14.2
     */
    @Test
    void invalidYamlInOneFileDoesNotStopProcessingOfOtherFiles() throws IOException {
        // Write a valid CloudFormation YAML file where BucketName is used as a YAML key
        // so it appears in the reference name (key path) and is directly searchable
        String validYaml =
            "AWSTemplateFormatVersion: '2010-09-09'\n" +
            "Parameters:\n" +
            "  TargetBucketName:\n" +
            "    Type: String\n" +
            "Resources:\n" +
            "  MyBucket:\n" +
            "    Type: AWS::S3::Bucket\n" +
            "    Properties:\n" +
            "      BucketName: !Ref TargetBucketName\n";
        Files.writeString(repoRoot.resolve("valid-template.yaml"), validYaml);

        // Write a syntactically invalid YAML file
        String invalidYaml =
            "AWSTemplateFormatVersion: '2010-09-09'\n" +
            "Resources:\n" +
            "  : invalid: yaml: content: [\n" +
            "    unclosed bracket\n";
        Files.writeString(repoRoot.resolve("invalid-template.yaml"), invalidYaml);

        SearchEngineImpl engine = new SearchEngineImpl();

        // Should not throw — invalid file is skipped with a [WARN] log
        SearchResult result = assertDoesNotThrow(
            () -> engine.execute("TargetBucketName", repoRoot, 3),
            "SearchEngineImpl.execute() should not throw when one YAML file is invalid"
        );

        // Results from the valid file should still be returned
        assertFalse(result.iterations().isEmpty(),
            "Should find results from the valid YAML file even though another file is invalid");

        // The reference name is the YAML key path; "TargetBucketName" appears as a key under Parameters
        boolean foundRef = result.iterations().stream()
            .anyMatch(iter -> iter.sourceReference().name().contains("TargetBucketName"));
        assertTrue(foundRef,
            "Should find 'TargetBucketName' from the valid CloudFormation template");
    }

    /**
     * When a YAML file has a list (sequence) as its root instead of a Map,
     * SearchEngineImpl.execute() should return empty results without throwing an exception.
     * Req 14.3
     */
    @Test
    void yamlFileWithListRootReturnsEmptyResultsWithoutException() throws IOException {
        // Write a YAML file whose root is a list, not a Map
        String listRootYaml =
            "- item1\n" +
            "- item2\n" +
            "- item3\n";
        Files.writeString(repoRoot.resolve("list-root.yaml"), listRootYaml);

        SearchEngineImpl engine = new SearchEngineImpl();

        // Should not throw — non-Map root is silently skipped
        SearchResult result = assertDoesNotThrow(
            () -> engine.execute("item1", repoRoot, 3),
            "SearchEngineImpl.execute() should not throw when a YAML file has a list root"
        );

        // No results expected since the list-root file produces no ConfigEntry
        assertTrue(result.iterations().isEmpty(),
            "Should return empty iterations for a YAML file with a list root");
    }

    /**
     * When a directory has a list-root YAML file alongside a valid YAML file,
     * the valid file is still processed normally.
     * Req 14.3
     */
    @Test
    void yamlFileWithListRootDoesNotPreventProcessingOfValidFiles() throws IOException {
        // Valid CloudFormation template — QueueResourceName is a key under Parameters
        // so it appears in the reference name (key path) and is directly searchable
        String validYaml =
            "AWSTemplateFormatVersion: '2010-09-09'\n" +
            "Parameters:\n" +
            "  QueueResourceName:\n" +
            "    Type: String\n" +
            "Resources:\n" +
            "  Queue:\n" +
            "    Type: AWS::SQS::Queue\n" +
            "    Properties:\n" +
            "      QueueName: !Ref QueueResourceName\n";
        Files.writeString(repoRoot.resolve("valid-template.yaml"), validYaml);

        // YAML file with a list root — should produce no entries, no exception
        String listRootYaml =
            "- item1\n" +
            "- item2\n";
        Files.writeString(repoRoot.resolve("list-root.yaml"), listRootYaml);

        SearchEngineImpl engine = new SearchEngineImpl();

        SearchResult result = assertDoesNotThrow(
            () -> engine.execute("QueueResourceName", repoRoot, 3),
            "SearchEngineImpl.execute() should not throw when one YAML file has a list root"
        );

        assertFalse(result.iterations().isEmpty(),
            "Should find results from the valid YAML file even when another has a list root");

        boolean foundRef = result.iterations().stream()
            .anyMatch(iter -> iter.sourceReference().name().contains("QueueResourceName"));
        assertTrue(foundRef,
            "Should find 'QueueResourceName' from the valid CloudFormation template");
    }
}
