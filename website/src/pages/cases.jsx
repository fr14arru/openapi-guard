import React, {useState, useMemo} from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import CodeBlock from '@theme/CodeBlock';
import {catalog, useQueryValue, Verdict, PageIntro, GuideLink} from '../components/Explorer';

export default function Cases() {
  const requested = useQueryValue('id');
  const [query, setQuery] = useState('');
  const [family, setFamily] = useState('');
  const [verdict, setVerdict] = useState('');
  const families = Array.from(new Set(catalog.scenarios.map(item => item.family))).sort();
  const matches = useMemo(() => catalog.scenarios.filter(item => (!family || family === item.family) && (!verdict || verdict === item.verdict) && `${item.title} ${item.id} ${item.explanation} ${item.rules.join(' ')} ${item.diagnostic ?? ''}`.toLowerCase().includes(query.toLowerCase())), [query, family, verdict]);
  const active = catalog.scenarios.find(item => item.id === requested) ?? catalog.scenarios.find(item => item.id === 'guide-integer-number-request');
  const exit = active.status === 'FAILED' ? 2 : active.verdict === 'BREAKING' ? 1 : active.verdict === 'COMPATIBLE' ? 0 : 3;
  return <Layout title="Casos de uso" description="Comparaciones OpenAPI con expectativas explícitas">
    <main className="container explorer-page"><PageIntro eyebrow="CONTRATOS Y DECISIONES" title="Ver el cambio antes de leer la regla"><p>{catalog.scenarios.length} comparaciones: 146 del corpus histórico y {catalog.scenarios.length - 146} ejemplos ampliados. Cubren familias y fronteras; no todos los contratos posibles.</p></PageIntro>
      <div className="explorer-toolbar"><label>Buscar caso<input value={query} onChange={event => setQuery(event.target.value)} placeholder="nullable, retirada, arrays, REQUEST…"/></label><label>Familia<select value={family} onChange={event => setFamily(event.target.value)}><option value="">Todas las familias</option>{families.map(item => <option key={item} value={item}>{item}</option>)}</select></label><label>Resultado<select value={verdict} onChange={event => setVerdict(event.target.value)}><option value="">Todos los resultados</option><option value="COMPATIBLE">Compatible</option><option value="BREAKING">Ruptura</option><option value="INDETERMINATE">Revisión / entrada fallida</option></select></label><span>{matches.length} casos</span></div>
      <div className="explorer-split"><nav className="file-list" aria-label="Comparaciones">{matches.map(item => <Link className={`file-row ${active.id === item.id ? 'selected' : ''}`} key={item.id} to={`/cases?id=${encodeURIComponent(item.id)}`}><strong>{item.title}</strong><small>{item.profile} · {item.verdict}</small></Link>)}{!matches.length && <p>No hay casos con ese filtro.</p>}</nav>
        <article className="file-detail"><p className="eyebrow">{active.family}</p><h2>{active.title}</h2><Verdict verdict={active.verdict} status={active.status}/><p className="case-explanation">{active.explanation}</p>
          <dl className="case-facts"><div><dt>Perfil</dt><dd>{active.profile}</dd></div><div><dt>Estado esperado</dt><dd>{active.status}</dd></div><div><dt>Código esperado</dt><dd>{exit}</dd></div><div><dt>Origen</dt><dd>{active.origin === 'corpus' ? 'Corpus histórico de tests' : 'Ejemplo ampliado verificado'}</dd></div></dl>
          <p className="muted">ID: {active.id}</p><p><strong>Reglas esperadas:</strong> {active.rules.length ? active.rules.join(', ') : 'Sin ruptura demostrada'}</p>{active.diagnostic && <p><strong>Diagnóstico esperado:</strong> {active.diagnostic}</p>}
          <GuideLink to="/docs/portal/use-cases">Interpretar familias y límites</GuideLink>{' '}<GuideLink to="/docs/code/domain">Seguir las reglas</GuideLink>
          <div className="contracts-grid"><section><h3>Anterior · baseline</h3>{active.baselinePath && <Link to={`/code?file=${encodeURIComponent(active.baselinePath)}`}>Abrir fixture original</Link>}<CodeBlock language={active.baseline.trim().startsWith('{') ? 'json' : 'yaml'} showLineNumbers>{active.baseline}</CodeBlock></section><section><h3>Propuesto · candidate</h3>{active.candidatePath && <Link to={`/code?file=${encodeURIComponent(active.candidatePath)}`}>Abrir fixture original</Link>}<CodeBlock language={active.candidate.trim().startsWith('{') ? 'json' : 'yaml'} showLineNumbers>{active.candidate}</CodeBlock></section></div>
          <aside className="coverage-note"><strong>Este navegador muestra ejemplos verificados.</strong><p>Para comparar tus contratos, utiliza la CLI local. El portal no ejecuta el motor en el navegador ni envía datos a servidores.</p></aside>
        </article></div>
    </main>
  </Layout>;
}
