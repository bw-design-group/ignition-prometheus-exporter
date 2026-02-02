# Scrape Configuration

Configure Prometheus to collect metrics from Ignition gateways using the Prometheus Exporter Module endpoint.

## Basic Scrape Configuration

### Minimal Configuration

Add this configuration to your `prometheus.yml` file:

```yaml
scrape_configs:
  - job_name: 'ignition-gateway'
    static_configs:
      - targets: ['your-gateway:8088']
    metrics_path: '/system/metrics'
    scrape_interval: 30s
    scrape_timeout: 10s
```

### Complete Configuration Example

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'ignition-gateways'
    static_configs:
      - targets: 
        - 'gateway-prod:8088'
        - 'gateway-dev:8088'
        - 'gateway-test:8088'
    metrics_path: '/system/metrics'
    scrape_interval: 30s
    scrape_timeout: 10s
    honor_labels: false
    honor_timestamps: true
    
    # Add custom labels to all metrics from this job
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: '127.0.0.1:9090'  # Prometheus server address
    
    # Filter and modify metrics after scraping
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'jvm_(.*)'
        target_label: __name__
        replacement: 'ignition_jvm_${1}'
```

## Multi-Gateway Configurations

### Static Configuration

For environments with fixed gateway addresses:

```yaml
scrape_configs:
  - job_name: 'ignition-production'
    static_configs:
      - targets: 
        - 'gateway-primary.company.com:8088'
        - 'gateway-backup.company.com:8088'
        labels:
          environment: 'production'
          region: 'us-east-1'
    
  - job_name: 'ignition-development'
    static_configs:
      - targets:
        - 'gateway-dev1.company.com:8088'
        - 'gateway-dev2.company.com:8088'
        labels:
          environment: 'development'
          region: 'us-west-2'
    
    # Different intervals for different environments
    scrape_interval: 15s  # More frequent for development
```

### Service Discovery

#### DNS-Based Discovery

```yaml
scrape_configs:
  - job_name: 'ignition-dns'
    dns_sd_configs:
      - names:
        - 'ignition-gateways.company.internal'
        type: 'A'
        port: 8088
    metrics_path: '/system/metrics'
    relabel_configs:
      - source_labels: [__meta_dns_name]
        target_label: gateway_name
```

#### File-Based Discovery

Create a targets file `/etc/prometheus/ignition-targets.json`:

```json
[
  {
    "targets": ["gateway-1:8088", "gateway-2:8088"],
    "labels": {
      "environment": "production",
      "site": "factory-a"
    }
  },
  {
    "targets": ["gateway-dev:8088"],
    "labels": {
      "environment": "development", 
      "site": "office"
    }
  }
]
```

Prometheus configuration:

```yaml
scrape_configs:
  - job_name: 'ignition-file-sd'
    file_sd_configs:
      - files:
        - '/etc/prometheus/ignition-targets.json'
        refresh_interval: 5m
    metrics_path: '/system/metrics'
```

## SSL/TLS Configuration

### HTTPS Endpoints

For gateways with SSL enabled:

```yaml
scrape_configs:
  - job_name: 'ignition-https'
    static_configs:
      - targets: ['gateway:8043']  # HTTPS port
    scheme: https
    metrics_path: '/system/metrics'
    
    tls_config:
      # Skip certificate verification (for self-signed certs)
      insecure_skip_verify: true
      
      # Or provide CA certificate
      # ca_file: '/etc/ssl/certs/company-ca.crt'
      
      # Client certificate authentication (if required)
      # cert_file: '/etc/ssl/certs/prometheus.crt'
      # key_file: '/etc/ssl/private/prometheus.key'
```

### Mixed HTTP/HTTPS Environment

```yaml
scrape_configs:
  # HTTP gateways (development)
  - job_name: 'ignition-http'
    static_configs:
      - targets: ['gateway-dev:8088']
    scheme: http
    metrics_path: '/system/metrics'
    
  # HTTPS gateways (production)  
  - job_name: 'ignition-https'
    static_configs:
      - targets: ['gateway-prod:8043']
    scheme: https
    metrics_path: '/system/metrics'
    tls_config:
      insecure_skip_verify: true
```

## Authentication Configuration

### Basic Authentication

If using a reverse proxy with basic auth:

```yaml
scrape_configs:
  - job_name: 'ignition-auth'
    static_configs:
      - targets: ['proxy.company.com:9090']
    metrics_path: '/metrics'  # Proxied path
    basic_auth:
      username: 'prometheus'
      password: 'secret123'
      # Or use password_file: '/etc/prometheus/password'
```

### Token-Based Authentication

For token-based authentication:

```yaml
scrape_configs:
  - job_name: 'ignition-token'
    static_configs:
      - targets: ['gateway:8088']
    metrics_path: '/system/metrics'
    authorization:
      type: Bearer
      credentials: 'your-auth-token'
      # Or use credentials_file: '/etc/prometheus/token'
```

## Advanced Scrape Options

### Custom Headers

Add custom HTTP headers to scrape requests:

```yaml
scrape_configs:
  - job_name: 'ignition-headers'
    static_configs:
      - targets: ['gateway:8088']
    metrics_path: '/system/metrics'
    params:
      'format': ['prometheus']
    headers:
      'X-Prometheus-Scrape': 'true'
      'Accept-Encoding': 'gzip'
```

### Proxy Configuration

Scrape through HTTP proxy:

```yaml
scrape_configs:
  - job_name: 'ignition-proxy'
    static_configs:
      - targets: ['gateway-remote:8088']
    metrics_path: '/system/metrics'
    proxy_url: 'http://proxy.company.com:8080'
```

## Metric Relabeling

### Label Management

Add consistent labeling across all Ignition metrics:

```yaml
scrape_configs:
  - job_name: 'ignition-labeled'
    static_configs:
      - targets: ['gateway:8088']
    metrics_path: '/system/metrics'
    
    metric_relabel_configs:
      # Add instance label with friendly name
      - target_label: instance_name
        replacement: 'production-gateway'
      
      # Add application label
      - target_label: application
        replacement: 'ignition'
        
      # Normalize JVM metric names
      - source_labels: [__name__]
        regex: 'jvm_(.*)'
        target_label: __name__
        replacement: 'ignition_jvm_${1}'
        
      # Add environment from hostname
      - source_labels: [instance]
        regex: 'gateway-(.+)\..*'
        target_label: environment
        replacement: '${1}'
```

### Metric Filtering

Filter specific metrics during scraping:

```yaml
scrape_configs:
  - job_name: 'ignition-filtered'
    static_configs:
      - targets: ['gateway:8088']
    metrics_path: '/system/metrics'
    
    metric_relabel_configs:
      # Keep only essential metrics
      - source_labels: [__name__]
        regex: 'jvm_(memory_bytes_used|memory_bytes_max|threads_current|gc_collection_seconds_total)'
        action: keep
        
      # Drop noisy histogram buckets
      - source_labels: [__name__]
        regex: '.*_bucket$'
        action: drop
        
      # Keep only heap memory metrics
      - source_labels: [area]
        regex: 'heap'
        action: keep
```

## High Availability Configuration

### Federation Setup

Configure federation for multi-tier Prometheus architecture:

```yaml
# Local Prometheus (near Ignition gateways)
scrape_configs:
  - job_name: 'local-ignition'
    static_configs:
      - targets: ['gateway-1:8088', 'gateway-2:8088']
    metrics_path: '/system/metrics'
    scrape_interval: 15s

# Central Prometheus (datacenter)
scrape_configs:
  - job_name: 'federate-ignition'
    scrape_interval: 30s
    honor_labels: true
    metrics_path: '/federate'
    params:
      'match[]':
        - '{job="local-ignition"}'
        - '{__name__=~"ignition_.*"}'
    static_configs:
      - targets: ['prometheus-local-1:9090', 'prometheus-local-2:9090']
```

### Load Balancing

Distribute scrape load across multiple gateways:

```yaml
scrape_configs:
  # Primary scraping
  - job_name: 'ignition-primary'
    static_configs:
      - targets: ['gateway-1:8088']
    metrics_path: '/system/metrics'
    scrape_interval: 30s
    
  # Backup scraping (different metrics or reduced frequency)
  - job_name: 'ignition-backup'  
    static_configs:
      - targets: ['gateway-2:8088']
    metrics_path: '/system/metrics' 
    scrape_interval: 60s
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'jvm_memory.*'
        action: keep  # Only collect memory metrics from backup
```

## Validation and Testing

### Configuration Validation

Test Prometheus configuration before deployment:

```bash
# Validate prometheus.yml syntax
promtool check config /etc/prometheus/prometheus.yml

# Test scrape target connectivity
promtool query instant 'up{job="ignition-gateway"}'

# Check target discovery
curl http://localhost:9090/api/v1/targets
```

### Scrape Testing

Verify scrape configuration is working:

```bash
# Check if targets are up
curl -s http://prometheus:9090/api/v1/targets | jq '.data.activeTargets[] | select(.job=="ignition-gateway") | .health'

# Test metric collection
curl -s http://prometheus:9090/api/v1/query?query=up{job="ignition-gateway"}

# Verify metric ingestion
curl -s http://prometheus:9090/api/v1/query?query=jvm_memory_bytes_used
```

### Performance Testing

Monitor scrape performance:

```bash
# Check scrape duration
curl -s http://prometheus:9090/api/v1/query?query=scrape_duration_seconds{job="ignition-gateway"}

# Monitor sample ingestion rate  
curl -s http://prometheus:9090/api/v1/query?query=rate(prometheus_tsdb_symbol_table_size_bytes[5m])

# Check for scrape errors
curl -s http://prometheus:9090/api/v1/query?query=scrape_samples_scraped{job="ignition-gateway"}
```

## Troubleshooting Scrape Issues

### Common Problems

**Target Down**

```bash
# Check network connectivity
telnet gateway-host 8088

# Verify endpoint responds
curl -I http://gateway-host:8088/system/metrics

# Check Prometheus logs
docker logs prometheus | grep "ignition-gateway"
```

**SSL/TLS Errors**

```bash
# Test SSL connectivity
openssl s_client -connect gateway-host:8043

# Check certificate validity
curl -k https://gateway-host:8043/system/metrics

# Verify CA configuration
curl --cacert /path/to/ca.crt https://gateway-host:8043/system/metrics
```

**Authentication Failures**

```bash
# Test basic auth manually
curl -u username:password http://proxy:9090/metrics

# Check authorization header
curl -H "Authorization: Bearer token123" http://gateway:8088/system/metrics
```

### Diagnostic Queries

Monitor scrape health with these Prometheus queries:

```promql
# Targets that are down
up == 0

# High scrape durations
scrape_duration_seconds > 5

# Failed scrapes
rate(scrape_samples_scraped[5m]) == 0

# Sample ingestion rate
rate(prometheus_tsdb_samples_appended_total[5m])
```

## Next Steps

After configuring scraping:

1. **[Set up alerting rules](alerting-rules)** for gateway health monitoring
2. **[Create Grafana dashboards](grafana-dashboards)** for metric visualization  
3. **[Configure performance monitoring](../troubleshooting/performance)** for optimization