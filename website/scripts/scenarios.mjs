/** Executable documentation examples. Expectations are independent of engine output. */
export function extendedScenarios() {
  const cases = [];
  const contract = (schema, direction = 'REQUEST', version = '3.0.3') => ({
    openapi: version, info: {title: 'Documentation example', version: '1'},
    paths: {'/values': {post: {
      ...(direction === 'REQUEST' ? {requestBody: {content: {'application/json': {schema}}}} : {}),
      responses: {'200': {description: 'OK', ...(direction === 'RESPONSE' ? {content: {'application/json': {schema}}} : {})}},
    }}},
  });
  const add = (id, title, family, old, updated, verdict, rules = [], status = 'COMPLETED', explanation = title, diagnostic, profile = 'full') => {
    cases.push({id: `guide-${id}`, title, family, baseline: typeof old === 'string' ? old : JSON.stringify(old, null, 2),
      candidate: typeof updated === 'string' ? updated : JSON.stringify(updated, null, 2), verdict, rules, status,
      explanation, diagnostic, profile, origin: 'guide'});
  };
  const object = (required = [], additionalProperties = true, properties = {name: {type: 'string'}}) =>
    ({type: 'object', properties, required, additionalProperties});
  const array = (items, minItems = 0, maxItems = 10) => ({type: 'array', items, minItems, maxItems});
  const pairs = [
    ['integer-number', 'Ampliar integer a number', 'Tipos y valores', {type: 'integer'}, {type: 'number'}, 'COMPATIBLE', 'BREAKING'],
    ['enum', 'Restringir un enum', 'Tipos y valores', {type: 'string', enum: ['a', 'b']}, {type: 'string', enum: ['a']}, 'BREAKING', 'COMPATIBLE'],
    ['nullable', 'Permitir null en OpenAPI 3.0', 'Tipos y valores', {type: 'string'}, {type: 'string', nullable: true}, 'COMPATIBLE', 'BREAKING'],
    ['minimum', 'Elevar el mínimo numérico', 'Límites conjuntos', {type: 'integer', minimum: 0}, {type: 'integer', minimum: 1}, 'BREAKING', 'COMPATIBLE'],
    ['maximum', 'Ampliar el máximo numérico', 'Límites conjuntos', {type: 'number', maximum: 10}, {type: 'number', maximum: 20}, 'COMPATIBLE', 'BREAKING'],
    ['min-length', 'Elevar la longitud mínima', 'Límites conjuntos', {type: 'string', minLength: 0}, {type: 'string', minLength: 1}, 'BREAKING', 'COMPATIBLE'],
    ['max-length', 'Ampliar la longitud máxima', 'Límites conjuntos', {type: 'string', maxLength: 10}, {type: 'string', maxLength: 20}, 'COMPATIBLE', 'BREAKING'],
    ['required-property', 'Exigir una propiedad', 'Objetos', object(), object(['name']), 'BREAKING', 'COMPATIBLE'],
    ['close-object', 'Cerrar nombres adicionales', 'Objetos', object(), object([], false), 'BREAKING', 'COMPATIBLE'],
    ['remove-closed-property', 'Retirar propiedad de objeto cerrado', 'Objetos', object([], false), object([], false, {}), 'BREAKING', 'COMPATIBLE'],
    ['remove-open-property', 'Retirar propiedad de objeto abierto', 'Objetos', object(), object([], true, {}), 'COMPATIBLE', 'BREAKING'],
    ['add-open-property', 'Declarar propiedad opcional antes libre', 'Objetos', object([], true, {}), object(), 'BREAKING', 'COMPATIBLE'],
    ['array-items', 'Ampliar integer a number en items', 'Arrays', array({type: 'integer'}), array({type: 'number'}), 'COMPATIBLE', 'BREAKING'],
    ['array-min', 'Elevar minItems', 'Arrays', array({type: 'string'}), array({type: 'string'}, 1), 'BREAKING', 'COMPATIBLE'],
    ['array-max', 'Ampliar maxItems', 'Arrays', array({type: 'string'}), array({type: 'string'}, 0, 20), 'COMPATIBLE', 'BREAKING'],
    ['array-empty', 'Cambiar items de arrays solo vacíos', 'Arrays', array({type: 'string'}, 0, 0), array({type: 'number'}, 0, 0), 'COMPATIBLE', 'COMPATIBLE'],
    ['joint-enum', 'Ampliar enum y tipo conjuntamente', 'Límites conjuntos', {type: 'integer', enum: [1, 2], minimum: 0}, {type: 'number', enum: [1, 2, 3], minimum: 0}, 'COMPATIBLE', 'BREAKING'],
  ];
  for (const [id, title, family, before, after, request, response] of pairs) {
    for (const direction of ['REQUEST', 'RESPONSE']) {
      const verdict = direction === 'REQUEST' ? request : response;
      const rule = direction === 'REQUEST' ? 'REQUEST_SCHEMA_VALUES_RESTRICTED' : 'RESPONSE_SCHEMA_VALUES_EXPANDED';
      add(`${id}-${direction.toLowerCase()}`, `${title} · ${direction === 'REQUEST' ? 'petición' : 'respuesta'}`,
        family, contract(before, direction), contract(after, direction), verdict, verdict === 'BREAKING' ? [rule] : [],
        'COMPLETED', `${title}. En ${direction === 'REQUEST' ? 'petición se comprueba anterior ⊆ candidato' : 'respuesta se comprueba candidato ⊆ anterior'}. Resultado limitado al perfil full.`);
    }
  }
  for (const [id, schema] of [
    ['pattern', {type: 'string', pattern: '^a'}],
    ['composition', {anyOf: [{type: 'string'}, {type: 'integer'}]}],
    ['contradictory', {type: 'integer', minimum: 10, maximum: 0}],
    ['schema-additional-properties', {type: 'object', additionalProperties: {type: 'string'}}],
    ['array-unique', {...array({type: 'string'}), uniqueItems: true}],
  ]) {
    add(`review-${id}`, `Restricción sin prueba completa: ${id}`, 'Revisión e incertidumbre', contract(schema), contract(schema),
      'INDETERMINATE', [], 'PARTIAL', 'Igualdad textual de una construcción no soportada o contradictoria no permite certificar compatibilidad.', 'SCHEMA_INCLUSION_REQUIRES_REVIEW');
  }
  const optionalOpaque = {name: {type: 'string'}, note: {type: 'string', pattern: '^a'}};
  add('partial-break', 'Ruptura demostrada y laguna simultáneas', 'Revisión e incertidumbre',
    contract(object([], false, optionalOpaque)), contract(object(['name'], false, optionalOpaque)),
    'BREAKING', ['REQUEST_SCHEMA_VALUES_RESTRICTED'], 'PARTIAL', 'La nueva propiedad requerida demuestra ruptura; pattern en otra propiedad mantiene el análisis parcial. Código 1.', 'SCHEMA_INCLUSION_REQUIRES_REVIEW');
  add('readonly', 'Cambio de readOnly requiere revisión', 'Objetos', contract(object()),
    contract(object([], true, {name: {type: 'string', readOnly: true}})), 'INDETERMINATE', [], 'PARTIAL',
    'Se conserva el modo de acceso original: no se supone rechazo del servidor al cambiar readOnly.', 'SCHEMA_INCLUSION_REQUIRES_REVIEW');
  for (const direction of ['REQUEST', 'RESPONSE']) {
    const verdict = direction === 'REQUEST' ? 'COMPATIBLE' : 'BREAKING';
    add(`union31-${direction.toLowerCase()}`, `Unión con null en 3.1 · ${direction}`, 'Dialectos',
      contract({type: 'string'}, direction, '3.1.1'), contract({type: ['string', 'null']}, direction, '3.1.1'), verdict,
      verdict === 'BREAKING' ? ['RESPONSE_SCHEMA_VALUES_EXPANDED'] : []);
  }
  const empty = () => ({openapi: '3.0.3', info: {title: 'Operations', version: '1'}, paths: {}});
  const operation = () => ({responses: {'200': {description: 'OK'}}});
  for (const method of ['get', 'put', 'post', 'delete', 'options', 'head', 'patch', 'trace']) {
    const old = empty(); old.paths['/pets'] = {[method]: operation()};
    const updated = empty(); updated.paths['/pets'] = {};
    add(`method-${method}`, `Retirar ${method.toUpperCase()} /pets`, 'Operaciones', old, updated, 'BREAKING', ['HTTP_METHOD_REMOVED']);
  }
  const oldEndpoint = empty(); oldEndpoint.paths['/pets'] = {get: operation(), post: operation()};
  add('endpoint', 'Retirar endpoint sin duplicar métodos', 'Operaciones', oldEndpoint, empty(), 'BREAKING', ['ENDPOINT_REMOVED']);
  add('endpoint-add', 'Añadir un endpoint', 'Operaciones', empty(), oldEndpoint, 'COMPATIBLE');
  const body = required => {const doc = contract({type: 'string'}); doc.paths['/values'].post.requestBody.required = required; return doc;};
  add('body-required', 'Cuerpo opcional que pasa a obligatorio', 'Cuerpos y formatos', body(false), body(true), 'BREAKING', ['REQUEST_BODY_BECAME_REQUIRED']);
  add('body-relaxed', 'Cuerpo obligatorio que pasa a opcional', 'Cuerpos y formatos', body(true), body(false), 'COMPATIBLE');
  const noBody = empty(); noBody.paths['/values'] = {post: operation()};
  add('body-new-required', 'Añadir cuerpo obligatorio', 'Cuerpos y formatos', noBody, body(true), 'BREAKING', ['REQUEST_BODY_REQUIRED_ADDED']);
  add('body-removed', 'Retirar documentación del cuerpo', 'Cuerpos y formatos', body(false), noBody, 'INDETERMINATE', [], 'PARTIAL',
    'La ausencia del cuerpo en el contrato no demuestra que el servidor lo rechace.', 'REQUEST_BODY_REMOVAL_REQUIRES_REVIEW');
  const withText = body(false); withText.paths['/values'].post.requestBody.content['text/plain'] = {schema: {type: 'string'}};
  add('request-media', 'Retirar text/plain de petición', 'Cuerpos y formatos', withText, body(false), 'BREAKING', ['REQUEST_MEDIA_TYPE_REMOVED'], 'PARTIAL',
    'La retirada de formato está demostrada; el uso anterior no JSON añade revisión en la cobertura de schemas.', 'REQUEST_SCHEMA_COVERAGE_REQUIRES_REVIEW');
  const responses = map => {const doc = empty(); doc.paths['/values'] = {get: {responses: map}}; return doc;};
  const ok = {description: 'OK'};
  const json = {description: 'OK', content: {'application/json': {schema: {type: 'string'}}}};
  add('response-code-remove', 'Retirar 204 sin cuerpo', 'Política de respuestas', responses({'200': ok, '204': ok}), responses({'200': ok}), 'BREAKING', ['RESPONSE_STATUS_REMOVED']);
  add('response-code-add', 'Añadir 204', 'Política de respuestas', responses({'200': ok}), responses({'200': ok, '204': ok}), 'INDETERMINATE', [], 'PARTIAL', 'Una adición documental requiere revisar consumidores y comportamiento.', 'RESPONSE_STATUS_ADDED_REQUIRES_REVIEW');
  add('response-media-remove', 'Retirar payload JSON de respuesta', 'Política de respuestas', responses({'200': json}), responses({'200': ok}), 'BREAKING', ['RESPONSE_MEDIA_TYPE_REMOVED']);
  add('response-media-add', 'Añadir payload JSON de respuesta', 'Política de respuestas', responses({'200': ok}), responses({'200': json}), 'INDETERMINATE', [], 'PARTIAL', 'Revisar negociación antes de certificar una adición de formato.', 'RESPONSE_MEDIA_TYPE_ADDED_REQUIRES_REVIEW');
  for (const fallback of ['default', '2XX']) add(`response-fallback-${fallback}`, `Retirar 204 con ${fallback}`, 'Política de respuestas',
    responses({'200': ok, '204': ok}), responses({'200': ok, [fallback]: ok}), 'INDETERMINATE', [], 'PARTIAL', 'Fallback puede cubrir la retirada, pero su precedencia y comportamiento requieren revisión.', 'RESPONSE_STATUS_COVERAGE_REQUIRES_REVIEW');
  const reference = contract({$ref: '#/components/schemas/Name'}); reference.components = {schemas: {Name: {type: 'string'}}};
  add('internal-ref', 'Schema inline y referencia interna equivalentes', 'Referencias', contract({type: 'string'}), reference, 'COMPATIBLE');
  const cycle = contract({$ref: '#/components/schemas/Node'}); cycle.components = {schemas: {Node: {type: 'object', properties: {child: {$ref: '#/components/schemas/Node'}}}}};
  add('schema-cycle', 'Ciclo de schema requiere revisión', 'Referencias', cycle, cycle, 'INDETERMINATE', [], 'PARTIAL', 'El ciclo se corta como nodo opaco; no se expande indefinidamente.', 'SCHEMA_INCLUSION_REQUIRES_REVIEW');
  const valid = contract({type: 'string'});
  for (const [id, title, candidate] of [
    ['external', 'Referencia externa rechazada sin descargar', contract({$ref: 'https://example.invalid/schema.json'})],
    ['dangling', 'Referencia interna inexistente', contract({$ref: '#/components/schemas/Missing'})],
    ['invalid-json', 'Documento mal formado', '{invalid'],
    ['duplicate', 'Claves JSON duplicadas', JSON.stringify(valid).replace('"paths":', '"paths":{},"paths":')],
    ['unsupported-version', 'Familia OpenAPI no soportada', {...valid, openapi: '3.2.0'}],
  ]) add(`input-${id}`, title, 'Errores de entrada', valid, candidate, 'INDETERMINATE', [], 'FAILED', title, 'INVALID_OR_UNSUPPORTED_INPUT');
  add('cross-dialect', 'No comparar familias 3.0 y 3.1 entre sí', 'Dialectos', valid, {...valid, openapi: '3.1.1'}, 'INDETERMINATE', [], 'FAILED', 'Se mantienen dialectos sin migración automática.', 'UNSUPPORTED_VERSION_PAIR');
  return cases;
}
