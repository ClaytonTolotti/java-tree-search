package com.astcodesearch.parser;

// Feature: cloudformation-yaml-support, Property 6: Segurança contra deserialização

import com.javatreesearch.parser.CloudFormationYamlParseException;
import com.javatreesearch.parser.CloudFormationYamlParser;
import com.javatreesearch.parser.CloudFormationYamlParserImpl;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for CloudFormationYamlParser deserialization security.
 *
 * **Validates: Requirements 15.1, 15.2**
 */
class CloudFormationYamlParserSecurityPropertiesTest {

    private static final Path SOURCE = Path.of("security-test.yaml");
    private final CloudFormationYamlParser parser = new CloudFormationYamlParserImpl();

    /**
     * Property 6: Segurança contra deserialização
     *
     * For any YAML payload attempting to instantiate an arbitrary Java class
     * via the !! syntax, parseAll() must reject it and throw
     * CloudFormationYamlParseException — never instantiating the class.
     *
     * **Validates: Requirements 15.1, 15.2**
     */
    @Property(tries = 50)
    void yamlWithJavaClassInstantiation_throwsCloudFormationYamlParseException(
            @ForAll("javaClassNames") String className
    ) {
        String maliciousYaml = "!!" + className + " {}";

        assertThrows(
                CloudFormationYamlParseException.class,
                () -> materialize(parser.parseAll(maliciousYaml, SOURCE)),
                "parseAll() must throw CloudFormationYamlParseException for !! payload: " + maliciousYaml
        );
    }

    /**
     * Property 6 (scalar variant): !!ClassName with a scalar value must also be rejected.
     *
     * **Validates: Requirements 15.1, 15.2**
     */
    @Property(tries = 50)
    void yamlWithJavaClassInstantiationScalar_throwsCloudFormationYamlParseException(
            @ForAll("javaClassNames") String className,
            @ForAll @NotBlank String scalarValue
    ) {
        String safe = scalarValue.replaceAll("[\\n\\r\\t\\x00-\\x1F]", "").trim();
        if (safe.isEmpty()) safe = "value";

        String maliciousYaml = "!!" + className + " " + safe;

        assertThrows(
                CloudFormationYamlParseException.class,
                () -> materialize(parser.parseAll(maliciousYaml, SOURCE)),
                "parseAll() must throw CloudFormationYamlParseException for !! scalar payload"
        );
    }

    /**
     * Property 6 (embedded in document): !! payloads embedded in a YAML document
     * must also be rejected.
     *
     * **Validates: Requirements 15.1, 15.2**
     */
    @Property(tries = 30)
    void yamlDocumentWithEmbeddedJavaClass_throwsCloudFormationYamlParseException(
            @ForAll("javaClassNames") String className
    ) {
        String maliciousYaml = "key: !!" + className + " {}";

        assertThrows(
                CloudFormationYamlParseException.class,
                () -> materialize(parser.parseAll(maliciousYaml, SOURCE)),
                "parseAll() must throw CloudFormationYamlParseException for embedded !! payload"
        );
    }

    // -------------------------------------------------------------------------
    // Providers
    // -------------------------------------------------------------------------

    @Provide
    Arbitrary<String> javaClassNames() {
        // Well-known Java classes that could be dangerous if instantiated
        return Arbitraries.of(
                "java.lang.Runtime",
                "java.lang.ProcessBuilder",
                "java.io.FileInputStream",
                "java.io.FileOutputStream",
                "java.net.URL",
                "java.net.Socket",
                "java.lang.Thread",
                "java.lang.System",
                "java.lang.Class",
                "java.lang.ClassLoader",
                "java.lang.reflect.Method",
                "java.util.Scanner",
                "javax.script.ScriptEngineManager",
                "com.sun.jndi.rmi.registry.RegistryContext",
                "org.springframework.context.support.ClassPathXmlApplicationContext"
        );
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static void materialize(Iterable<Object> iterable) {
        List<Object> list = new ArrayList<>();
        for (Object obj : iterable) {
            list.add(obj);
        }
    }
}
