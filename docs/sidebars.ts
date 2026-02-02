import type { SidebarsConfig } from '@docusaurus/plugin-content-docs';

const sidebars: SidebarsConfig = {
  docsSidebar: [
    "index",
    {
      type: 'category',
      label: 'Introduction',
      collapsed: false,
      items: [
        'introduction/overview',
        'introduction/features',
        'introduction/architecture',
      ],
    },
    {
      type: 'category',
      label: 'Getting Started',
      collapsed: false,
      items: [
        'getting-started/installation',
        'getting-started/configuration',
        'getting-started/first-metrics',
      ],
    },
    {
      type: 'category',
      label: 'Configuration',
      items: [
        'configuration/endpoint-setup',
        'configuration/metrics-filtering',
        'configuration/performance-tuning',
      ],
    },
    {
      type: 'category',
      label: 'Prometheus Integration',
      items: [
        'prometheus-integration/scrape-configuration',
        'prometheus-integration/alerting-rules',
        'prometheus-integration/grafana-dashboards',
      ],
    },
    {
      type: 'category',
      label: 'Metrics Reference',
      items: [
        'metrics/dropwizard-metrics',
        'metrics/metric-types',
        'metrics/custom-metrics',
      ],
    },
    {
      type: 'category',
      label: 'Examples',
      items: [
        'examples/basic-monitoring',
        'examples/dashboard-creation',
        'examples/alerting-setup',
      ],
    },
    {
      type: 'category',
      label: 'Troubleshooting',
      items: [
        'troubleshooting/common-issues',
        'troubleshooting/debugging',
        'troubleshooting/performance',
      ],
    },
  ],
};

export default sidebars;