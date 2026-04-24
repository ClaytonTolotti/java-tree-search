package com.javatreesearch.parser;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação de {@link CloudFormationYamlParser} que usa o SnakeYAML clássico
 * com todos os {@code Construct}s CloudFormation registrados via
 * {@link CloudFormationTagRegistry}.
 *
 * <p>Requirements: 5.1, 5.2, 5.3, 5.4, 5.5
 */
public class CloudFormationYamlParserImpl implements CloudFormationYamlParser {

    /**
     * Faz o parse de conteúdo YAML com suporte a tags CloudFormation.
     *
     * @param content conteúdo YAML como string
     * @param source  caminho do arquivo (para mensagens de erro)
     * @return Iterable de objetos parseados
     * @throws IllegalArgumentException         se {@code content} ou {@code source} for nulo
     * @throws CloudFormationYamlParseException se o YAML for sintaticamente inválido
     */
    @Override
    public Iterable<Object> parseAll(String content, Path source) {
        if (content == null) {
            throw new IllegalArgumentException("content must not be null");
        }
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }

        try {
            CloudFormationTagRegistry registry = new CloudFormationTagRegistryImpl();
            Constructor constructor = registry.buildConstructor();
            Yaml yaml = new Yaml(constructor);

            // loadAll is lazy — materialize into a list to force parsing now
            // so any YAMLException is thrown within this try block
            List<Object> result = new ArrayList<>();
            for (Object obj : yaml.loadAll(content)) {
                result.add(obj);
            }
            return result;
        } catch (YAMLException e) {
            throw new CloudFormationYamlParseException(source, e);
        }
    }
}
