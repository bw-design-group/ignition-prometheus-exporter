import { themes as prismThemes } from 'prism-react-renderer';
import type { Config } from '@docusaurus/types';

const config: Config = {
  title: 'Prometheus Exporter Module',
  tagline: 'Export Ignition metrics to Prometheus for monitoring and observability',
  favicon: 'img/favicon.svg',
  url: 'https://bw-design-group.github.io',
  baseUrl: '/',
  organizationName: 'bw-design-group',
  projectName: 'prometheus-exporter',
  onBrokenLinks: 'warn',
  onBrokenMarkdownLinks: 'warn',
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },

  presets: [
    [
      'classic',
      {
        docs: {
          sidebarPath: './sidebars.ts',
          routeBasePath: '/',
          editUrl:
            'https://github.com/bw-design-group/prometheus-exporter/tree/main/docs',
        },
        blog: false,
        theme: {
          customCss: require.resolve('./static/css/custom.css'),
        },
      },
    ],
  ],

  markdown: {
    mermaid: true,
  },

  themes: ['@docusaurus/theme-mermaid'],

  themeConfig: {
    navbar: {
      title: 'Prometheus Exporter Module',
      logo: {
        alt: 'Prometheus Logo',
        src: 'img/prometheus-logo.svg',
      },
      items: [
        {
          type: 'docSidebar',
          sidebarId: 'docsSidebar',
          position: 'left',
          label: 'Documentation',
        },
        {
          href: 'https://github.com/bw-design-group/prometheus-exporter',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        {
          title: 'Docs',
          items: [
            {
              label: 'Getting Started',
              to: '/getting-started/installation',
            },
            {
              label: 'Configuration',
              to: '/configuration/endpoint-setup',
            },
          ],
        },
        {
          title: 'Community',
          items: [
            {
              label: 'GitHub Issues',
              href: 'https://github.com/bw-design-group/prometheus-exporter/issues',
            },
          ],
        },
        {
          title: 'More',
          items: [
            {
              label: 'GitHub',
              href: 'https://github.com/bw-design-group/prometheus-exporter',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} Barry-Wehmiller Design Group. Built with Docusaurus.`,
    },
    prism: {
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
      additionalLanguages: ['java', 'json', 'bash', 'python', 'yaml'],
    },
    mermaid: {
      theme: { light: 'neutral', dark: 'dark' },
    },
  },
};

export default config;