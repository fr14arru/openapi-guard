import React from 'react';
import Link from '@docusaurus/Link';
import {useLocation} from '@docusaurus/router';
import catalog from '@site/.generated/catalog.json';

export {catalog};

export function useQueryValue(name) {
  return new URLSearchParams(useLocation().search).get(name) ?? '';
}

export function Verdict({verdict, status}) {
  const label = verdict === 'COMPATIBLE' ? 'Compatible en el perfil' : verdict === 'BREAKING' ? 'Ruptura detectada' : 'Revisión o entrada fallida';
  return <span className={`verdict verdict-${verdict.toLowerCase()}`}>{label}{status === 'PARTIAL' ? ' · análisis parcial' : status === 'FAILED' ? ' · error de entrada' : ''}</span>;
}

export function PageIntro({eyebrow, title, children}) {
  return <header className="page-intro"><p className="eyebrow">{eyebrow}</p><h1>{title}</h1><div className="intro-copy">{children}</div></header>;
}

export function GuideLink({to, children}) {
  return <Link className="button button--secondary button--sm" to={to}>{children}</Link>;
}
