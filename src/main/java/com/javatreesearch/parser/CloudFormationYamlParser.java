package com.javatreesearch.parser;

import java.nio.file.Path;

/**
 * Encapsula a criação e configuração do SnakeYAML clássico com suporte a tags
 * CloudFormation, isolando a dependência da biblioteca do restante do código.
 *
 * <p>Requirements: 5.1
 */
public interface CloudFormationYamlParser {

    /**
     * Faz o parse de conteúdo YAML com suporte a tags CloudFormation.
     * Tags não reconhecidas são tratadas como strings pelo GenericCfnConstruct.
     *
     * @param content conteúdo YAML como string
     * @param source  caminho do arquivo (para mensagens de erro)
     * @return Iterable de objetos parseados (Map, List, String, CfnTagValue, etc.)
     * @throws CloudFormationYamlParseException se o YAML for sintaticamente inválido
     * @throws IllegalArgumentException         se {@code content} ou {@code source} for nulo
     */
    Iterable<Object> parseAll(String content, Path source);
}
