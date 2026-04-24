package com.javatreesearch.parser;

/**
 * Registra todos os {@code Construct}s para as tags intrínsecas do CloudFormation
 * em um {@code Constructor} do SnakeYAML, centralizando o suporte a tags como
 * {@code !Ref}, {@code !Sub}, {@code !GetAtt}, {@code !If}, entre outras.
 *
 * <p>Implementações devem garantir que nenhum {@code Construct} execute código
 * arbitrário (segurança contra deserialização maliciosa).
 */
public interface CloudFormationTagRegistry {

    /**
     * Cria e retorna um Constructor do SnakeYAML com todos os
     * Constructs CloudFormation registrados.
     *
     * @return {@code Constructor} configurado com suporte a todas as tags
     *         intrínsecas do CloudFormation definidas em {@link CloudFormationTag}
     */
    org.yaml.snakeyaml.constructor.Constructor buildConstructor();
}
