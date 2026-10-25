package com.openapiguard.adapter.openapi;

import com.openapiguard.domain.HttpMethod;
import com.openapiguard.domain.OperationId;
import com.openapiguard.domain.ParameterId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Normalización de herencia, override, referencias y validación de parámetros efectivos. */
class EffectiveParametersTest {
    private final SwaggerContractAnalyzer analyzer = new SwaggerContractAnalyzer();
    private static final OperationId GET = new OperationId("/pets", HttpMethod.GET);
    private static final OperationId POST = new OperationId("/pets", HttpMethod.POST);

    @Test
    void inheritsOverridesAndPreservesSourceWithoutRemovingInheritedParameters() throws Exception {
        var contract = analyzer.readContract(document("""
                    parameters:
                      - {name: limit, in: query, required: true, schema: {type: integer}}
                      - {name: trace, in: header, schema: {type: string}}
                    get:
                      parameters:
                        - {name: limit, in: query, schema: {type: integer}}
                      responses: {'200': {description: OK}}
                    post:
                      parameters: []
                      responses: {'200': {description: OK}}
                """));
        var get = contract.parameters().get(GET);
        assertEquals(2, get.size());
        var overridden = get.get(new ParameterId("limit", "query"));
        assertFalse(overridden.required());
        assertEquals("/paths/~1pets/get/parameters/0", overridden.definitionPointer());
        assertEquals(overridden.definitionPointer(), overridden.usagePointer());
        assertTrue(contract.parameters().get(POST).get(new ParameterId("limit", "query")).required());
        assertEquals("/paths/~1pets/parameters/0", contract.parameters().get(POST)
                .get(new ParameterId("limit", "query")).definitionPointer());
        assertThrows(UnsupportedOperationException.class, get::clear);
    }

    @Test
    void usesLocationAndHeaderCasingWithoutFoldingQueryNames() throws Exception {
        var contract = analyzer.readContract(document("""
                    parameters:
                      - {name: token, in: query, schema: {type: string}}
                      - {name: Token, in: query, schema: {type: string}}
                      - {name: X-Trace, in: header, required: true, schema: {type: string}}
                    get:
                      parameters:
                        - {name: x-trace, in: header, schema: {type: string}}
                      responses: {'200': {description: OK}}
                """));
        var parameters = contract.parameters().get(GET);
        assertEquals(3, parameters.size());
        assertTrue(parameters.containsKey(new ParameterId("token", "query")));
        assertTrue(parameters.containsKey(new ParameterId("Token", "query")));
        assertFalse(parameters.get(new ParameterId("X-TRACE", "header")).required());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Accept", "content-type", "AUTHORIZATION"})
    void ignoresSpecialHeaders(String name) throws Exception {
        var contract = analyzer.readContract(document("""
                    get:
                      parameters:
                        - {name: '%s', in: header, required: true, schema: {type: string}}
                      responses: {'200': {description: OK}}
                """.formatted(name)));
        assertTrue(contract.parameters().get(GET).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.3", "3.1.1"})
    void internalParameterReferencesPreserveDefinitionAndUsage(String version) throws Exception {
        String content = document("""
                    get:
                      parameters:
                        - {$ref: '#/components/parameters/Limit'}
                      responses: {'200': {description: OK}}
                """) + """
                components:
                  parameters:
                    Limit: {name: limit, in: query, required: true, schema: {type: integer}}
                """;
        var parameter = analyzer.readContract(content.replace("3.0.3", version)).parameters().get(GET)
                .get(new ParameterId("limit", "query"));
        assertTrue(parameter.required());
        assertEquals("/components/parameters/Limit", parameter.definitionPointer());
        assertEquals("/paths/~1pets/get/parameters/0", parameter.usagePointer());
    }

    @Test
    void orderDoesNotChangeEffectiveIdentity() throws Exception {
        String first = "- {name: a, in: query, schema: {type: string}}\n";
        String second = "- {name: b, in: cookie, required: true, schema: {type: string}}\n";
        var old = analyzer.readContract(withParameters(first + second)).parameters().get(GET);
        var reordered = analyzer.readContract(withParameters(second + first)).parameters().get(GET);
        assertEquals(old.keySet(), reordered.keySet());
        old.forEach((id, parameter) -> assertEquals(parameter.required(), reordered.get(id).required()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "- {name: a, in: query, schema: {type: string}}\n- {name: a, in: query, schema: {type: string}}\n",
            "- {name: X-A, in: header, schema: {type: string}}\n- {name: x-a, in: header, schema: {type: string}}\n",
            "- {name: a, in: unknown, schema: {type: string}}\n",
            "- {name: a, in: query, required: 'yes', schema: {type: string}}\n",
            "- {name: a, in: path, required: false, schema: {type: string}}\n",
            "- {name: a, in: path, schema: {type: string}}\n"
    })
    void invalidParametersCannotBecomeEmptyEffectiveMaps(String parameters) {
        assertThrows(IllegalArgumentException.class, () -> analyzer.readContract(withParameters(parameters)));
    }

    @Test
    void cyclesAndWrongReferenceTargetsAreRejected() {
        String base = withParameters("- {$ref: '#/components/parameters/A'}\n");
        assertThrows(IllegalArgumentException.class, () -> analyzer.readContract(base + """
                components:
                  parameters:
                    A: {$ref: '#/components/parameters/B'}
                    B: {$ref: '#/components/parameters/A'}
                """));
        assertThrows(IllegalArgumentException.class, () -> analyzer.readContract(base + """
                components:
                  parameters:
                    A: {type: string}
                """));
    }

    @Test
    void everyTemplateVariableMustHaveAnEffectiveRequiredPathParameter() {
        String missing = document("""
                    get:
                      responses: {'200': {description: OK}}
                """).replace("/pets:", "/pets/{id}:");
        assertThrows(IllegalArgumentException.class, () -> analyzer.readContract(missing));
    }

    private static String withParameters(String parameters) {
        return document("    get:\n      parameters:\n" + parameters.indent(8)
                + "      responses: {'200': {description: OK}}\n");
    }

    private static String document(String pathItem) {
        return "openapi: 3.0.3\ninfo: {title: Parameters, version: '1'}\npaths:\n  /pets:\n" + pathItem;
    }
}
