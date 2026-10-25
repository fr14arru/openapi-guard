package com.openapiguard.adapter.openapi;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.openapiguard.application.ContractAnalyzer;
import com.openapiguard.domain.ComparisonReport;
import com.openapiguard.domain.ComparisonProfile;
import com.openapiguard.domain.RequiredParametersRule;
import com.openapiguard.domain.RequestBodiesRule;
import com.openapiguard.domain.SchemaValuesRule;
import com.openapiguard.domain.DetectedChange;
import com.openapiguard.domain.HttpMethod;
import com.openapiguard.domain.OperationContract;
import com.openapiguard.domain.RemovedOperationsRule;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.yaml.snakeyaml.LoaderOptions;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Valida ambos documentos y ejecuta reglas sobre snapshots, sin resolver recursos externos. */
public final class SwaggerContractAnalyzer implements ContractAnalyzer {
    /** Presupuesto UTF-8 por documento compartido con la frontera de lectura CLI. */
    public static final int MAX_DOCUMENT_BYTES = 5 * 1024 * 1024;
    private final ObjectMapper inputMapper;
    private final RemovedOperationsRule rule = new RemovedOperationsRule();
    private final ComparisonProfile profile;

    /** Selecciona el perfil histórico de presencia de parámetros. */
    public SwaggerContractAnalyzer() {
        this(ComparisonProfile.PARAMETER_PRESENCE);
    }

    /** Configura cobertura y lectura estricta, sin descargar ni abrir referencias.
     * @param profile cobertura versionada del análisis; no expresa compatibilidad global
     */
    public SwaggerContractAnalyzer(ComparisonProfile profile) {
        this.profile = Objects.requireNonNull(profile);
        LoaderOptions yamlOptions = new LoaderOptions();
        yamlOptions.setMaxAliasesForCollections(0);
        yamlOptions.setNestingDepthLimit(128);
        yamlOptions.setCodePointLimit(MAX_DOCUMENT_BYTES);
        YAMLFactory factory = YAMLFactory.builder().loaderOptions(yamlOptions)
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(128).build())
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
        inputMapper = new ObjectMapper(factory)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    /** Expone el perfil seleccionado al construir el analizador.
     * @return perfil publicado del analizador
     */
    @Override
    public ComparisonProfile profile() {
        return profile;
    }

    /** Una entrada fallida no produce retiradas especulativas; agrega solo reglas habilitadas.
     * @param baseline contrato anterior usado como referencia de compatibilidad
     * @param candidate contrato propuesto, cuya compatibilidad se evalúa
     * @return informe limitado al perfil con rupturas y diagnósticos
     */
    @Override
    public ComparisonReport analyze(String baseline, String candidate) {
        List<ComparisonReport.Diagnostic> diagnostics = new ArrayList<>();
        OperationContract oldContract = read(baseline, "baseline", diagnostics);
        OperationContract newContract = read(candidate, "candidate", diagnostics);
        if (oldContract == null || newContract == null) {
            return ComparisonReport.of(profile, List.of(), diagnostics, true);
        }
        if (!oldContract.openApiVersion().substring(0, 3)
                .equals(newContract.openApiVersion().substring(0, 3))) {
            return ComparisonReport.of(profile, List.of(), List.of(new ComparisonReport.Diagnostic(
                    "UNSUPPORTED_VERSION_PAIR", "comparison", "/openapi",
                    "OpenAPI 3.0 and 3.1 families cannot be compared with each other.")), true);
        }
        var operations = rule.compare(oldContract, newContract);
        if (profile == ComparisonProfile.OPERATIONS_ONLY) {
            return operations;
        }
        var parameters = new RequiredParametersRule().compare(oldContract, newContract);
        List<DetectedChange> changes = new ArrayList<>(operations.changes());
        changes.addAll(parameters.changes());
        if (profile.includesRequestBodies()) {
            var bodies = new RequestBodiesRule().compare(oldContract, newContract);
            changes.addAll(bodies.changes());
            diagnostics.addAll(bodies.diagnostics());
        }
        if (profile.includesSchemas()) {
            var schemas = new SchemaValuesRule().compare(oldContract, newContract, profile);
            changes.addAll(schemas.changes());
            diagnostics.addAll(schemas.diagnostics());
        }
        if (profile.includesResponsePolicy()) {
            var responses = new com.openapiguard.domain.ResponsePolicyRule().compare(oldContract, newContract);
            changes.addAll(responses.changes());
            diagnostics.addAll(responses.diagnostics());
        }
        changes.sort(Comparator.comparing((DetectedChange change) -> change.location().path())
                .thenComparing(change -> change.location().method() == null ? "" : change.location().method().name())
                .thenComparing(DetectedChange::ruleId)
                .thenComparing(change -> change.location().parameter() == null ? "" : change.location().parameter().in())
                .thenComparing(change -> change.location().parameter() == null ? "" : change.location().parameter().name())
                .thenComparing(change -> change.location().responseCode() == null ? "" : change.location().responseCode())
                .thenComparing(change -> change.location().mediaType() == null ? "" : change.location().mediaType()));
        diagnostics.addAll(operations.diagnostics());
        diagnostics.addAll(parameters.diagnostics());
        return ComparisonReport.of(profile, changes, diagnostics, false);
    }

    /** Convierte errores de validación/normalización en diagnósticos de entrada. */
    private OperationContract read(String content, String source,
                                   List<ComparisonReport.Diagnostic> diagnostics) {
        try {
            return readContract(content, profile != ComparisonProfile.OPERATIONS_ONLY);
        } catch (Exception exception) {
            diagnostics.add(new ComparisonReport.Diagnostic("INVALID_OR_UNSUPPORTED_INPUT",
                    source, "", exception.getMessage() == null
                    ? exception.getClass().getSimpleName() : exception.getMessage()));
            return null;
        }
    }

    /** Acceso de paquete para comprobar snapshots y procedencia en tests. */
    OperationContract readContract(String content) throws IOException {
        return readContract(content, true);
    }

    /** Valida fronteras globales y normaliza solo las facetas activas del perfil. */
    private OperationContract readContract(String content, boolean includeParameters) throws IOException {
        if (content == null || content.isBlank()
                || content.getBytes(StandardCharsets.UTF_8).length > MAX_DOCUMENT_BYTES) {
            throw new IllegalArgumentException("Empty document or document larger than 5 MiB.");
        }
        // Jackson consumes YAML events directly, so LoaderOptions' composer alias
        // limit alone does not reject aliases. Check events before materializing a tree.
        try (YAMLParser parser = (YAMLParser) inputMapper.getFactory().createParser(content)) {
            while (parser.nextToken() != null) {
                if (parser.isCurrentAlias()) {
                    throw new IllegalArgumentException("YAML aliases are unsupported.");
                }
            }
        }
        JsonNode tree = inputMapper.readTree(content);
        String version = tree.path("openapi").asText();
        if (!version.matches("3\\.[01]\\.\\d+")) {
            throw new IllegalArgumentException("Only OpenAPI 3.0.x and 3.1.x are supported.");
        }
        if (!tree.path("paths").isObject()) {
            throw new IllegalArgumentException("An explicit paths object is required.");
        }
        checkReferences(tree, tree);
        var entries = tree.path("paths").properties().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            if (entry.getKey().startsWith("x-")) {
                continue;
            }
            if (!entry.getKey().startsWith("/") || !entry.getValue().isObject()
                    || entry.getValue().has("$ref")) {
                throw new IllegalArgumentException("Unsupported Path Item: " + entry.getKey());
            }
        }
        ParseOptions options = new ParseOptions();
        options.setResolve(false);
        options.setResolveFully(false);
        var parsed = new OpenAPIV3Parser().readContents(tree.toString(), null, options);
        if (parsed.getOpenAPI() == null || (parsed.getMessages() != null
                && !parsed.getMessages().isEmpty())) {
            throw new IllegalArgumentException("Invalid or unsupported contract: "
                    + parsed.getMessages());
        }
        Map<String, Set<HttpMethod>> paths = new HashMap<>();
        if (parsed.getOpenAPI().getPaths() != null) {
            parsed.getOpenAPI().getPaths().forEach((path, item) -> {
                Set<HttpMethod> methods = new HashSet<>();
                item.readOperationsMap().keySet().forEach(method ->
                        methods.add(HttpMethod.valueOf(method.name())));
                paths.put(path, methods);
            });
        }
        var parameters = includeParameters ? new EffectiveParametersReader().read(tree, paths) : Map.<com.openapiguard.domain.OperationId, Map<com.openapiguard.domain.ParameterId, com.openapiguard.domain.EffectiveParameter>>of();
        var bodies = profile.includesRequestBodies()
                ? new RequestBodiesReader().read(tree, paths) : Map.<com.openapiguard.domain.OperationId, com.openapiguard.domain.RequestBodyContract>of();
        return new OperationContract(version, paths, parameters, bodies,
                profile.includesSchemas()
                        ? new SchemaValuesReader(tree, profile.includesObjects(), profile.includesArrays()).read(paths, parameters, bodies) : Map.of(),
                profile.includesResponsePolicy() ? new ResponseContractsReader().read(tree, paths) : Map.of());
    }

    /** Comprueba también referencias no usadas, sin acceder a sus recursos. */
    private static void checkReferences(JsonNode node, JsonNode root) {
        if (node.isObject() && node.has("$ref")) {
            JsonNode ref = node.get("$ref");
            if (!ref.isTextual() || !ref.asText().startsWith("#/")) {
                throw new IllegalArgumentException("External or unsupported reference; it will not be resolved.");
            }
            if (root.at(ref.asText().substring(1)).isMissingNode()) {
                throw new IllegalArgumentException("Internal reference does not exist: " + ref.asText());
            }
        }
        node.elements().forEachRemaining(child -> checkReferences(child, root));
    }
}
