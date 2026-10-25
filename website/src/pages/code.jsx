import React, {useState, useMemo} from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import CodeBlock from '@theme/CodeBlock';
import {catalog, useQueryValue, PageIntro, GuideLink} from '../components/Explorer';

function language(file) {
  const extension = file.split('.').pop();
  return {java: 'java', json: 'json', yaml: 'yaml', xml: 'xml', ps1: 'powershell', properties: 'properties', js: 'javascript', jsx: 'jsx', mjs: 'javascript', css: 'css', md: 'markdown'}[extension] ?? 'text';
}

export default function CodeExplorer() {
  const requested = useQueryValue('file');
  const [query, setQuery] = useState('');
  const [group, setGroup] = useState('');
  const [allLines, setAllLines] = useState(false);
  const active = catalog.files.find(file => file.path === requested) ?? catalog.files.find(file => file.path.endsWith('/CompareContracts.java'));
  const groups = Array.from(new Set(catalog.files.map(file => file.group))).sort();
  const matches = useMemo(() => catalog.files.filter(file => (!group || file.group === group) && `${file.path}\n${file.content}`.toLowerCase().includes(query.toLowerCase())), [query, group]);
  const lines = active.content.split('\n');
  const display = allLines ? active.content : lines.slice(0, 1500).join('\n');
  const related = catalog.files.filter(file => file.group === 'Pruebas' && file.path.includes(active.path.split('/').pop().replace('.java', '')));
  return <Layout title="Explorar código" description="Código íntegro con navegación y explicaciones">
    <main className="container explorer-page">
      <PageIntro eyebrow="LECTURA GUIADA" title="Todo el código, en contexto"><p>Busca una clase o un concepto, filtra por responsabilidad y abre su explicación. Las fuentes se regeneran desde el repositorio al construir el portal.</p></PageIntro>
      <div className="explorer-toolbar"><label>Buscar archivo o contenido<input value={query} onChange={event => setQuery(event.target.value)} placeholder="SchemaInclusion, required, referencias…"/></label><label>Responsabilidad<select value={group} onChange={event => setGroup(event.target.value)}><option value="">Todos los grupos</option>{groups.map(item => <option key={item} value={item}>{item}</option>)}</select></label><span>{matches.length} archivos</span></div>
      <div className="explorer-split"><nav className="file-list" aria-label="Archivos del repositorio">{matches.map(file => <Link className={`file-row ${active.path === file.path ? 'selected' : ''}`} to={`/code?file=${encodeURIComponent(file.path)}`} key={file.path} onClick={() => setAllLines(false)}><strong>{file.path.split('/').pop()}</strong><small>{file.path}</small></Link>)}{!matches.length && <p>No hay archivos con ese filtro.</p>}</nav>
        <article className="file-detail"><div className="file-heading"><span className="eyebrow">{active.group}</span><h2>{active.path.split('/').pop()}</h2><p className="file-path">{active.path}</p><p>{active.summary}</p><GuideLink to={active.explanation}>Entender esta pieza</GuideLink></div>
          {related.length > 0 && <p>Pruebas relacionadas: {related.map(file => <Link className="inline-test" key={file.path} to={`/code?file=${encodeURIComponent(file.path)}`}>{file.path.split('/').pop()}</Link>)}</p>}
          <p className="muted">{lines.length} líneas · fuente completa del repositorio · lectura sin modificar archivos</p>
          <CodeBlock language={language(active.path)} showLineNumbers>{display}</CodeBlock>
          {!allLines && lines.length > 1500 && <button className="button button--secondary" onClick={() => setAllLines(true)}>Mostrar las {lines.length} líneas completas</button>}
        </article></div>
    </main>
  </Layout>;
}
