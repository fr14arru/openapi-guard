import React from 'react';
import Layout from '@theme/Layout';
import Link from '@docusaurus/Link';
import {catalog} from '../components/Explorer';

export default function Home() {
  const cards = [
    ['01', 'Entender el proyecto', 'Qué compara hoy, qué garantiza y hacia dónde puede evolucionar.', '/docs/portal/project'],
    ['02', 'Ver el roadmap', 'Implementado, pendiente y propuesto, con criterios para avanzar.', '/docs/portal/roadmap'],
    ['03', 'Explorar comparaciones', 'Contratos anteriores y candidatos, resultados esperados y casos de revisión.', '/cases'],
    ['04', 'Recorrer el código', 'Archivos completos, líneas, filtros por paquete y explicaciones conectadas.', '/code'],
  ];
  return <Layout title="Comprende el motor" description="Guía visual y local de OpenAPI Breaking Change Guard">
    <main className="portal-home container">
      <div className="hero-copy"><p className="eyebrow">OPENAPI BREAKING CHANGE GUARD</p>
        <h1>Del cambio de contrato<br/>a una decisión comprensible.</h1>
        <p>Una guía para utilizar el motor, revisar sus garantías y entender el código paso a paso.</p>
        <div className="hero-actions"><Link className="button button--primary button--lg" to="/docs/portal/getting-started">Empezar con un ejemplo</Link><Link className="button button--secondary button--lg" to="/docs/code/README">Leer la implementación</Link></div>
      </div>
      <section className="stat-grid" aria-label="Contenido disponible">
        <div><strong>{catalog.scenarios.length}</strong><span>comparaciones documentadas</span></div>
        <div><strong>7</strong><span>perfiles conservados</span></div>
        <div><strong>{catalog.files.length}</strong><span>archivos para explorar</span></div>
        <div><strong>0</strong><span>servicios externos necesarios</span></div>
      </section>
      <section className="journey-grid" aria-label="Recorrido recomendado">{cards.map(([number, title, text, to]) => <Link className="journey-card" to={to} key={number}><span className="step-number">{number}</span><h2>{title}</h2><p>{text}</p><span className="card-action">Abrir guía →</span></Link>)}</section>
      <aside className="coverage-note"><strong>Compatible dentro de una cobertura explícita.</strong><p>El perfil full reúne lo implementado. Un análisis incompleto exige revisión; no certificamos toda la API por ausencia de hallazgos.</p><Link to="/docs/portal/use-cases">Consultar casos y límites →</Link></aside>
    </main>
  </Layout>;
}
