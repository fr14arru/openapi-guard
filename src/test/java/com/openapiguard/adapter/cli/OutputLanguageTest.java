package com.openapiguard.adapter.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapiguard.adapter.openapi.SwaggerContractAnalyzer;
import com.openapiguard.application.CompareContracts;
import com.openapiguard.domain.ComparisonProfile;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import static com.openapiguard.TestSpecifications.fixture;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica presentación inglesa, JSON legible y conservación de datos Unicode originales. */
class OutputLanguageTest {
    @Test void jsonAndCiAreIndentedWithoutChangingTheirStructure() throws Exception {
        var report = new CompareContracts(new SwaggerContractAnalyzer(ComparisonProfile.RESPONSE_POLICY))
                .compare(fixture("parameters-baseline.yaml"), fixture("parameters-required.yaml"));
        var renderer = new ReportRenderer();
        var json = new ObjectMapper();
        for (String format : new String[]{"json", "ci"}) {
            String output = renderer.render(report, format);
            assertTrue(output.contains("\n  \""), output);
            var parsed = json.readTree(output);
            var semantic = format.equals("ci") ? parsed.path("report") : parsed;
            assertEquals(json.valueToTree(report), semantic);
        }
    }

    @Test void generatedTextAndRuleDescriptionsAreEnglishWhileUnicodeNamesArePreserved() throws Exception {
        var report = new CompareContracts(new SwaggerContractAnalyzer(ComparisonProfile.RESPONSE_POLICY))
                .compare(fixture("parameters-baseline.yaml").replace("limit", "límite"),
                        fixture("parameters-required.yaml").replace("limit", "límite"));
        String output = new ReportRenderer().render(report, "text");
        assertTrue(output.startsWith("Result: BREAKING | Status: COMPLETED"), output);
        assertTrue(output.contains("The parameter límite in query becomes required."), output);
        assertTrue(output.contains("Excluded facets:"), output);
    }

    @Test void inputErrorsAndHelpAreEnglish() {
        var command = new ComparisonCommand(new CompareContracts(new SwaggerContractAnalyzer()));
        var bytes = new ByteArrayOutputStream();
        var output = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        assertEquals(2, command.execute(new String[]{"--format=text", "missing.yaml", "next.yaml"}, output, output));
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("Could not read the contract."));
        bytes.reset();
        assertEquals(0, command.execute(new String[]{"--help"}, output, output));
        assertTrue(bytes.toString(StandardCharsets.UTF_8).startsWith("Usage:"));
        bytes.reset();
        assertEquals(2, command.execute(new String[]{"--profile=unknown", "old.yaml", "new.yaml"}, output, output));
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("Unknown profile."));
    }
}
