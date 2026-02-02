# Overview

The Prometheus Exporter Module transforms Ignition gateways into observable systems by exposing internal metrics in Prometheus format. This gateway-scoped module integrates directly with Ignition's metric collection infrastructure to provide real-time operational insights.

## What Problem Does This Solve?

Modern industrial automation requires comprehensive monitoring and observability. While Ignition provides excellent operational capabilities, gaining visibility into gateway health, performance trends, and resource utilization traditionally required custom solutions or third-party tools.

The Prometheus Exporter Module bridges this gap by:

- **Standardizing Metrics Export**: Uses the widely-adopted Prometheus format
- **Leveraging Existing Infrastructure**: Works with Ignition's built-in Dropwizard metrics
- **Enabling Modern Tooling**: Compatible with Prometheus, Grafana, and alerting systems
- **Providing Historical Context**: Supports time-series analysis and trend identification

## How It Works

The module operates as a gateway-scoped plugin that registers a servlet endpoint at `/system/metrics`. This endpoint serves metrics in Prometheus exposition format, allowing external monitoring systems to scrape and store the data.

```
Ignition Gateway → Dropwizard Metrics → Prometheus Format → /system/metrics → Monitoring Stack
```

## Target Audience

This module is designed for:

- **System Administrators** managing Ignition infrastructure
- **DevOps Engineers** implementing monitoring solutions  
- **Plant Engineers** requiring operational visibility
- **IT Professionals** supporting industrial automation systems

## Prerequisites

- Ignition Gateway version 8.1.44 or later
- Network access for Prometheus server to reach gateway
- Basic understanding of Prometheus concepts (helpful but not required)

## Scope and Limitations

**What's Included:**
- JVM metrics (memory, threads, garbage collection)
- System metrics (CPU, disk, network when available)
- Ignition-specific metrics (from internal MetricRegistry)
- Servlet endpoint configuration

**What's Not Included:**
- Custom application metrics (requires separate implementation)
- Tag database metrics (use Ignition's built-in tools)
- Historical data storage (handled by Prometheus server)
- Alerting rules (configured in Prometheus/Alertmanager)