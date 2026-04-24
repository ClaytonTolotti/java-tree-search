package com.javatreesearch.parser;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;

import java.util.function.Function;

import org.yaml.snakeyaml.nodes.Node;

/**
 * Implementação de {@link CloudFormationTagRegistry} que registra todos os
 * {@code Construct}s para as tags intrínsecas do CloudFormation em um
 * {@code SafeConstructor} do SnakeYAML.
 *
 * <p>Usa {@code SafeConstructor} como base para garantir que nenhuma classe
 * Java arbitrária seja instanciada durante o parsing (segurança contra
 * deserialização maliciosa — CVE-2022-1471 e similares).
 *
 * <p>Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
public class CloudFormationTagRegistryImpl implements CloudFormationTagRegistry {

    /**
     * Subclasse interna que expõe {@code yamlConstructors} (campo protegido)
     * via método público, e expõe {@code constructObject} como função pública,
     * permitindo o registro de constructs sem reflexão.
     *
     * Extends {@code Constructor} (which extends {@code SafeConstructor}) to
     * satisfy the return type of {@link CloudFormationTagRegistry#buildConstructor()}.
     */
    private static final class ExtendedConstructor extends Constructor {
        ExtendedConstructor() {
            super(new LoaderOptions());
        }

        void addConstruct(Tag tag, org.yaml.snakeyaml.constructor.Construct construct) {
            yamlConstructors.put(tag, construct);
        }

        /**
         * Exposes the protected {@code constructObject} method as a public
         * {@link Function} so that constructs can resolve child nodes recursively.
         */
        Function<Node, Object> nodeResolver() {
            return this::constructObject;
        }
    }

    /**
     * Cria e retorna um {@code Constructor} configurado com todos os
     * {@code Construct}s CloudFormation registrados.
     *
     * <p>Cada chamada retorna uma nova instância (sem estado compartilhado).
     *
     * @return {@code Constructor} não nulo com suporte a todas as tags
     *         definidas em {@link CloudFormationTag} e fallback genérico
     */
    @Override
    public Constructor buildConstructor() {
        ExtendedConstructor constructor = new ExtendedConstructor();
        Function<Node, Object> resolver = constructor.nodeResolver();

        for (CloudFormationTag tag : CloudFormationTag.values()) {
            org.yaml.snakeyaml.constructor.Construct construct = createConstruct(tag, resolver);
            constructor.addConstruct(new Tag(tag.yamlTag()), construct);
        }

        // Fallback para tags desconhecidas
        constructor.addConstruct(null, new GenericCfnConstruct());

        return constructor;
    }

    private org.yaml.snakeyaml.constructor.Construct createConstruct(
            CloudFormationTag tag,
            Function<Node, Object> resolver) {
        return switch (tag) {
            case REF          -> new ScalarCfnConstruct("Ref");
            case SUB          -> new ScalarCfnConstruct("Sub");
            case IMPORT_VALUE -> new ScalarCfnConstruct("ImportValue");
            case BASE64       -> new ScalarCfnConstruct("Base64");
            case IF           -> new SequenceCfnConstruct("If", resolver);
            case SELECT       -> new SequenceCfnConstruct("Select", resolver);
            case JOIN         -> new SequenceCfnConstruct("Join", resolver);
            case FIND_IN_MAP  -> new SequenceCfnConstruct("FindInMap", resolver);
            case CIDR         -> new SequenceCfnConstruct("Cidr", resolver);
            case SPLIT        -> new SequenceCfnConstruct("Split", resolver);
            case GET_ATT      -> new ScalarOrSequenceCfnConstruct("GetAtt", resolver);
            case TRANSFORM    -> new MappingCfnConstruct("Transform", resolver);
        };
    }
}
