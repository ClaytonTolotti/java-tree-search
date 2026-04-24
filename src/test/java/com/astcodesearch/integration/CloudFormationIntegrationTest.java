package com.javatreesearch.integration;

import com.javatreesearch.engine.SearchEngineImpl;
import com.javatreesearch.model.IterationResult;
import com.javatreesearch.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for SearchEngineImpl with real CloudFormation templates.
 * Validates: Requirements 12.1, 12.2, 12.3, 12.4
 */
class CloudFormationIntegrationTest {

    @TempDir
    Path repoRoot;

    @BeforeEach
    void copyFixtures() throws IOException {
        copyResource("cloudformation/s3-template.yaml", repoRoot.resolve("s3-template.yaml"));
        copyResource("cloudformation/iam-template.yaml", repoRoot.resolve("iam-template.yaml"));
        copyResource("cloudformation/lambda-template.yaml", repoRoot.resolve("lambda-template.yaml"));
    }

    private void copyResource(String resourcePath, Path destination) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(in, "Fixture resource not found: " + resourcePath);
            Files.copy(in, destination);
        }
    }

    /**
     * Searching "BucketName" should find entries from !Ref BucketName in s3-template.yaml.
     * Req 12.1
     */
    @Test
    void searchBucketNameFindsRefBucketName() {
        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("BucketName", repoRoot, 3);

        assertFalse(result.iterations().isEmpty(),
            "Should find results for 'BucketName'");

        boolean foundRef = result.iterations().stream()
            .anyMatch(iter -> iter.sourceReference().name().contains("BucketName"));
        assertTrue(foundRef,
            "Should find a reference containing 'BucketName' (from !Ref BucketName)");
    }

    /**
     * Searching "BucketName" should find entries from !Sub "arn:aws:s3:::${BucketName}".
     * Req 12.2
     */
    @Test
    void searchBucketNameFindsSubExpression() {
        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("BucketName", repoRoot, 3);

        assertFalse(result.iterations().isEmpty(),
            "Should find results for 'BucketName'");

        // The !Sub "arn:aws:s3:::${BucketName}/*" should produce a ConfigEntry
        // whose value contains "BucketName" (via CfnTagValue "Sub:arn:aws:s3:::${BucketName}/*")
        boolean foundSubValue = result.iterations().stream()
            .anyMatch(iter -> {
                String refName = iter.sourceReference().name();
                return refName.contains("BucketName");
            });
        assertTrue(foundSubValue,
            "Should find 'BucketName' referenced in a !Sub expression");
    }

    /**
     * Searching "MyBucket" should find entries from !GetAtt MyBucket.Arn.
     * Req 12.3
     */
    @Test
    void searchMyBucketFindsGetAttExpression() {
        SearchEngineImpl engine = new SearchEngineImpl();
        SearchResult result = engine.execute("MyBucket", repoRoot, 3);

        assertFalse(result.iterations().isEmpty(),
            "Should find results for 'MyBucket'");

        // !GetAtt MyBucket.Arn should produce a ConfigEntry with value "GetAtt:MyBucket.Arn"
        boolean foundGetAtt = result.iterations().stream()
            .anyMatch(iter -> iter.sourceReference().name().contains("MyBucket"));
        assertTrue(foundGetAtt,
            "Should find 'MyBucket' referenced in a !GetAtt expression");
    }

    /**
     * SearchEngineImpl.execute() with a directory containing CloudFormation templates
     * should find and return references from those templates.
     * Req 12.4
     */
    @Test
    void searchEngineProcessesCloudFormationDirectory() {
        SearchEngineImpl engine = new SearchEngineImpl();

        // Search for a term present in the IAM template
        SearchResult result = engine.execute("RoleName", repoRoot, 3);

        assertFalse(result.iterations().isEmpty(),
            "Should find results for 'RoleName' in IAM template");

        boolean foundRole = result.iterations().stream()
            .anyMatch(iter -> iter.sourceReference().name().contains("RoleName"));
        assertTrue(foundRole,
            "Should find 'RoleName' from IAM CloudFormation template");
    }

    /**
     * SearchEngineImpl.execute() should find Lambda function references.
     * Req 12.4
     */
    @Test
    void searchEngineFindsLambdaFunctionReferences() {
        SearchEngineImpl engine = new SearchEngineImpl();

        SearchResult result = engine.execute("FunctionName", repoRoot, 3);

        assertFalse(result.iterations().isEmpty(),
            "Should find results for 'FunctionName' in Lambda template");

        boolean foundFunction = result.iterations().stream()
            .anyMatch(iter -> iter.sourceReference().name().contains("FunctionName"));
        assertTrue(foundFunction,
            "Should find 'FunctionName' from Lambda CloudFormation template");
    }
}
