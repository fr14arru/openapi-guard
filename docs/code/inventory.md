**Transición MIT (22 de octubre):** prepareAttributions incluye LICENSE.txt desde la licencia raíz y devuelve productLicenseStatus MIT, productLicenseFile y productLicenseSha256. prepare-evaluation.ps1 ya no copia términos de piloto; genera estados OPEN_SOURCE_LOCAL_PACKAGE u OPEN_SOURCE_CANDIDATE_PUBLICATION_PENDING. Las pruebas de atribuciones y paquete real comprueban MIT y ausencia de condiciones comerciales. El verificador de integridad conserva compatibilidad con paquetes históricos. prepare-test-delivery.mjs permanece como utilidad histórica, no como flujo actual. Ver [distribución actual](../open-source.md).

# Inventario completo del código y recursos

Aportaciones voluntarias (25 de octubre): .github/FUNDING.yml contiene solo una URL custom de PayPal. Es metadato de GitHub para el botón Sponsor, sin conexión con el motor, la CLI ni las reglas. Los enlaces de README y guías usan la misma URL pública; no se añaden dependencias ni condiciones a MIT.

Regresión de exportación (25 de octubre): `SwaggerContractAnalyzerTest.parserValidationAndDanglingReferencesFailClosed` parametriza LF y CRLF. La eliminación de `info.title` emplea el salto real y comprueba que la mutación cambió el contrato antes de exigir FAILED. Evita que un cambio de finales de línea convierta la entrada inválida en un contrato válido. No modifica el analizador de producción.

Entrega de prueba de tienda (22 de octubre): [scripts/prepare-test-delivery.mjs](../../scripts/prepare-test-delivery.mjs) exporta prepareTestDelivery; su helper approvedLicense comprueba el blob aprobado y extrae las cláusulas, separando procedencia del motor y commit de empaquetado. [scripts/test-delivery.test.mjs](../../scripts/test-delivery.test.mjs) utiliza fixture/persist para paquetes sintéticos y prueba preservación, entradas alteradas, filtrado de documentos, licencia/fuentes, destinos existentes y transporte ZIP. docs/test-delivery-readme.txt contiene las instrucciones autónomas incluidas. No se leen los borradores privados para empaquetar.

Actualización de atribuciones (19 de octubre): scripts/prepare-attributions.mjs añade prepareAttributions (conjunto exacto, hashes, textos/fuentes y avisos) y evidencePath (rutas regulares sin traversal ni symlinks). scripts/prepare-evaluation.ps1 excluye clases de la evidencia textual, calcula el fingerprint ordinal del loader y ejecuta esa validación antes de cerrar la integridad. licensing/catalog.json y licensing/files/ conservan evidencia original; licensing/sources/ contiene ocho archivos de fuentes. .gitattributes protege sus bytes. scripts/attributions.test.mjs y scripts/attribution-package.test.mjs prueban componentes/evidencia alterados, fuentes EPL, empaquetado real y checkout Windows.

Publicación opcional de PR: [pr-comment.mjs](../../scripts/pr-comment.mjs) declara `commentMarker`, `runOrder`, `newer`, `context`, `commentBody` y `publishPrComment`, con helpers locales `request` y `current`. Usa REST sin dependencias, escapa el informe, valida origen HTTPS, pagina y actualiza el comentario del bot. [pr-comment.test.mjs](../../scripts/pr-comment.test.mjs) prueba creación/actualización, desaparición de rupturas, forks/eventos, ejecuciones obsoletas, usuarios que imitan marcadores, permisos, contenido y límites. action-runner añade opción/estado y aísla credenciales; prepare-evaluation copia el módulo y verify-package exige cobertura para Actions que ofrecen pr-comment, conservando verificación de paquetes v2 anteriores. github-presentation exporta también `markdown` para reutilizar el escape. El workflow de desarrollo incluye las pruebas del publicador con API simulada, manteniendo permisos de lectura.

Este índice permite comprobar la cobertura de la guía y abrir directamente cada archivo. Los nombres de miembros conservan los identificadores ingleses del código. Las explicaciones de responsabilidad, invariantes y algoritmos están en los capítulos enlazados.

Estado de este inventario: 39 archivos Java de producción (incluidos package-info), 24 archivos Java de pruebas/utilidades y 76 recursos de tests. También se enumeran configuración, scripts y ejemplos. No incluye artefactos generados bajo target ni configuración privada del IDE.

## Producción: archivos y declaraciones

El índice enumera nombres de métodos y constructores declarados, incluidas sobrecargas y helpers privados; el enlace al código muestra firmas y cuerpos. Los constructores compactos se indican también. Los accessors y métodos de valor generados por records no se repiten: sus campos y semántica se explican en domain.md. Las clases anidadas se describen en el capítulo correspondiente.

### ComparisonCommand.java

[src/main/java/com/openapiguard/adapter/cli/ComparisonCommand.java](../../src/main/java/com/openapiguard/adapter/cli/ComparisonCommand.java) · [Explicación](runtime.md)

Miembros declarados: `ComparisonCommand`, `execute`, `read`. Parsing, formatos y escritura se separan en las tres piezas siguientes.

### CliOptions.java

[CliOptions](../../src/main/java/com/openapiguard/adapter/cli/CliOptions.java): record exterior con `baseline`, `candidate`, `format`, `profile`, `output`; `profiles` mantiene aliases estables y `parse` rechaza opciones repetidas/ambiguas sin E/S.

### ReportRenderer.java

[ReportRenderer](../../src/main/java/com/openapiguard/adapter/cli/ReportRenderer.java): `render` produce JSON/texto/Markdown; `printText` presenta un renglón por hallazgo con posiciones candidatas; `escape` y `markdown` presentan datos del contrato sin introducir HTML/enlaces. [Guía](../product-usability.md).

### ReportFiles.java

[ReportFiles](../../src/main/java/com/openapiguard/adapter/cli/ReportFiles.java): `validate` protege inputs y aliases existentes; `write` guarda UTF-8 con temporal y reemplazo. No es un repositorio de dominio ni un puerto nuevo.

### OpenApiGuardApplication.java

[src/main/java/com/openapiguard/adapter/cli/OpenApiGuardApplication.java](../../src/main/java/com/openapiguard/adapter/cli/OpenApiGuardApplication.java) · [Explicación](runtime.md)

Miembros declarados: `command`, `comparison`, `main`, `start`.

### package-info.java

[src/main/java/com/openapiguard/adapter/cli/package-info.java](../../src/main/java/com/openapiguard/adapter/cli/package-info.java) · [Explicación](runtime.md)

Define el tipo, sus componentes/valores o el contrato del paquete; sin métodos explícitos adicionales.

### EffectiveParametersReader.java

[src/main/java/com/openapiguard/adapter/openapi/EffectiveParametersReader.java](../../src/main/java/com/openapiguard/adapter/openapi/EffectiveParametersReader.java) · [Explicación](openapi.md)

Miembros declarados: `escape`, `invalid`, `list`, `read`.

### package-info.java

[src/main/java/com/openapiguard/adapter/openapi/package-info.java](../../src/main/java/com/openapiguard/adapter/openapi/package-info.java) · [Explicación](openapi.md)

Define el tipo, sus componentes/valores o el contrato del paquete; sin métodos explícitos adicionales.

### RequestBodiesReader.java

[src/main/java/com/openapiguard/adapter/openapi/RequestBodiesReader.java](../../src/main/java/com/openapiguard/adapter/openapi/RequestBodiesReader.java) · [Explicación](openapi.md)

Miembros declarados: `read`.

### ResponseContractsReader.java

[src/main/java/com/openapiguard/adapter/openapi/ResponseContractsReader.java](../../src/main/java/com/openapiguard/adapter/openapi/ResponseContractsReader.java) · [Explicación](openapi.md)

Miembros declarados: `escape`, `read`.

### SchemaValuesReader.java

[src/main/java/com/openapiguard/adapter/openapi/SchemaValuesReader.java](../../src/main/java/com/openapiguard/adapter/openapi/SchemaValuesReader.java) · [Explicación](openapi.md)

Miembros declarados: `bound`, `cacheable`, `content`, `decimal`, `escape`, `flag`, `length`, `opaque`, `read`, `referenceObject`, `scalar`, `schema`, `SchemaValuesReader` (3 declaraciones).

### SwaggerContractAnalyzer.java

[src/main/java/com/openapiguard/adapter/openapi/SwaggerContractAnalyzer.java](../../src/main/java/com/openapiguard/adapter/openapi/SwaggerContractAnalyzer.java) · [Explicación](openapi.md)

Miembros declarados: `analyze`, `checkReferences`, `profile`, `read`, `readContract` (2 declaraciones), `SwaggerContractAnalyzer` (2 declaraciones).

### CompareContracts.java

[src/main/java/com/openapiguard/application/CompareContracts.java](../../src/main/java/com/openapiguard/application/CompareContracts.java) · [Explicación](runtime.md)

Miembros declarados: `compare`, `CompareContracts`, `profile`.

### ContractAnalyzer.java

[src/main/java/com/openapiguard/application/ContractAnalyzer.java](../../src/main/java/com/openapiguard/application/ContractAnalyzer.java) · [Explicación](runtime.md)

Miembros declarados: `analyze`, `profile`.

### package-info.java

[src/main/java/com/openapiguard/application/package-info.java](../../src/main/java/com/openapiguard/application/package-info.java) · [Explicación](runtime.md)

Define el tipo, sus componentes/valores o el contrato del paquete; sin métodos explícitos adicionales.

### ArrayShape.java

[src/main/java/com/openapiguard/domain/ArrayShape.java](../../src/main/java/com/openapiguard/domain/ArrayShape.java) · [Explicación](domain.md)

Define el tipo, sus componentes/valores o el contrato del paquete; sin métodos explícitos adicionales.

Record `ArrayShape`: `SchemaValueContract items, int minItems, int maxItems`.

### ComparisonProfile.java

[src/main/java/com/openapiguard/domain/ComparisonProfile.java](../../src/main/java/com/openapiguard/domain/ComparisonProfile.java) · [Explicación](domain.md)

Miembros declarados: `ComparisonProfile`, `excludedFacets`, `id`, `includesArrays`, `includesObjects`, `includesRequestBodies`, `includesResponsePolicy`, `includesSchemas`.

### ComparisonReport.java

[src/main/java/com/openapiguard/domain/ComparisonReport.java](../../src/main/java/com/openapiguard/domain/ComparisonReport.java) · [Explicación](domain.md)

Miembros declarados: `ComparisonReport (constructor compacto)`, `of` (2 declaraciones).

Record `ComparisonReport`: `String profileId, Status status, Verdict verdict, List<String> excludedFacets, List<DetectedChange> changes, List<Diagnostic> diagnostics`.

Record `Diagnostic`: `String code, String source, String location, String description`.

### DetectedChange.java

[src/main/java/com/openapiguard/domain/DetectedChange.java](../../src/main/java/com/openapiguard/domain/DetectedChange.java) · [Explicación](domain.md)

Miembros declarados: `hash`, `Location` (2 declaraciones), `parameterRequired`, `removed`, `requestBody`, `responsePolicy`, `schema`, `token`, `constraintChanges`, `schemaAt`, `detail`, `boundText`, `lengthText`, `enumText`.

Record `DetectedChange`: `String id, String ruleId, Severity severity, Location location, String description`. `constraintChanges` explica diferencias del nodo probado en orden baseline → candidate; `schemaAt` busca por pointer con recorrido de identidad; `detail`, `boundText`, `lengthText` y `enumText` formatean cambios deterministas y acotan enums. No alteran reglas ni IDs.

Record `Location`: `String path, HttpMethod method, String baselinePointer, String candidatePointer, ParameterId parameter, String baselineUsagePointer, String candidateUsagePointer, String direction, String responseCode, String mediaType`.

### EffectiveParameter.java

[src/main/java/com/openapiguard/domain/EffectiveParameter.java](../../src/main/java/com/openapiguard/domain/EffectiveParameter.java) · [Explicación](domain.md)

Miembros declarados: `EffectiveParameter (constructor compacto)`.

Record `EffectiveParameter`: `ParameterId id, boolean required, String definitionPointer, String usagePointer, String serialization, boolean presenceSupported`.

### HttpMethod.java

[src/main/java/com/openapiguard/domain/HttpMethod.java](../../src/main/java/com/openapiguard/domain/HttpMethod.java) · [Explicación](domain.md)

Define el tipo, sus componentes/valores o el contrato del paquete; sin métodos explícitos adicionales.

### ObjectShape.java

[src/main/java/com/openapiguard/domain/ObjectShape.java](../../src/main/java/com/openapiguard/domain/ObjectShape.java) · [Explicación](domain.md)

Miembros declarados: `ObjectShape`, `ObjectShape (constructor compacto)`.

Record `ObjectShape`: `Map<String, SchemaValueContract> properties, Set<String> required, boolean additionalProperties, Map<String, String> accessModes`.

### OperationContract.java

[src/main/java/com/openapiguard/domain/OperationContract.java](../../src/main/java/com/openapiguard/domain/OperationContract.java) · [Explicación](domain.md)

Miembros declarados: `OperationContract` (4 declaraciones), `OperationContract (constructor compacto)`.

Record `OperationContract`: `String openApiVersion, Map<String, Set<HttpMethod>> paths, Map<OperationId, Map<ParameterId, EffectiveParameter>> parameters, Map<OperationId, RequestBodyContract> requestBodies, Map<SchemaUseId, SchemaUse> schemas, Map<OperationId, Map<String, ResponseContract>> responses`.

### OperationId.java

[src/main/java/com/openapiguard/domain/OperationId.java](../../src/main/java/com/openapiguard/domain/OperationId.java) · [Explicación](domain.md)

Miembros declarados: `OperationId (constructor compacto)`.

Record `OperationId`: `String path, HttpMethod method`.

### package-info.java

[src/main/java/com/openapiguard/domain/package-info.java](../../src/main/java/com/openapiguard/domain/package-info.java) · [Explicación](domain.md)

Define el tipo, sus componentes/valores o el contrato del paquete; sin métodos explícitos adicionales.

### ParameterId.java

[src/main/java/com/openapiguard/domain/ParameterId.java](../../src/main/java/com/openapiguard/domain/ParameterId.java) · [Explicación](domain.md)

Miembros declarados: `compareTo`, `ParameterId (constructor compacto)`.

Record `ParameterId`: `String name, String in`.

### RemovedOperationsRule.java

[src/main/java/com/openapiguard/domain/RemovedOperationsRule.java](../../src/main/java/com/openapiguard/domain/RemovedOperationsRule.java) · [Explicación](domain.md)

Miembros declarados: `compare`, `shapes`, `templateShape`.

### RequestBodiesRule.java

[src/main/java/com/openapiguard/domain/RequestBodiesRule.java](../../src/main/java/com/openapiguard/domain/RequestBodiesRule.java) · [Explicación](domain.md)

Miembros declarados: `compare`, `covers`, `supported`.

### RequestBodyContract.java

[src/main/java/com/openapiguard/domain/RequestBodyContract.java](../../src/main/java/com/openapiguard/domain/RequestBodyContract.java) · [Explicación](domain.md)

Miembros declarados: `RequestBodyContract (constructor compacto)`.

Record `RequestBodyContract`: `boolean required, Set<String> mediaTypes, String definitionPointer, String usagePointer`.

### RequiredParametersRule.java

[src/main/java/com/openapiguard/domain/RequiredParametersRule.java](../../src/main/java/com/openapiguard/domain/RequiredParametersRule.java) · [Explicación](domain.md)

Miembros declarados: `compare`, `diagnostic`.

### ResponseContract.java

[src/main/java/com/openapiguard/domain/ResponseContract.java](../../src/main/java/com/openapiguard/domain/ResponseContract.java) · [Explicación](domain.md)

Miembros declarados: `ResponseContract (constructor compacto)`.

Record `ResponseContract`: `Map<String, String> representations, String definitionPointer, String usagePointer`.

### ResponsePolicyRule.java

[src/main/java/com/openapiguard/domain/ResponsePolicyRule.java](../../src/main/java/com/openapiguard/domain/ResponsePolicyRule.java) · [Explicación](domain.md)

Miembros declarados: `compare`, `compareMedia`, `concrete`, `review`.

### ScalarValue.java

[src/main/java/com/openapiguard/domain/ScalarValue.java](../../src/main/java/com/openapiguard/domain/ScalarValue.java) · [Explicación](domain.md)

Miembros declarados: `integer`, `ScalarValue (constructor compacto)`.

Record `ScalarValue`: `String kind, String value`.

### SchemaInclusion.java

[src/main/java/com/openapiguard/domain/SchemaInclusion.java](../../src/main/java/com/openapiguard/domain/SchemaInclusion.java) · [Explicación](inclusion.md)

Miembros declarados: `arrayInclusion`, `arrayMaximum`, `arrayShape`, `calculateSatisfiable`, `compare` (2 declaraciones), `compute`, `contains`, `effectiveTypes`, `empty`, `enumCovers`, `enumWitness`, `equals`, `excluded`, `finiteValues`, `hashCode`, `included`, `interval`, `objectInclusion`, `objectSatisfiable`, `Pair`, `predicate`, `Result`, `satisfiable`, `shape`, `singleton`, `uncertainty`, `unknown`, `unrestricted`.

Record `Result`: `Outcome outcome, String reason, String sourcePointer, String targetPointer, boolean incomplete`.

Record `Interval`: `SchemaValueContract.Bound lower, SchemaValueContract.Bound upper`.

### SchemaUse.java

[src/main/java/com/openapiguard/domain/SchemaUse.java](../../src/main/java/com/openapiguard/domain/SchemaUse.java) · [Explicación](domain.md)

Define el tipo, sus componentes/valores o el contrato del paquete; sin métodos explícitos adicionales.

Record `SchemaUse`: `SchemaValueContract schema, String usagePointer, String representationIssue`.

### SchemaUseId.java

[src/main/java/com/openapiguard/domain/SchemaUseId.java](../../src/main/java/com/openapiguard/domain/SchemaUseId.java) · [Explicación](domain.md)

Define el tipo, sus componentes/valores o el contrato del paquete; sin métodos explícitos adicionales.

Record `SchemaUseId`: `OperationId operation, Direction direction, ParameterId parameter, String responseCode, String mediaType`.

### SchemaValueContract.java

[src/main/java/com/openapiguard/domain/SchemaValueContract.java](../../src/main/java/com/openapiguard/domain/SchemaValueContract.java) · [Explicación](domain.md)

Miembros declarados: `accepts`, `acceptsConstraints`, `SchemaValueContract` (2 declaraciones), `SchemaValueContract (constructor compacto)`.

Record `SchemaValueContract`: `Set<String> types, List<ScalarValue> enumValues, Bound minimum, Bound maximum, int minLength, int maxLength, List<String> unsupported, String definitionPointer, ObjectShape objectShape, boolean readOnly, boolean writeOnly, ArrayShape arrayShape`.

Record `Bound`: `BigDecimal value, boolean exclusive`.

### SchemaValuesRule.java

[src/main/java/com/openapiguard/domain/SchemaValuesRule.java](../../src/main/java/com/openapiguard/domain/SchemaValuesRule.java) · [Explicación](domain.md)

Miembros declarados: `compare` (2 declaraciones).

## Configuración, scripts y archivos raíz

| Archivo | Explicación |
| --- | --- |
| [pom.xml](../../pom.xml) | [Build y automatización](build.md) |
| [.gitignore](../../.gitignore) | [Build y automatización](build.md) |
| [AGENTS.md](../../AGENTS.md) | [Build y automatización](build.md) |
| [README.md](../../README.md) | [Build y automatización](build.md) |
| [src/main/resources/application.properties](../../src/main/resources/application.properties) | [Build y automatización](build.md) |
| [src/main/resources/logback-spring.xml](../../src/main/resources/logback-spring.xml) | [Build y automatización](build.md) |
| [scripts/compare-contracts.ps1](../../scripts/compare-contracts.ps1) | [Build y automatización](build.md) |
| [scripts/test-ci.ps1](../../scripts/test-ci.ps1) | [Build y automatización](build.md) |

## Tests: métodos y generadores de escenarios

El [capítulo de pruebas](tests.md) explica el propósito y el número de ejecuciones de cada clase. Se enumeran los métodos de test, generadores parametrizados y helpers de construcción/verificación para localizar cualquier fallo. Sus nombres describen el comportamiento o dato generado; las sobrecargas se agrupan.

### ComparisonCommandTest.java

[src/test/java/com/openapiguard/adapter/cli/ComparisonCommandTest.java](../../src/test/java/com/openapiguard/adapter/cli/ComparisonCommandTest.java)

`execute`, `missingFilesAndWrongArgumentsAreInputErrors`, `returnsStructuredJsonAndExitCodesForRealFilesWithSpaces`, `template`

### ComparisonProcessTest.java

[src/test/java/com/openapiguard/adapter/cli/ComparisonProcessTest.java](../../src/test/java/com/openapiguard/adapter/cli/ComparisonProcessTest.java)

`bootEntryPointClosesAndExitsWithBreakingCodeAndCleanJson`

### HumanOutputTest.java

[src/test/java/com/openapiguard/adapter/cli/HumanOutputTest.java](../../src/test/java/com/openapiguard/adapter/cli/HumanOutputTest.java)

`command`, `explainsBreakingChangesAndCoverage`, `helpSucceedsWithoutReadingFiles`, `invalidInputHasHumanDiagnosticsAndFailureCode`, `rejectsUnknownOrDuplicateOptionsWithoutProducingAnAnalysis`, `schemaValuesProfileWorksFromCliWithResponseDirection`, `selectsArrayProfileAndExplainsItemChanges`, `selectsFullProfileAndExplainsRemovedResponseCodes`, `selectsRequestBodyProfileExplicitly`, `selectsTheCompleteSchemaProfileForHumansAndAutomation`

### OpenApiGuardApplicationTest.java

[src/test/java/com/openapiguard/adapter/cli/OpenApiGuardApplicationTest.java](../../src/test/java/com/openapiguard/adapter/cli/OpenApiGuardApplicationTest.java)

`startsWithoutWebServerAndClosesContext`

### PackagedCliIT.java

[src/test/java/com/openapiguard/adapter/cli/PackagedCliIT.java](../../src/test/java/com/openapiguard/adapter/cli/PackagedCliIT.java)

`detectsRemovalsWithStableIdentityAndOriginalPointers`, `equivalentYamlAndJsonAndAdditionsAreCompatibleOnlyWithinTheProfile`, `invalidParameterReferenceFailsWithoutInventingRemovedOperations`, `malformedContractCannotProduceSpeculativeRemovals`, `missingFilesAndInvalidUtf8ProduceReadDiagnostics`, `oversizedFileIsRejectedAtTheCliBoundary`, `packagedJarAnalyzesRequestAndResponseScalarValues`, `packagedJarAppliesResponsePolicyAndPreservesReview`, `packagedJarComparesArraysByDirection`, `packagedJarComparesObjectsAndPreservesDirectionAndCoverage`, `packagedJarComparesRequiredBodiesAndRemovedMediaTypes`, `packagedJarDetectsInheritedAndReferencedParameterBreaks`, `packagedJarHandlesOptionalAdditionsAndRelaxationWithoutFalseBreaks`, `packagedJarShowsRemovalAndUnsupportedPresenceAsReview`, `packagedJarSupportsHumanOutputAndHelp`, `renamedTemplateIsPartialAndAProvenRemovalStillBlocks`, `report`, `run`, `template`, `write`, `wrongNumberOfArgumentsUsesStderrAndReturnsUsageError`

### ArraySchemaComparisonTest.java

[src/test/java/com/openapiguard/adapter/openapi/ArraySchemaComparisonTest.java](../../src/test/java/com/openapiguard/adapter/openapi/ArraySchemaComparisonTest.java)

`analyzer`, `array`, `compare`, `contract`, `dialect31AllowsOmittedItemsAndBooleanItems`, `emptyOnlyArraysDoNotCompareUnreachableItems`, `internalReferencesAreEquivalentAndCyclesRequireReview`, `itemTypesDependOnDirection`, `lengthBoundsDependOnDirection`, `nestedArraysAndObjectsPreserveLocation`, `opaqueItemsCannotProveThatALongerArrayExists`, `opaqueRequiredItemsCannotProveAnArrayTypeCounterexample`, `priorProfileRetainsPartialArrayCoverage`, `unsupportedAssertionsAndContradictionsCannotGiveGreen`

### ContractBoundaryIntegrationTest.java

[src/test/java/com/openapiguard/adapter/openapi/ContractBoundaryIntegrationTest.java](../../src/test/java/com/openapiguard/adapter/openapi/ContractBoundaryIntegrationTest.java)

`acceptsTheDocumentByteLimitAndRejectsOneByteMore`, `assertFailed`, `emptyPathsAndMetadataOnlyPathsHaveNoOperationsToRemove`, `everyHttpMethodSurvivesParsingAndIsDetectedWhenRemoved`, `excessiveNestingIsRejectedBeforeComparison`, `expandedProfileRejectsActiveExternalReferencesWithoutNetworkAccess`, `externalHttpReferenceIsRejectedWithoutOpeningAConnection`, `recursiveInternalSchemasRemainExcludedWithoutExpandingThem`, `sizeLimitCountsUtf8BytesRatherThanCharacters`

### ContractCorpusTest.java

[src/test/java/com/openapiguard/adapter/openapi/ContractCorpusTest.java](../../src/test/java/com/openapiguard/adapter/openapi/ContractCorpusTest.java)

`cases`, `comparesDocumentedContractPairs`, `toString`

### EffectiveParametersTest.java

[src/test/java/com/openapiguard/adapter/openapi/EffectiveParametersTest.java](../../src/test/java/com/openapiguard/adapter/openapi/EffectiveParametersTest.java)

`cyclesAndWrongReferenceTargetsAreRejected`, `document`, `everyTemplateVariableMustHaveAnEffectiveRequiredPathParameter`, `ignoresSpecialHeaders`, `inheritsOverridesAndPreservesSourceWithoutRemovingInheritedParameters`, `internalParameterReferencesPreserveDefinitionAndUsage`, `invalidParametersCannotBecomeEmptyEffectiveMaps`, `orderDoesNotChangeEffectiveIdentity`, `usesLocationAndHeaderCasingWithoutFoldingQueryNames`, `withParameters`

### ObjectSchemaComparisonTest.java

[src/test/java/com/openapiguard/adapter/openapi/ObjectSchemaComparisonTest.java](../../src/test/java/com/openapiguard/adapter/openapi/ObjectSchemaComparisonTest.java)

`accessModeTransitionsRequireReviewRatherThanAssumingServerRejection`, `additionalPropertiesDependOnDirection`, `compare`, `contract`, `defaultsAreOpenAndPropertiesDoNotImplyObjectType`, `internalObjectReferencesAndCyclesPreserveCoverage`, `mathematicallyIntegralLengthBoundsAcceptDecimalNotation`, `nestedPropertyChangesPreserveOriginalPointers`, `normalizationBudgetIsVisibleInsteadOfSilentlyCertifyingIncompleteData`, `object`, `opaquePropertiesAndSchemaAdditionalPropertiesArePartial`, `optionalPropertyAdditionCanConstrainPreviouslyAdditionalValues`, `optionalPropertyRemovalDependsOnOpenOrClosedObjects`, `provenBreakAndOpaqueOptionalPropertyRetainPartialCoverage`, `readOnlyAndWriteOnlyProjectRequiredByDirection`, `requiredPropertiesDependOnDirection`, `requiredPropertyDeletionAndInvalidAccessModesAreExplicit`, `sharedAcyclicReferenceGraphStaysBoundedAndDetectsLeafChanges`, `sharedComponentIsComparedSeparatelyForRequestsAndResponses`, `unsupportedArraysAndCompositionRemainPartial`

### ParameterComparisonTest.java

[src/test/java/com/openapiguard/adapter/openapi/ParameterComparisonTest.java](../../src/test/java/com/openapiguard/adapter/openapi/ParameterComparisonTest.java)

`comparesEffectiveRequiredParametersAcrossSupportedFamilies`, `complexPresenceIsPartialAndResponseRequiredIsExcluded`, `document`, `effectiveOverrideWinsOverARequiredInheritedDeclaration`, `headerCaseAndExplicitSerializationDefaultsAreEquivalent`, `legacyProfileRetainsItsScopeAndRemovalIds`, `refAndInlineAreEquivalentAndPointersPointToDefinitionsAndUsages`

### RequestBodyComparisonTest.java

[src/test/java/com/openapiguard/adapter/openapi/RequestBodyComparisonTest.java](../../src/test/java/com/openapiguard/adapter/openapi/RequestBodyComparisonTest.java)

`additionAndRemovalUseDifferentPolicies`, `ambiguousMethodBodySemanticsRequireReview`, `analyzer`, `body`, `comparesMediaTypeCoverage`, `comparesRequiredBody`, `contract`, `cyclicBodyReferenceFailsWithoutSpeculativeFindings`, `internalReferencesRetainDefinitionAndUsageWithoutExpansion`, `onlySharedUnambiguousOperationsAreCompared`, `unsupportedMediaParametersCannotGiveGreen`

### ResponsePolicyComparisonTest.java

[src/test/java/com/openapiguard/adapter/openapi/ResponsePolicyComparisonTest.java](../../src/test/java/com/openapiguard/adapter/openapi/ResponsePolicyComparisonTest.java)

`additionsRequireReviewOfConsumerAndNegotiationPolicy`, `analyzer`, `compare`, `contract`, `equivalentDescriptionsAndMediaCaseAreCompatibleAndIdsIgnoreDefinitionNames`, `fallbackCanCoverRemovedExplicitCodeButCannotCertifyCompatibility`, `invalidAndCyclicResponseReferencesFailWithoutSpeculativeChanges`, `payloadComparisonStillUsesResponseDirectionAndArrays`, `priorArrayProfileStillExcludesResponseCodePolicy`, `removedConcreteCodeIsAPublishedPolicyBreakEvenWithoutBody`, `removedConcreteRepresentationHasOriginalLocation`, `removedOperationDoesNotDuplicateResponsePolicyFindings`, `removedResponseSchemaDoesNotInventRequestCoverageDiagnostics`, `removingPayloadFormatsWithoutReplacementIsDetected`, `response`, `responseReferencesRetainDefinitionAndUse`, `unchangedFallbackWithoutBodyIsStillExplicitlyPartial`, `unrelatedRangeDoesNotHideProvenCodeRemoval`, `wildcardCannotProveAConcreteRepresentationWasRemoved`

### SchemaValuesComparisonTest.java

[src/test/java/com/openapiguard/adapter/openapi/SchemaValuesComparisonTest.java](../../src/test/java/com/openapiguard/adapter/openapi/SchemaValuesComparisonTest.java)

`analyzer`, `compare`, `contract`, `enumExpansionHasOppositeEffects`, `enumTypeAndBoundsAreEvaluatedTogether`, `exclusiveBoundsAreDialectSpecificAndJoint`, `missingSchemaPointersReferToExistingMediaObject`, `nullableChangesAreDirectional`, `nullableDoesNotOverrideEnumAndMissingTypeIsNotInvented`, `numericAndStringLimitsRespectDirection`, `opaqueAndContradictorySchemasCannotGiveGreen`, `parameterValuesAreComparedByEffectiveIdentity`, `sharedComponentsAreAnalyzedByUseAndCyclesArePartial`, `supports31UnionNullWithoutConvertingDialects`, `typeInclusionDependsOnDirection`, `unknownSchemaDialectCannotGiveGreen`, `explanationsShowBaselineToCandidateConstraintsInBothDirectionsAndNestedProperties`, `arrayExplanationKeepsChronologicalBoundsWhenResponseInclusionIsReversed`, `abbreviatedEnumDetailsPreserveWholeUnicodeCharacters`.

### SwaggerContractAnalyzerTest.java

[src/test/java/com/openapiguard/adapter/openapi/SwaggerContractAnalyzerTest.java](../../src/test/java/com/openapiguard/adapter/openapi/SwaggerContractAnalyzerTest.java)

`comparesSmallFilesThroughTheUseCaseWithoutSpring`, `internalResponseReferencesDoNotChangeOperationIdentity`, `invalidCandidateDoesNotProduceFalseRemovals`, `operationRemovalWorksInSupportedFamilies`, `parserValidationAndDanglingReferencesFailClosed`, `rejectsCrossFamilyComparison`, `rejectsDuplicateKeysAndOversizedDocuments`, `rejectsExternalRefsWithoutTouchingTheReferencedFile`, `rejectsLocalPathItemRefsInsteadOfSilentlyTreatingTheirMethodsAsAbsent`, `rejectsMultipleDocuments`, `rejectsYamlAliasesWithoutReportingRemovals`, `reorderedJsonAndYamlAreEquivalentAndAdditionsDoNotBreak`

### ArrayInclusionTest.java

[src/test/java/com/openapiguard/domain/ArrayInclusionTest.java](../../src/test/java/com/openapiguard/domain/ArrayInclusionTest.java)

`accepts`, `inclusionMatchesIndependentArrayOracle`, `pairs`, `samples`, `schema`, `Shape`

### ObjectInclusionTest.java

[src/test/java/com/openapiguard/domain/ObjectInclusionTest.java](../../src/test/java/com/openapiguard/domain/ObjectInclusionTest.java)

`accepts`, `inclusionMatchesIndependentObjectOracle`, `objects`, `pairs`, `schema`, `Shape`

### RemovedOperationsRuleTest.java

[src/test/java/com/openapiguard/domain/RemovedOperationsRuleTest.java](../../src/test/java/com/openapiguard/domain/RemovedOperationsRuleTest.java)

`additionsAndEquivalentContractsHaveNoRemovals`, `contract`, `detectsEveryRemovedHttpMethod`, `findingsAreSortedAndMetadataOnlyPathsAreNotEndpoints`, `reportsOneEndpointRemovalWithoutDuplicatingMethods`, `templateRenameIsIndeterminateInsteadOfAnEndpointRemoval`

### RequiredParametersRuleTest.java

[src/test/java/com/openapiguard/domain/RequiredParametersRuleTest.java](../../src/test/java/com/openapiguard/domain/RequiredParametersRuleTest.java)

`contract`, `idsIgnorePhysicalIndicesAndFindingsUseSemanticOrder`, `newlyRequiredParametersBreakRequests`, `optionalAdditionsAndUnchangedContractsRemainCompatible`, `optionalToRequiredBreaksAndTheReverseDoesNot`, `parameter`, `removalRequiresReviewRatherThanInventingARequestRejection`, `removedAndNewOperationsDoNotGenerateParameterDuplicates`, `serializationChangeIsPartialAndDoesNotHideIndependentBreaks`, `unsupportedBaselineKeepsItsOriginalSourceAndLocation`, `unsupportedPresenceNeverProducesAGreenOrASpeculativeBreak`

### ResponsePolicyRuleTest.java

[src/test/java/com/openapiguard/domain/ResponsePolicyRuleTest.java](../../src/test/java/com/openapiguard/domain/ResponsePolicyRuleTest.java)

`contract`, `domainPolicyRunsWithoutParserAndRetainsStableIdentity`, `responseSnapshotsAreDeeplyImmutable`, `unrelatedFallbackPreservesProvenBreakAndPartialCoverage`

### SchemaInclusionTest.java

[src/test/java/com/openapiguard/domain/SchemaInclusionTest.java](../../src/test/java/com/openapiguard/domain/SchemaInclusionTest.java)

`boundedIntegerInclusionMatchesExhaustiveMathematicalOracle`, `incompatibleUnionReportsFirstTypeInCanonicalOrder`, `integer`, `integerIntervals`, `largeEnumBoundsCanExcludeValuesDespiteMatchingEnumEntries`, `largeEnumsProveIntegerIntervalCoverageAndApplyTheirBounds`, `lengthCountsUnicodeCodePointsRatherThanUtf16Units`, `unknownAssertionsRemainUnknownEvenWhenIdentical`

### TestSpecifications.java

[src/test/java/com/openapiguard/TestSpecifications.java](../../src/test/java/com/openapiguard/TestSpecifications.java)

`fixture`, `TestSpecifications`

## Contratos pequeños y manifest

| Recurso | Papel |
| --- | --- |
| [src/test/resources/openapi/arrays-new.yaml](../../src/test/resources/openapi/arrays-new.yaml) | Candidata para dirección de items/longitudes. |
| [src/test/resources/openapi/arrays-old.yaml](../../src/test/resources/openapi/arrays-old.yaml) | Base con arrays homogéneos. |
| [src/test/resources/openapi/baseline.yaml](../../src/test/resources/openapi/baseline.yaml) | Base de rutas/métodos. |
| [src/test/resources/openapi/body-optional.yaml](../../src/test/resources/openapi/body-optional.yaml) | Cuerpo de petición opcional. |
| [src/test/resources/openapi/body-required.yaml](../../src/test/resources/openapi/body-required.yaml) | Cuerpo obligatorio y cobertura de formatos distinta. |
| [src/test/resources/openapi/corpus/cases.json](../../src/test/resources/openapi/corpus/cases.json) | 146 comparaciones con expectativas explícitas; consumido por ContractCorpusTest. |
| [src/test/resources/openapi/equivalent.json](../../src/test/resources/openapi/equivalent.json) | Equivalente a baseline en distinto formato/orden. |
| [src/test/resources/openapi/objects-new.yaml](../../src/test/resources/openapi/objects-new.yaml) | Cambios required de petición/respuesta. |
| [src/test/resources/openapi/objects-old.yaml](../../src/test/resources/openapi/objects-old.yaml) | Base de objetos y garantías de propiedades. |
| [src/test/resources/openapi/parameters-baseline.yaml](../../src/test/resources/openapi/parameters-baseline.yaml) | Parámetros heredados y opcionales. |
| [src/test/resources/openapi/parameters-required.yaml](../../src/test/resources/openapi/parameters-required.yaml) | Obligatoriedad heredada y parámetro referido nuevo. |
| [src/test/resources/openapi/removed.yaml](../../src/test/resources/openapi/removed.yaml) | Retira endpoint y método de baseline. |
| [src/test/resources/openapi/responses-new.yaml](../../src/test/resources/openapi/responses-new.yaml) | Candidata con retiradas de respuesta. |
| [src/test/resources/openapi/responses-old.yaml](../../src/test/resources/openapi/responses-old.yaml) | Base de política de respuestas. |
| [src/test/resources/openapi/schema-values-new.yaml](../../src/test/resources/openapi/schema-values-new.yaml) | Candidata con restricciones escalares por dirección. |
| [src/test/resources/openapi/schema-values-old.yaml](../../src/test/resources/openapi/schema-values-old.yaml) | Base de restricciones escalares. |

## Todos los contratos del corpus

Cada fila enlaza un fixture y ejemplos de IDs de comparaciones que lo usan. El nombre distingue la variación: ubicación/tipo/obligatoriedad, herencia, representación, referencia o entrada inválida. Un fixture no tiene un veredicto por sí solo: el resultado depende del par y del perfil. Consultar cases.json para todas sus expectativas.

| Fixture | Pares que lo usan | Primeros IDs de casos |
| --- | ---: | --- |
| [src/test/resources/openapi/corpus/array-required.json](../../src/test/resources/openapi/corpus/array-required.json) | 2 | `array-presence-unsupported`, `array-identity-still-partial` |
| [src/test/resources/openapi/corpus/content-required.json](../../src/test/resources/openapi/corpus/content-required.json) | 1 | `content-presence-unsupported` |
| [src/test/resources/openapi/corpus/cookie-boolean-optional.json](../../src/test/resources/openapi/corpus/cookie-boolean-optional.json) | 5 | `cookie-boolean-absent-to-optional`, `cookie-boolean-optional-to-absent`, `cookie-boolean-optional-to-optional` |
| [src/test/resources/openapi/corpus/cookie-boolean-required.json](../../src/test/resources/openapi/corpus/cookie-boolean-required.json) | 5 | `cookie-boolean-absent-to-required`, `cookie-boolean-optional-to-required`, `cookie-boolean-required-to-absent` |
| [src/test/resources/openapi/corpus/cookie-number-optional.json](../../src/test/resources/openapi/corpus/cookie-number-optional.json) | 5 | `cookie-number-absent-to-optional`, `cookie-number-optional-to-absent`, `cookie-number-optional-to-optional` |
| [src/test/resources/openapi/corpus/cookie-number-required.json](../../src/test/resources/openapi/corpus/cookie-number-required.json) | 5 | `cookie-number-absent-to-required`, `cookie-number-optional-to-required`, `cookie-number-required-to-absent` |
| [src/test/resources/openapi/corpus/cookie-optional.json](../../src/test/resources/openapi/corpus/cookie-optional.json) | 5 | `cookie-absent-to-optional`, `cookie-optional-to-absent`, `cookie-optional-to-optional` |
| [src/test/resources/openapi/corpus/cookie-required.json](../../src/test/resources/openapi/corpus/cookie-required.json) | 5 | `cookie-absent-to-required`, `cookie-optional-to-required`, `cookie-required-to-absent` |
| [src/test/resources/openapi/corpus/cookie-string-optional.json](../../src/test/resources/openapi/corpus/cookie-string-optional.json) | 5 | `cookie-string-absent-to-optional`, `cookie-string-optional-to-absent`, `cookie-string-optional-to-optional` |
| [src/test/resources/openapi/corpus/cookie-string-required.json](../../src/test/resources/openapi/corpus/cookie-string-required.json) | 5 | `cookie-string-absent-to-required`, `cookie-string-optional-to-required`, `cookie-string-required-to-absent` |
| [src/test/resources/openapi/corpus/empty-31.json](../../src/test/resources/openapi/corpus/empty-31.json) | 2 | `cross-family-rejected`, `31-new-required` |
| [src/test/resources/openapi/corpus/empty-local-list.json](../../src/test/resources/openapi/corpus/empty-local-list.json) | 1 | `empty-local-list-keeps-inheritance` |
| [src/test/resources/openapi/corpus/empty.json](../../src/test/resources/openapi/corpus/empty.json) | 80 | `query-absent-to-absent`, `query-absent-to-optional`, `query-absent-to-required` |
| [src/test/resources/openapi/corpus/endpoint-removed.json](../../src/test/resources/openapi/corpus/endpoint-removed.json) | 3 | `endpoint-removal`, `operation-addition`, `parameter-rules-suppressed-with-removed-operation` |
| [src/test/resources/openapi/corpus/explicit-defaults.json](../../src/test/resources/openapi/corpus/explicit-defaults.json) | 1 | `explicit-defaults` |
| [src/test/resources/openapi/corpus/header-boolean-optional.json](../../src/test/resources/openapi/corpus/header-boolean-optional.json) | 5 | `header-boolean-absent-to-optional`, `header-boolean-optional-to-absent`, `header-boolean-optional-to-optional` |
| [src/test/resources/openapi/corpus/header-boolean-required.json](../../src/test/resources/openapi/corpus/header-boolean-required.json) | 5 | `header-boolean-absent-to-required`, `header-boolean-optional-to-required`, `header-boolean-required-to-absent` |
| [src/test/resources/openapi/corpus/header-number-optional.json](../../src/test/resources/openapi/corpus/header-number-optional.json) | 5 | `header-number-absent-to-optional`, `header-number-optional-to-absent`, `header-number-optional-to-optional` |
| [src/test/resources/openapi/corpus/header-number-required.json](../../src/test/resources/openapi/corpus/header-number-required.json) | 5 | `header-number-absent-to-required`, `header-number-optional-to-required`, `header-number-required-to-absent` |
| [src/test/resources/openapi/corpus/header-optional.json](../../src/test/resources/openapi/corpus/header-optional.json) | 5 | `header-absent-to-optional`, `header-optional-to-absent`, `header-optional-to-optional` |
| [src/test/resources/openapi/corpus/header-required.json](../../src/test/resources/openapi/corpus/header-required.json) | 6 | `header-absent-to-required`, `header-optional-to-required`, `header-required-to-absent` |
| [src/test/resources/openapi/corpus/header-string-optional.json](../../src/test/resources/openapi/corpus/header-string-optional.json) | 5 | `header-string-absent-to-optional`, `header-string-optional-to-absent`, `header-string-optional-to-optional` |
| [src/test/resources/openapi/corpus/header-string-required.json](../../src/test/resources/openapi/corpus/header-string-required.json) | 5 | `header-string-absent-to-required`, `header-string-optional-to-required`, `header-string-required-to-absent` |
| [src/test/resources/openapi/corpus/header-uppercase.json](../../src/test/resources/openapi/corpus/header-uppercase.json) | 1 | `header-casing-is-equivalent` |
| [src/test/resources/openapi/corpus/inherited-required.json](../../src/test/resources/openapi/corpus/inherited-required.json) | 2 | `inherited-required`, `empty-local-list-keeps-inheritance` |
| [src/test/resources/openapi/corpus/internal-ref-required.json](../../src/test/resources/openapi/corpus/internal-ref-required.json) | 1 | `referenced-required` |
| [src/test/resources/openapi/corpus/internal-ref.json](../../src/test/resources/openapi/corpus/internal-ref.json) | 1 | `inline-ref-equivalent` |
| [src/test/resources/openapi/corpus/invalid-both-schema-content.json](../../src/test/resources/openapi/corpus/invalid-both-schema-content.json) | 1 | `invalid-both-schema-content` |
| [src/test/resources/openapi/corpus/invalid-cyclic.json](../../src/test/resources/openapi/corpus/invalid-cyclic.json) | 1 | `invalid-cyclic` |
| [src/test/resources/openapi/corpus/invalid-duplicate.json](../../src/test/resources/openapi/corpus/invalid-duplicate.json) | 1 | `invalid-duplicate` |
| [src/test/resources/openapi/corpus/invalid-external-reference.json](../../src/test/resources/openapi/corpus/invalid-external-reference.json) | 1 | `invalid-external-reference` |
| [src/test/resources/openapi/corpus/invalid-header-duplicate.json](../../src/test/resources/openapi/corpus/invalid-header-duplicate.json) | 1 | `invalid-header-duplicate` |
| [src/test/resources/openapi/corpus/invalid-missing-reference.json](../../src/test/resources/openapi/corpus/invalid-missing-reference.json) | 1 | `invalid-missing-reference` |
| [src/test/resources/openapi/corpus/invalid-null-parameters.json](../../src/test/resources/openapi/corpus/invalid-null-parameters.json) | 1 | `invalid-null-parameters` |
| [src/test/resources/openapi/corpus/invalid-path-missing.json](../../src/test/resources/openapi/corpus/invalid-path-missing.json) | 1 | `invalid-path-missing` |
| [src/test/resources/openapi/corpus/invalid-path-optional.json](../../src/test/resources/openapi/corpus/invalid-path-optional.json) | 1 | `invalid-path-optional` |
| [src/test/resources/openapi/corpus/invalid-required.json](../../src/test/resources/openapi/corpus/invalid-required.json) | 1 | `invalid-invalid-required` |
| [src/test/resources/openapi/corpus/invalid-unknown-location.json](../../src/test/resources/openapi/corpus/invalid-unknown-location.json) | 1 | `invalid-unknown-location` |
| [src/test/resources/openapi/corpus/local-override.json](../../src/test/resources/openapi/corpus/local-override.json) | 1 | `local-overrides-inherited` |
| [src/test/resources/openapi/corpus/method-removed.json](../../src/test/resources/openapi/corpus/method-removed.json) | 1 | `method-removal` |
| [src/test/resources/openapi/corpus/object-required.json](../../src/test/resources/openapi/corpus/object-required.json) | 2 | `object-presence-unsupported`, `object-identity-still-partial` |
| [src/test/resources/openapi/corpus/order-a.json](../../src/test/resources/openapi/corpus/order-a.json) | 1 | `parameter-order-equivalent` |
| [src/test/resources/openapi/corpus/order-b.json](../../src/test/resources/openapi/corpus/order-b.json) | 1 | `parameter-order-equivalent` |
| [src/test/resources/openapi/corpus/path-renamed.json](../../src/test/resources/openapi/corpus/path-renamed.json) | 1 | `path-template-renamed` |
| [src/test/resources/openapi/corpus/path-valid.json](../../src/test/resources/openapi/corpus/path-valid.json) | 4 | `path-required-identity`, `invalid-path-optional`, `invalid-path-missing` |
| [src/test/resources/openapi/corpus/query-boolean-optional.json](../../src/test/resources/openapi/corpus/query-boolean-optional.json) | 5 | `query-boolean-absent-to-optional`, `query-boolean-optional-to-absent`, `query-boolean-optional-to-optional` |
| [src/test/resources/openapi/corpus/query-boolean-required.json](../../src/test/resources/openapi/corpus/query-boolean-required.json) | 5 | `query-boolean-absent-to-required`, `query-boolean-optional-to-required`, `query-boolean-required-to-absent` |
| [src/test/resources/openapi/corpus/query-number-optional.json](../../src/test/resources/openapi/corpus/query-number-optional.json) | 5 | `query-number-absent-to-optional`, `query-number-optional-to-absent`, `query-number-optional-to-optional` |
| [src/test/resources/openapi/corpus/query-number-required.json](../../src/test/resources/openapi/corpus/query-number-required.json) | 5 | `query-number-absent-to-required`, `query-number-optional-to-required`, `query-number-required-to-absent` |
| [src/test/resources/openapi/corpus/query-optional.json](../../src/test/resources/openapi/corpus/query-optional.json) | 12 | `query-absent-to-optional`, `query-optional-to-absent`, `query-optional-to-optional` |
| [src/test/resources/openapi/corpus/query-required.json](../../src/test/resources/openapi/corpus/query-required.json) | 6 | `query-absent-to-required`, `query-optional-to-required`, `query-required-to-absent` |
| [src/test/resources/openapi/corpus/query-string-optional.json](../../src/test/resources/openapi/corpus/query-string-optional.json) | 5 | `query-string-absent-to-optional`, `query-string-optional-to-absent`, `query-string-optional-to-optional` |
| [src/test/resources/openapi/corpus/query-string-required.json](../../src/test/resources/openapi/corpus/query-string-required.json) | 5 | `query-string-absent-to-required`, `query-string-optional-to-required`, `query-string-required-to-absent` |
| [src/test/resources/openapi/corpus/query-uppercase.json](../../src/test/resources/openapi/corpus/query-uppercase.json) | 1 | `query-casing-is-a-rename` |
| [src/test/resources/openapi/corpus/required-31.json](../../src/test/resources/openapi/corpus/required-31.json) | 1 | `31-new-required` |
| [src/test/resources/openapi/corpus/response-required.json](../../src/test/resources/openapi/corpus/response-required.json) | 1 | `response-required-is-excluded` |
| [src/test/resources/openapi/corpus/serialization-changed.json](../../src/test/resources/openapi/corpus/serialization-changed.json) | 1 | `serialization-changed` |
| [src/test/resources/openapi/corpus/special-accept.json](../../src/test/resources/openapi/corpus/special-accept.json) | 1 | `special-header-accept` |
| [src/test/resources/openapi/corpus/special-authorization.json](../../src/test/resources/openapi/corpus/special-authorization.json) | 1 | `special-header-authorization` |
| [src/test/resources/openapi/corpus/special-content-type.json](../../src/test/resources/openapi/corpus/special-content-type.json) | 1 | `special-header-content-type` |

## Ejemplos de producto

| Archivo | Papel |
| --- | --- |
| [examples/catalog/README.md](../../examples/catalog/README.md) | Expectativas y comandos del catálogo sintético. |
| [examples/catalog/base.yaml](../../examples/catalog/base.yaml) | Contrato anterior común de productos/pedidos. |
| [examples/catalog/compatible.yaml](../../examples/catalog/compatible.yaml) | Ampliación de peticiones y restricción de respuestas; código 0 en full. |
| [examples/catalog/breaking.yaml](../../examples/catalog/breaking.yaml) | Restricción de peticiones y expansión de respuestas; cuatro hallazgos, código 1. |
| [examples/catalog/review.yaml](../../examples/catalog/review.yaml) | Añade pattern no soportado; revisión y código 3. |

## Lanzador y validación documental

[guard.ps1](../../guard.ps1) facilita demo y comparación propia con full/texto, construyendo offline si falta el JAR. [launcher.test.mjs](../../scripts/launcher.test.mjs) comprueba demo, informes y códigos del JAR real. [javadoc.ps1](../../scripts/javadoc.ps1) reutiliza el classpath aprobado y exige doclint sin avisos. [UsabilityTest](../../src/test/java/com/openapiguard/adapter/cli/UsabilityTest.java) verifica nombres, formatos, guardado, hardlinks y escape de datos. Ver [objetivos y mantenimiento](../product-usability.md).

## Integración Git/GitHub

La [guía de integración](../git-integration.md) documenta funciones, políticas, formato y límites de cada pieza nueva.

| Archivo | Responsabilidad |
| --- | --- |
| [git-contracts.mjs](../../scripts/git-contracts.mjs) | Selección por commits, instantáneas, CLI Java e informes agregados. |
| [git-contracts.test.mjs](../../scripts/git-contracts.test.mjs) | Repositorios reales y comparación con el JAR empaquetado. |
| [github-workflow.test.mjs](../../scripts/github-workflow.test.mjs) | Política del workflow y verificación offline de checksum. |
| [setup-maven.ps1](../../scripts/setup-maven.ps1) | Bootstrap de Maven 3.10.0 en runner con SHA512. |
| [openapi-contract-guard.yml](../../.github/workflows/openapi-contract-guard.yml) | Check estable de PR y cola de merge. |

## Portal visual Docusaurus

Estas piezas son independientes del build Java. El [mantenimiento del portal](../portal/documentation-site.md) explica cómo se generan guías, código, escenarios e índices sin duplicar las fuentes.

| Archivo | Responsabilidad |
| --- | --- |
| [website/README.md](../../website/README.md) | Arranque local y recorrido de comandos. |
| [website/package.json](../../website/package.json) | Dependencias documentales y scripts aislados. |
| [website/docusaurus.config.js](../../website/docusaurus.config.js) | Navegación, idioma, Mermaid y build estricto. |
| [website/sidebars.js](../../website/sidebars.js) | Orden de capítulos y referencia. |
| [website/scripts/content.mjs](../../website/scripts/content.mjs) | Paths portables, títulos, clasificación y enlaces locales. |
| [website/scripts/prepare-content.mjs](../../website/scripts/prepare-content.mjs) | Preparación de páginas e índices desde el repositorio. |
| [website/scripts/scenarios.mjs](../../website/scripts/scenarios.mjs) | Contratos y expectativas de las 72 comparaciones ampliadas. |
| [website/scripts/content.test.mjs](../../website/scripts/content.test.mjs) | Regresiones del generador y estructura de ejemplos. |
| [website/scripts/site-build.test.mjs](../../website/scripts/site-build.test.mjs) | Regresiones de las opciones de filtros en HTML de producción. |
| [website/pnpm-lock.yaml](../../website/pnpm-lock.yaml) | Resolución fijada de dependencias documentales aprobadas. |
| [website/scripts/verify-cases.ps1](../../website/scripts/verify-cases.ps1) | Verificación de las expectativas con el motor Java existente. |
| [website/src/components/Explorer.jsx](../../website/src/components/Explorer.jsx) | Hook de selección por URL, veredicto e introducción comunes. |
| [website/src/pages/index.jsx](../../website/src/pages/index.jsx) | Portada y recorrido de revisión. |
| [website/src/pages/code.jsx](../../website/src/pages/code.jsx) | Explorador de archivos, búsqueda por contenido y líneas. |
| [website/src/pages/cases.jsx](../../website/src/pages/cases.jsx) | Filtros y comparación visual de contratos. |
| [website/src/pages/search.jsx](../../website/src/pages/search.jsx) | Búsqueda local unificada. |
| [website/src/css/custom.css](../../website/src/css/custom.css) | Presentación adaptable, foco y tema claro/oscuro. |

Los capítulos nuevos de [producto](../portal/project.md), [roadmap actual](../portal/roadmap.md), [primer recorrido](../portal/getting-started.md), [casos de uso](../portal/use-cases.md), [mejoras](../portal/improvements.md) y [mantenimiento del sitio](../portal/documentation-site.md) se conservan en docs/portal como Markdown fuente. Las carpetas generadas y dependencias quedan excluidas del inventario versionado.

## Referencias históricas

- [docs/architecture.md](../../docs/architecture.md)
- [docs/array-schemas-profile.md](../../docs/array-schemas-profile.md)
- [docs/ci.md](../../docs/ci.md)
- [docs/contract-corpus.md](../../docs/contract-corpus.md)
- [docs/development.md](../../docs/development.md)
- [docs/mvp-scope.md](../../docs/mvp-scope.md)
- [docs/next-steps.md](../../docs/next-steps.md)
- [docs/object-schemas-profile.md](../../docs/object-schemas-profile.md)
- [docs/operations-profile.md](../../docs/operations-profile.md)
- [docs/parameter-presence-profile.md](../../docs/parameter-presence-profile.md)
- [docs/planning-review.md](../../docs/planning-review.md)
- [docs/request-body-profile.md](../../docs/request-body-profile.md)
- [docs/response-policy-profile.md](../../docs/response-policy-profile.md)
- [docs/roadmap.md](../../docs/roadmap.md)
- [docs/schema-values-profile.md](../../docs/schema-values-profile.md)
- [docs/test-strategy.md](../../docs/test-strategy.md)

## Navegación del contrato candidato

Actualización del piloto (16 de octubre): prepare-pilot.ps1 también genera .gitattributes con `.guard-action/** -text`. pilot-kit.test.mjs clona el consumidor con core.autocrlf=true antes del caso incompatible: protege los bytes del paquete y sus hashes al pasar por Git, no solo al copiar directorios.

Candidato privado: [verify-package.mjs](../../scripts/verify-package.mjs) valida conjunto cerrado y hashes sin seguir symlinks; [archive-evaluation.ps1](../../scripts/archive-evaluation.ps1) crea ZIP/checksum; [install-evaluation.ps1](../../scripts/install-evaluation.ps1) limita y valida entradas antes de extraer. [prepare-pilot.ps1](../../scripts/prepare-pilot.ps1) genera kit con [workflow](../../examples/pilot/openapi-guard.yml); [build-candidate.ps1](../../scripts/build-candidate.ps1) verifica versión/checkout, builds/tests y procedencia. [Integridad](../../scripts/package-integrity.test.mjs), [piloto](../../scripts/pilot-kit.test.mjs) y [build](../../scripts/candidate-build.test.mjs) contienen regresiones. [Roadmap de aceptación](../candidate-roadmap.md).

Distribución evaluable: [action.yml](../../action.yml) declara inputs, outputs y setup de Java; [action-runner.mjs](../../scripts/action-runner.mjs) resuelve revisiones del evento, valida SHA256, delega al CLI Git y publica rutas de informes nuevos. [prepare-evaluation.ps1](../../scripts/prepare-evaluation.ps1) copia el JAR/scripts/ejemplos e inventaría evidencia embebida con nombres seguros. [Pruebas de Action](../../scripts/action-runner.test.mjs) y [paquete](../../scripts/evaluation-package.test.mjs) usan repositorios externos y el JAR real. [Diseño, límites y pasos de publicación](../evaluable-distribution.md).

[OutputLanguageTest](../../src/test/java/com/openapiguard/adapter/cli/OutputLanguageTest.java) verifica JSON/CI con sangría y estructura preservada, mensajes y ayuda en inglés, y nombres Unicode originales. ReportRenderer formatea mediante el escritor de Jackson existente; las reglas y adaptadores producen descripciones en inglés. No cambian reglas, perfiles ni códigos de salida.

La presentación CI se encuentra en [github-presentation.mjs](../../scripts/github-presentation.mjs), con [pruebas](../../scripts/github-presentation.test.mjs). annotations traduce hallazgos a comandos escapados, findings agrega errores de integración, positions valida coordenadas/rutas y findingSummary genera la tabla y cobertura. `operation` obtiene contexto estructurado, `outcome` distingue estados, `consoleSummary` emite una línea por hallazgo y `checkExitCode` aplica la política sin ocultar revisión pendiente. action-runner valida `fail-on-breaking`, conserva `exit-code` y publica `check-exit-code`; git-contracts añade policy al envoltorio, conserva el código original y agrupa anotaciones. [Guía y límites](../github-annotations.md). ReportRenderer añade location a las rupturas de --format=ci y CandidateSource expone coordenadas sin modificar el dominio.

[CandidateSource](../../src/main/java/com/openapiguard/adapter/cli/CandidateSource.java): constructor indexa posiciones; visit recorre tokens, describe selecciona definición/uso o ancestro, excerpt acota contexto, token escapa RFC 6901 y single elimina controles/saltos. Sin E/S ni semántica de compatibilidad. [Diseño](../source-locations.md).

[SourceOutputTest](../../src/test/java/com/openapiguard/adapter/cli/SourceOutputTest.java): diez regresiones YAML/JSON, referencias, eliminaciones, equivalencia, controles y errores de lectura. PackagedCliIT verifica también el JAR real.
