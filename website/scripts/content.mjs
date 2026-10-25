import path from 'node:path';

/** Repository-relative POSIX paths make output independent of Windows drive letters. */
export function repositoryPath(value) {
  return value.split(path.sep).join('/');
}

export function titleOf(markdown, fallback) {
  return markdown.match(/^#\s+(.+)$/m)?.[1] ?? fallback;
}

export function rewriteLink(origin, target, availableDocs, availableFiles) {
  if (/^(?:[a-z][a-z\d+.-]*:|\/|#)/i.test(target)) return target;
  const [file, ...fragments] = target.split('#');
  const relative = path.posix.normalize(path.posix.join(path.posix.dirname(origin), file));
  if (availableDocs.has(relative)) {
    return `/docs/${relative.slice(5).replace(/\.md$/, '')}${fragments.length ? `#${fragments.join('#')}` : ''}`;
  }
  if (availableFiles.has(relative)) return `/code?file=${encodeURIComponent(relative)}`;
  throw new Error(`Enlace local no resuelto: ${origin} -> ${target}`);
}

export function rewriteMarkdown(origin, markdown, docs, files) {
  // Keep commands/examples inside fences literal; only rewrite prose links.
  return markdown.split(/(^```[^\n]*\n[\s\S]*?^```[ \t]*$)/m).map((segment, index) =>
    index % 2 ? segment : segment.replace(/\[([^\]]+)\]\(([^)]+)\)/g,
      (_, label, target) => `[${label}](${rewriteLink(origin, target, docs, files)})`)).join('');
}

export function fileGroup(file) {
  if (file.startsWith('src/main/java/')) return file.includes('/domain/') ? 'Dominio' :
    file.includes('/application/') ? 'Aplicación' : file.includes('/adapter/openapi/') ? 'Adaptador OpenAPI' : 'CLI';
  if (file.startsWith('src/test/java/')) return 'Pruebas';
  if (file.startsWith('src/test/resources/') || file.startsWith('examples/')) return 'Contratos y ejemplos';
  if (file.startsWith('docs/') || /(?:README|AGENTS)\.md$/.test(file)) return 'Documentación';
  if (file.startsWith('website/')) return 'Portal documental';
  return 'Build y scripts';
}

export function explanationFor(file) {
  if (/scripts\/(verify-package|archive-evaluation|install-evaluation|prepare-pilot|build-candidate|package-integrity|pilot-kit|candidate-build)/.test(file)) return '/docs/candidate-roadmap';
  if (file==='action.yml' || /scripts\/(action-runner|prepare-evaluation|evaluation-package)/.test(file)) return '/docs/evaluable-distribution';
  if (/scripts\/github-presentation/.test(file)) return '/docs/github-annotations';
  if (file.startsWith('.github/') || /scripts\/(git-contracts|github-workflow|setup-maven)/.test(file)) return '/docs/git-integration';
  if (file.endsWith('/SchemaInclusion.java')) return '/docs/code/inclusion';
  if (file.startsWith('src/test/')) return '/docs/code/tests';
  if (file.includes('/domain/')) return '/docs/code/domain';
  if (file.includes('/adapter/openapi/')) return '/docs/code/openapi';
  if (file.includes('/adapter/cli/') || file.includes('/application/')) return '/docs/code/runtime';
  if (file.startsWith('website/')) return '/docs/portal/documentation-site';
  if (file.startsWith('examples/')) return '/docs/portal/use-cases';
  if (file.startsWith('docs/')) return '/docs/' + file.slice(5).replace(/\.md$/, '');
  return '/docs/code/build';
}
