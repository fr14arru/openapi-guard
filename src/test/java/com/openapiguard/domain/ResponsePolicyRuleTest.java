package com.openapiguard.domain;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Política pura de disponibilidad de códigos/formatos y procedencia de retiradas. */
class ResponsePolicyRuleTest {
    private final OperationId operation = new OperationId("/values", HttpMethod.GET);
    private final ResponseContract empty = new ResponseContract(Map.of(), "/components/responses/Empty", "/paths/~1values/get/responses/204");

    @Test void domainPolicyRunsWithoutParserAndRetainsStableIdentity() {
        var before = contract(Map.of("200", empty, "204", empty));
        var after = contract(Map.of("200", empty));
        var first = new ResponsePolicyRule().compare(before, after);
        assertEquals(ComparisonReport.Verdict.BREAKING, first.verdict());
        assertEquals("204", first.changes().getFirst().location().responseCode());
        var renamed = new ResponseContract(Map.of(), "/components/responses/Renamed", empty.usagePointer());
        assertEquals(first.changes().getFirst().id(), new ResponsePolicyRule().compare(contract(Map.of("200", empty, "204", renamed)), after).changes().getFirst().id());
    }

    @Test void unrelatedFallbackPreservesProvenBreakAndPartialCoverage() {
        var result = new ResponsePolicyRule().compare(contract(Map.of("200", empty)), contract(Map.of("4XX", empty)));
        assertEquals(ComparisonReport.Status.PARTIAL, result.status());
        assertEquals(ComparisonReport.Verdict.BREAKING, result.verdict());
        assertEquals(1, result.changes().size());
    }

    @Test void responseSnapshotsAreDeeplyImmutable() {
        var mutable = new HashMap<String, ResponseContract>();
        mutable.put("200", empty);
        var snapshot = contract(mutable);
        mutable.clear();
        assertEquals(1, snapshot.responses().get(operation).size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.responses().get(operation).clear());
    }

    private OperationContract contract(Map<String, ResponseContract> responses) {
        return new OperationContract("3.0.3", Map.of(operation.path(), Set.of(operation.method())),
                Map.of(), Map.of(), Map.of(), Map.of(operation, responses));
    }
}
