import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';

// Every filter needs distinct selectable values in the production artifact.
// Client hydration and interaction must also be checked in the browser.
const root = new URL('../', import.meta.url);
const catalog = JSON.parse(await fs.readFile(new URL('.generated/catalog.json', root), 'utf8'));
async function optionValues(page) {
  const html = await fs.readFile(new URL(`build/${page}/index.html`, root), 'utf8');
  return new Set([...html.matchAll(/<option\b([^>]*)>/g)].map(match => {
    const value = match[1].match(/\bvalue=(?:"([^"]*)"|'([^']*)'|([^\s>]+))/);
    return value ? value[1] ?? value[2] ?? value[3] : undefined;
  }));
}
for (const [page, values] of [
  ['code', catalog.files.map(file => file.group)],
  ['cases', catalog.scenarios.map(scenario => scenario.family)],
  ['search', catalog.search.map(entry => entry.kind)],
]) {
  test(`production ${page} preserves every distinct filter option`, async () => {
    const actual = await optionValues(page);
    for (const expected of new Set(values)) assert.ok(actual.has(expected), `Missing selectable ${expected}`);
  });
}
