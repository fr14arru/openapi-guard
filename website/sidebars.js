module.exports = {
  review: [
    { type: 'category', label: '1. Entender el proyecto', collapsed: false, items: [
      'portal/project', 'open-source', 'github-governance', 'reading-roadmap', 'portal/roadmap', 'portal/getting-started', 'portal/improvements',
    ] },
    { type: 'category', label: '2. Comparar y decidir', items: [
      'github-actions-quickstart', 'portal/use-cases', 'source-locations', 'github-annotations',
      { type: 'link', label: 'Catálogo interactivo', href: '/cases' },
      'operations-profile', 'parameter-presence-profile', 'request-body-profile',
      'schema-values-profile', 'object-schemas-profile', 'array-schemas-profile', 'response-policy-profile',
    ] },
    { type: 'category', label: '3. Leer todo el código', items: [
      'code/README', { type: 'link', label: 'Explorador de archivos', href: '/code' },
      'code/runtime', 'code/domain', 'code/inclusion', 'code/openapi',
      'code/build', 'code/tests', 'code/inventory', 'code/maintenance',
    ] },
    { type: 'category', label: '4. Referencia y decisiones', items: [
      'architecture', 'mvp-scope', 'roadmap', 'third-party-review', 'next-steps', 'development',
      'test-strategy', 'contract-corpus', 'ci', 'git-integration', 'evaluable-distribution', 'candidate-roadmap', 'pilot-delivery', 'pilot-offer', 'product-usability', 'planning-review', 'portal/documentation-site',
    ] },
  ],
};
