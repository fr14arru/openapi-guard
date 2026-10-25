import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {repositoryPath, titleOf, rewriteMarkdown, fileGroup, explanationFor} from './content.mjs';
import {extendedScenarios} from './scenarios.mjs';

const website = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const repo = path.resolve(website, '..');

async function walk(relative) {
  const entries = await fs.readdir(path.join(repo, relative), {withFileTypes: true});
  const results = await Promise.all(entries.map(async entry => {
    if (['node_modules', '.generated', '.docusaurus', 'build', '.pnpm-store'].includes(entry.name)) return [];
    const child = repositoryPath(path.join(relative, entry.name));
    return entry.isDirectory() ? walk(child) : [child];
  }));
  return results.flat();
}

const allFiles = [
  ...await walk('src'), ...await walk('docs'), ...await walk('scripts'),
  ...await walk('examples'), ...await walk('website'), ...await walk('.github'),
  'pom.xml', '.gitignore', 'AGENTS.md', 'README.md', 'LICENSE', 'CONTRIBUTING.md', 'guard.ps1', 'action.yml',
].filter(file => /\.(?:java|md|mjs|js|jsx|css|json|yaml|yml|xml|properties|ps1)$/.test(file) || file === '.gitignore' || file === 'LICENSE')
  .sort();
const availableFiles = new Set(allFiles);
const docs = allFiles.filter(file => file.startsWith('docs/') && file.endsWith('.md'));
const availableDocs = new Set(docs);
const generated = path.join(website, '.generated');
await fs.mkdir(path.join(generated, 'docs'), {recursive: true});
const files = await Promise.all(allFiles.map(async file => {
  const content = await fs.readFile(path.join(repo, file), 'utf8');
  const summary = content.match(/\/\*\*([\s\S]*?)\*\//)?.[1].replace(/^\s*\* ?/gm, '').trim();
  return {path: file, group: fileGroup(file), content, summary: summary ?? titleOf(content, file), explanation: explanationFor(file)};
}));
for (const file of files.filter(file => availableDocs.has(file.path))) {
  const relative = file.path.slice(5);
  const destination = path.join(generated, 'docs', relative);
  await fs.mkdir(path.dirname(destination), {recursive: true});
  const slug = relative.replace(/\.md$/, '');
  await fs.writeFile(destination, `---\nslug: /${slug}\n---\n\n` + rewriteMarkdown(file.path, file.content, availableDocs, availableFiles));
}
const manifest = JSON.parse(await fs.readFile(path.join(repo, 'src/test/resources/openapi/corpus/cases.json'), 'utf8'));
const findFile = file => files.find(item => item.path === file)?.content;
const vocabulary = {absent: 'ausente', optional: 'opcional', required: 'obligatorio', to: '→',
  equivalent: 'equivalente', invalid: 'inválido', duplicate: 'duplicado', path: 'ruta',
  removal: 'retirada', removed: 'retirado', new: 'nuevo', empty: 'vacío', inherited: 'heredado',
  override: 'reemplazo', excluded: 'excluido', response: 'respuesta', renamed: 'renombrado'};
const corpus = manifest.map(item => ({
  ...item,
  title: item.id.split('-').map(word => vocabulary[word] ?? word).join(' '), family: 'Presencia e identidades (corpus histórico)',
  profile: 'parameters', origin: 'corpus',
  baselinePath: `src/test/resources/openapi/corpus/${item.baseline}`,
  candidatePath: `src/test/resources/openapi/corpus/${item.candidate}`,
  baseline: findFile(`src/test/resources/openapi/corpus/${item.baseline}`),
  candidate: findFile(`src/test/resources/openapi/corpus/${item.candidate}`),
  explanation: 'Expectativa del manifest original; este caso se ejecuta en ContractCorpusTest con el perfil histórico de parámetros.',
}));
const scenarios = [...corpus, ...extendedScenarios()];
if (new Set(scenarios.map(item => item.id)).size !== scenarios.length) throw new Error('IDs de escenarios duplicados');
const search = files.map(file => ({title: file.path, kind: file.group, text: file.content, url: `/code?file=${encodeURIComponent(file.path)}`}));
search.push(...docs.map(file => ({title: titleOf(findFile(file), file), kind: 'Guía', text: findFile(file), url: '/docs/' + file.slice(5).replace(/\.md$/, '')})));
search.push(...scenarios.map(item => ({title: item.title, kind: 'Caso de uso', text: `${item.family} ${item.explanation} ${item.rules.join(' ')} ${item.verdict}`, url: `/cases?id=${encodeURIComponent(item.id)}`})));
await fs.writeFile(path.join(generated, 'catalog.json'), JSON.stringify({files, scenarios, search}));
console.log(`Contenido generado: ${docs.length} guías, ${files.length} archivos y ${scenarios.length} comparaciones.`);
