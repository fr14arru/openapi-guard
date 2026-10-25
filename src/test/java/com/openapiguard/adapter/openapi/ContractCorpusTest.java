package com.openapiguard.adapter.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

import static com.openapiguard.TestSpecifications.fixture;
import static org.junit.jupiter.api.Assertions.*;

/** Pares declarativos del perfil histórico, expectativas explícitas e igualdad al repetir análisis. */
class ContractCorpusTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void comparesDocumentedContractPairs(Case example) throws Exception {
        var analyzer = new SwaggerContractAnalyzer();
        var report = analyzer.analyze(fixture("corpus/" + example.baseline()), fixture("corpus/" + example.candidate()));
        assertEquals(example.status(), report.status().name());
        assertEquals(example.verdict(), report.verdict().name());
        assertEquals(example.rules(), report.changes().stream().map(change -> change.ruleId()).sorted().toList());
        if (example.diagnostic() != null) {
            assertTrue(report.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(example.diagnostic())),
                    () -> report.diagnostics().toString());
        } else {
            assertTrue(report.diagnostics().isEmpty());
        }
        assertEquals(report, analyzer.analyze(fixture("corpus/" + example.baseline()), fixture("corpus/" + example.candidate())));
    }

    static Stream<Case> cases() throws Exception {
        try (InputStream manifest = ContractCorpusTest.class.getResourceAsStream("/openapi/corpus/cases.json")) {
            assertNotNull(manifest, "The reviewable contract corpus manifest must exist");
            JsonNode cases = new ObjectMapper().readTree(manifest);
            var result = new java.util.ArrayList<Case>();
            for (JsonNode item : cases) {
                var rules = new java.util.ArrayList<String>();
                item.path("rules").forEach(rule -> rules.add(rule.asText()));
                result.add(new Case(item.path("id").asText(), item.path("baseline").asText(), item.path("candidate").asText(),
                        item.path("status").asText(), item.path("verdict").asText(), rules.stream().sorted().toList(),
                        item.has("diagnostic") ? item.path("diagnostic").asText() : null));
            }
            assertTrue(result.size() >= 30, "The corpus must cover the documented matrix");
            assertEquals(result.size(), result.stream().map(Case::id).distinct().count(), "Case IDs must be unique");
            return result.stream();
        }
    }

    record Case(String id, String baseline, String candidate, String status, String verdict,
                List<String> rules, String diagnostic) {
        @Override public String toString() { return id; }
    }
}
