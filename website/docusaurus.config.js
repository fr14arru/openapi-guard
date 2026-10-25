const config = {
  title: 'OpenAPI Guard',
  tagline: 'Comprende el contrato, el resultado y el código',
  url: 'http://localhost:3000',
  baseUrl: '/',
  organizationName: 'fr14arru',
  projectName: 'openapi-guard',
  onBrokenLinks: 'throw',
  i18n: { defaultLocale: 'es', locales: ['es'] },
  markdown: { format: 'md', mermaid: true, hooks: { onBrokenMarkdownLinks: 'throw' } },
  themes: ['@docusaurus/theme-mermaid'],
  // Mermaid's optional ELK import is still resolved by this webpack build.
  // All repository diagrams use built-in Dagre; exclude the unused layout.
  plugins: [function dagreOnly() {
    return {
      name: 'local-dagre-only',
      configureWebpack() {
        return {resolve: {fallback: {'@mermaid-js/layout-elk': false}}};
      },
    };
  }],
  presets: [['classic', {
    docs: { path: '.generated/docs', sidebarPath: './sidebars.js', routeBasePath: 'docs' },
    blog: false,
    theme: { customCss: './src/css/custom.css' },
    sitemap: false,
  }]],
  themeConfig: {
    navbar: {
      title: 'OpenAPI Guard',
      items: [
        { to: '/docs/portal/project', label: 'El proyecto', position: 'left' },
        { to: '/docs/portal/roadmap', label: 'Roadmap', position: 'left' },
        { to: '/cases', label: 'Casos de uso', position: 'left' },
        { to: '/code', label: 'Explorar código', position: 'left' },
        { to: '/search', label: 'Buscar', position: 'right' },
      ],
    },
    footer: {
      style: 'dark',
      copyright: 'Documentación local · Cobertura explícita · Sin servicios externos',
    },
    // XML is an alias of the built-in markup grammar, not a prism-xml module.
    prism: { additionalLanguages: ['java', 'json', 'yaml', 'powershell', 'bash'] },
    colorMode: { respectPrefersColorScheme: true },
  },
};
module.exports = config;
