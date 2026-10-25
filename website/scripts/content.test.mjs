import test from 'node:test';
import assert from 'node:assert/strict';
import {rewriteLink, rewriteMarkdown, fileGroup} from './content.mjs';
import {extendedScenarios} from './scenarios.mjs';
import fs from 'node:fs/promises';
import {createRequire} from 'node:module';

test('reading roadmap and source-location guide are reachable in the sidebar', async () => {
  const sidebar = createRequire(import.meta.url)('../sidebars.js');
  const flatten = items => items.flatMap(item => typeof item === 'string' ? [item] : item.items ? flatten(item.items) : []);
  const pages = flatten(sidebar.review);
  assert.ok(pages.includes('reading-roadmap'));
  assert.ok(pages.includes('source-locations'));
  for (const page of pages) await fs.access(new URL(`../../docs/${page}.md`, import.meta.url));
});

test('links to repository files open the local explorer and encode path characters', () => {
  assert.equal(rewriteLink('docs/code/inventory.md', '../../src/example.java', new Set(), new Set(['src/example.java'])), '/code?file=src%2Fexample.java');
});
test('documentation links retain page and anchor', () => {
  assert.equal(rewriteLink('docs/code/README.md', '../architecture.md#objetivo', new Set(['docs/architecture.md']), new Set()), '/docs/architecture#objetivo');
});
test('unknown local links fail content preparation', () => {
  assert.throws(() => rewriteLink('docs/a.md', 'missing.md', new Set(), new Set()), /no resuelto/);
});
test('literal examples inside code fences are not rewritten', () => {
  const input = '```powershell\n[example](missing.md)\n```\n[guía](a.md)';
  assert.equal(rewriteMarkdown('docs/b.md', input, new Set(['docs/a.md']), new Set()), '```powershell\n[example](missing.md)\n```\n[guía](/docs/a)');
});
test('production domain files are grouped separately from adapters', () => {
  assert.equal(fileGroup('src/main/java/com/openapiguard/domain/OperationId.java'), 'Dominio');
  assert.equal(fileGroup('src/main/java/com/openapiguard/adapter/openapi/SchemaValuesReader.java'), 'Adaptador OpenAPI');
});
test('extended cases have unique identities and explicit expectations', () => {
  const cases = extendedScenarios();
  assert.equal(new Set(cases.map(item => item.id)).size, cases.length);
  for (const item of cases) {
    assert.ok(item.baseline && item.candidate && item.explanation);
    assert.ok(['COMPATIBLE', 'BREAKING', 'INDETERMINATE'].includes(item.verdict));
    assert.ok(['COMPLETED', 'PARTIAL', 'FAILED'].includes(item.status));
  }
});
