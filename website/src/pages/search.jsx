import React, {useState, useMemo} from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import {catalog, PageIntro} from '../components/Explorer';

export default function Search() {
  const [query, setQuery] = useState('');
  const [kind, setKind] = useState('');
  const kinds = Array.from(new Set(catalog.search.map(item => item.kind))).sort();
  const matches = useMemo(() => query.trim().length < 2 ? [] : catalog.search.filter(item => (!kind || item.kind === kind) && `${item.title} ${item.text}`.toLowerCase().includes(query.toLowerCase())).slice(0, 60), [query, kind]);
  function excerpt(text) {
    const index = text.toLowerCase().indexOf(query.toLowerCase());
    return text.slice(Math.max(0, index - 60), Math.max(0, index - 60) + 220).replaceAll('\n', ' ');
  }
  return <Layout title="Buscar" description="Búsqueda local en guías, código y comparaciones"><main className="container search-page"><PageIntro eyebrow="ENCUENTRA EL CONTEXTO" title="Busca en todo el proyecto"><p>Guías, archivos y casos en un índice local. Escribe al menos dos caracteres; se muestran hasta 60 coincidencias.</p></PageIntro><div className="explorer-toolbar"><label>Texto a buscar<input autoFocus value={query} onChange={event => setQuery(event.target.value)} placeholder="additionalProperties, null, SchemaUse…"/></label><label>Tipo de contenido<select value={kind} onChange={event => setKind(event.target.value)}><option value="">Todo</option>{kinds.map(item => <option key={item} value={item}>{item}</option>)}</select></label></div>{matches.map((item, index) => <Link className="search-result" key={`${item.url}-${index}`} to={item.url}><small>{item.kind}</small><h2>{item.title}</h2><p>{excerpt(item.text)}</p></Link>)}{query.trim().length >= 2 && !matches.length && <p>No hay coincidencias. Prueba con el nombre de la regla o una palabra más corta.</p>}</main></Layout>;
}
