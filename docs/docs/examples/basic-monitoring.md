# Basic Monitoring Setup

A complete walkthrough for setting up basic Ignition gateway monitoring using the Prometheus Exporter Module.

## Complete Setup Example

This example demonstrates a full monitoring stack deployment for a single Ignition gateway.

### Infrastructure Setup

**Docker Compose Configuration**

Create `docker-compose.yml` for the monitoring stack:

```yaml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
      - ./alerts:/etc/prometheus/alerts
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=30d'
      - '--web.enable-lifecycle'
      - '--alertmanager.url=http://alertmanager:9093'
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana_data:/var/lib/grafana
      - ./grafana/provisioning:/etc/grafana/provisioning
      - ./grafana/dashboards:/var/lib/grafana/dashboards
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin123
      - GF_USERS_ALLOW_SIGN_UP=false
    networks:
      - monitoring

  alertmanager:
    image: prom/alertmanager:latest
    container_name: alertmanager
    ports:
      - "9093:9093"
    volumes:
      - ./alertmanager.yml:/etc/alertmanager/alertmanager.yml
      - alertmanager_data:/alertmanager
    networks:
      - monitoring

volumes:
  prometheus_data:
  grafana_data:
  alertmanager_data:

networks:
  monitoring:
    driver: bridge
```

### Prometheus Configuration

**Basic `prometheus.yml`**

```yaml
global:
  scrape_interval: 30s
  evaluation_interval: 30s

rule_files:
  - "alerts/*.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']

scrape_configs:
  # Ignition Gateway monitoring
  - job_name: 'ignition-gateway'
    static_configs:
      - targets: ['host.docker.internal:8088']  # Adjust for your gateway
    metrics_path: '/system/metrics'
    scrape_interval: 30s
    scrape_timeout: 10s
    
    # Add custom labels for identification
    relabel_configs:
      - target_label: 'environment'
        replacement: 'production'
      - target_label: 'site'
        replacement: 'factory-1'
    
    # Clean up metric names for consistency
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'jvm_(.*)'
        target_label: __name__
        replacement: 'ignition_jvm_${1}'

  # Monitor Prometheus itself
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  # Monitor other services
  - job_name: 'node-exporter'
    static_configs:
      - targets: ['host.docker.internal:9100']  # If running node_exporter
```

### Alert Rules Configuration

Create `alerts/ignition-basic.yml`:

```yaml
groups:
  - name: ignition.basic.health
    rules:
      - alert: IgnitionGatewayDown
        expr: up{job="ignition-gateway"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Ignition Gateway is down"
          description: "Gateway {{ $labels.instance }} has been down for more than 1 minute"

      - alert: IgnitionHighMemoryUsage
        expr: (ignition_jvm_memory_bytes_used{area="heap"} / ignition_jvm_memory_bytes_max{area="heap"}) * 100 > 85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High memory usage on Ignition Gateway"
          description: "Memory usage is {{ $value | humanizePercentage }} on {{ $labels.instance }}"

      - alert: IgnitionCriticalMemoryUsage
        expr: (ignition_jvm_memory_bytes_used{area="heap"} / ignition_jvm_memory_bytes_max{area="heap"}) * 100 > 95
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Critical memory usage on Ignition Gateway"
          description: "Memory usage is {{ $value | humanizePercentage }} on {{ $labels.instance }}"

      - alert: IgnitionHighGCActivity
        expr: rate(ignition_jvm_gc_collection_seconds_count[5m]) > 0.1
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High garbage collection activity"
          description: "GC rate is {{ $value | humanize }} collections/sec on {{ $labels.instance }}"
```

### AlertManager Configuration

Create `alertmanager.yml`:

```yaml
global:
  smtp_smarthost: 'mail.company.com:587'
  smtp_from: 'alertmanager@company.com'

route:
  group_by: ['alertname', 'instance']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 24h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'critical-alerts'
      repeat_interval: 5m
    - match:
        severity: warning
      receiver: 'warning-alerts'

receivers:
  - name: 'default'
    email_configs:
      - to: 'admin@company.com'
        subject: 'Ignition Alert: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          Instance: {{ .Labels.instance }}
          {{ end }}

  - name: 'critical-alerts'
    email_configs:
      - to: 'oncall@company.com'
        subject: 'CRITICAL: {{ .GroupLabels.alertname }}'
        
  - name: 'warning-alerts'
    email_configs:
      - to: 'team@company.com'
        subject: 'WARNING: {{ .GroupLabels.alertname }}'
```

## Basic Dashboard Setup

### Grafana Data Source

Create `grafana/provisioning/datasources/prometheus.yml`:

```yaml
apiVersion: 1

datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
    editable: false
    jsonData:
      timeInterval: "30s"
      queryTimeout: "60s"
      httpMethod: "POST"
```

### Basic Dashboard

Create `grafana/provisioning/dashboards/dashboard.yml`:

```yaml
apiVersion: 1

providers:
  - name: 'ignition-dashboards'
    type: file
    disableDeletion: true
    updateIntervalSeconds: 30
    options:
      path: /var/lib/grafana/dashboards
```

Create basic dashboard `grafana/dashboards/ignition-overview.json`:

```json
{
  "dashboard": {
    "id": null,
    "title": "Ignition Gateway Overview",
    "tags": ["ignition", "gateway"],
    "timezone": "browser",
    "panels": [
      {
        "id": 1,
        "title": "Gateway Status",
        "type": "stat",
        "targets": [{
          "expr": "up{job=\"ignition-gateway\"}",
          "legendFormat": "Status"
        }],
        "fieldConfig": {
          "defaults": {
            "mappings": [
              {"options": {"0": {"text": "Down", "color": "red"}}, 
               "options": {"1": {"text": "Up", "color": "green"}}
            ]
          }
        },
        "gridPos": {"h": 4, "w": 6, "x": 0, "y": 0}
      },
      {
        "id": 2,
        "title": "Memory Usage",
        "type": "timeseries",
        "targets": [
          {
            "expr": "ignition_jvm_memory_bytes_used{area=\"heap\"} / 1024 / 1024",
            "legendFormat": "Heap Used (MB)"
          },
          {
            "expr": "ignition_jvm_memory_bytes_max{area=\"heap\"} / 1024 / 1024",
            "legendFormat": "Heap Max (MB)"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "decbytes",
            "custom": {"drawStyle": "line"}
          }
        },
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 4}
      }
    ],
    "refresh": "30s",
    "time": {"from": "now-1h", "to": "now"}
  }
}
```

## Deployment Walkthrough

### Step 1: Prepare Configuration Files

Create directory structure:
```bash
mkdir ignition-monitoring
cd ignition-monitoring
mkdir grafana/provisioning/datasources
mkdir grafana/provisioning/dashboards  
mkdir grafana/dashboards
mkdir alerts
```

Save the configuration files above in their respective locations.

### Step 2: Deploy the Stack

Start the monitoring stack:
```bash
# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps

# Check logs for any issues
docker-compose logs prometheus
docker-compose logs grafana
```

### Step 3: Verify Ignition Integration

Check that Prometheus can scrape Ignition:
```bash
# Check target status in Prometheus
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.job=="ignition-gateway") | .health'

# Should return: "up"

# Query for Ignition metrics
curl -s "http://localhost:9090/api/v1/query?query=up{job=\"ignition-gateway\"}" | jq '.data.result[0].value[1]'

# Should return: "1"
```

### Step 4: Access Dashboards

1. **Prometheus UI**: http://localhost:9090
   - Check Status > Targets to verify Ignition gateway is being scraped
   - Try queries like `ignition_jvm_memory_bytes_used{area="heap"}`

2. **Grafana UI**: http://localhost:3000
   - Login: admin / admin123
   - Navigate to dashboards to view Ignition overview

3. **AlertManager UI**: http://localhost:9093
   - View active alerts and alert history

## Basic Monitoring Queries

### Essential Prometheus Queries

**Gateway Health Check**
```promql
# Gateway availability
up{job="ignition-gateway"}

# Service uptime percentage over 24 hours
avg_over_time(up{job="ignition-gateway"}[24h]) * 100
```

**Memory Monitoring**
```promql
# Current memory usage percentage
(ignition_jvm_memory_bytes_used{area="heap"} / ignition_jvm_memory_bytes_max{area="heap"}) * 100

# Available heap memory in MB
(ignition_jvm_memory_bytes_max{area="heap"} - ignition_jvm_memory_bytes_used{area="heap"}) / 1024 / 1024

# Memory usage trend (growth rate per hour)
predict_linear(ignition_jvm_memory_bytes_used{area="heap"}[1h], 3600)
```

**Performance Metrics**
```promql
# CPU usage percentage (if process metrics available)
rate(process_cpu_seconds_total{job="ignition-gateway"}[5m]) * 100

# Garbage collection frequency
rate(ignition_jvm_gc_collection_seconds_count[5m])

# Thread count
ignition_jvm_threads_current
```

**System Health Indicators**
```promql
# Classes loaded (indicates system activity)
ignition_jvm_classes_loaded

# File descriptors usage (Linux/macOS)
process_open_fds / process_max_fds * 100

# Response time for metrics collection
scrape_duration_seconds{job="ignition-gateway"}
```

## Testing and Validation

### Validate Alert Rules

Test that alerts trigger correctly:

```bash
# Stop Ignition temporarily to test gateway down alert
sudo systemctl stop ignition

# Wait 2 minutes, then check for alert
curl -s http://localhost:9090/api/v1/alerts | jq '.data.alerts[] | select(.labels.alertname=="IgnitionGatewayDown")'

# Restart Ignition
sudo systemctl start ignition
```

### Load Testing

Simulate high memory usage to test memory alerts:

```bash
# Create memory pressure on gateway (use cautiously in test environment)
# This is just an example - implement based on your system
```

### Dashboard Verification

Confirm dashboard displays real data:

1. Check that panels show current values
2. Verify time series charts update with new data  
3. Test dashboard template variables (if configured)
4. Validate alert annotations appear on charts

## Maintenance and Operations

### Regular Maintenance Tasks

**Weekly:**
- Review alert history and tune thresholds if needed
- Check dashboard accuracy and relevance
- Verify backup and retention policies

**Monthly:**  
- Update monitoring stack components
- Review and archive old alert data
- Assess monitoring coverage and gaps

**Quarterly:**
- Review alert effectiveness and false positive rates
- Evaluate new monitoring requirements
- Performance tune Prometheus and Grafana

### Backup Configuration

```bash
#!/bin/bash
# backup-monitoring-config.sh

DATE=$(date +%Y%m%d)
BACKUP_DIR="/backup/monitoring-$DATE"

mkdir -p $BACKUP_DIR

# Backup configuration files
cp prometheus.yml $BACKUP_DIR/
cp -r alerts $BACKUP_DIR/
cp alertmanager.yml $BACKUP_DIR/
cp -r grafana/provisioning $BACKUP_DIR/

# Export Grafana dashboards
docker exec grafana grafana-cli admin export-dashboard ignition-overview > $BACKUP_DIR/ignition-overview.json

echo "Monitoring configuration backed up to $BACKUP_DIR"
```

### Troubleshooting Common Issues

**Prometheus Can't Reach Ignition**
```bash
# Test connectivity from Prometheus container
docker exec prometheus wget -qO- http://host.docker.internal:8088/system/metrics

# Check firewall rules
sudo iptables -L | grep 8088

# Verify Ignition module status
curl -I http://ignition-gateway:8088/system/metrics
```

**Missing Metrics in Grafana**
```bash
# Verify data source connection
curl -s http://admin:admin123@localhost:3000/api/datasources/1 | jq '.url'

# Test query directly against Prometheus
curl -s 'http://localhost:9090/api/v1/query?query=up{job="ignition-gateway"}' | jq '.data.result'
```

**Alerts Not Firing**
```bash
# Check alert rule evaluation
curl -s http://localhost:9090/api/v1/rules | jq '.data.groups[].rules[] | select(.name=="IgnitionGatewayDown")'

# Verify AlertManager connectivity
curl -s http://localhost:9093/api/v1/status | jq '.data.configYAML'
```

## Next Steps

After basic monitoring is operational:

1. **[Create advanced dashboards](dashboard-creation)** with detailed visualizations
2. **[Set up comprehensive alerting](alerting-setup)** with escalation policies  
3. **[Implement custom metrics](../metrics/custom-metrics)** for business-specific monitoring
4. **[Configure high availability](../prometheus-integration/scrape-configuration)** monitoring for production