# Alerting Rules

Configure Prometheus alerting rules to proactively monitor Ignition gateway health and performance using metrics from the Prometheus Exporter Module.

## Alert Rule Structure

### Basic Alert Configuration

Create an alert rules file `/etc/prometheus/rules/ignition-alerts.yml`:

```yaml
groups:
  - name: ignition.gateway.health
    interval: 30s
    rules:
      - alert: IgnitionGatewayDown
        expr: up{job="ignition-gateway"} == 0
        for: 1m
        labels:
          severity: critical
          service: ignition
        annotations:
          summary: "Ignition Gateway is down"
          description: "Gateway {{ $labels.instance }} has been down for more than 1 minute"
          runbook_url: "https://docs.company.com/runbooks/ignition-gateway-down"
```

### Complete Rules File Example

```yaml
groups:
  - name: ignition.gateway.health
    interval: 30s
    rules:
      # Gateway availability
      - alert: IgnitionGatewayDown
        expr: up{job="ignition-gateway"} == 0
        for: 1m
        labels:
          severity: critical
          service: ignition
          team: infrastructure
        annotations:
          summary: "Ignition Gateway {{ $labels.instance }} is down"
          description: "Gateway has been unreachable for more than 1 minute. Check network connectivity and gateway service status."
          dashboard: "https://grafana.company.com/d/ignition-overview"
      
      # Scrape failures
      - alert: IgnitionScrapeFailure  
        expr: up{job="ignition-gateway"} == 1 and (scrape_samples_scraped{job="ignition-gateway"} == 0 or increase(scrape_samples_scraped{job="ignition-gateway"}[5m]) == 0)
        for: 2m
        labels:
          severity: warning
          service: ignition
        annotations:
          summary: "Ignition metrics scraping failing"
          description: "Unable to scrape metrics from {{ $labels.instance }} for {{ $value }} minutes"

  - name: ignition.jvm.memory
    interval: 30s  
    rules:
      # High memory usage
      - alert: IgnitionHighMemoryUsage
        expr: (jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) * 100 > 85
        for: 5m
        labels:
          severity: warning
          service: ignition
        annotations:
          summary: "Ignition Gateway high memory usage"
          description: "Gateway {{ $labels.instance }} heap memory usage is {{ $value | humanizePercentage }} (>85%)"
          
      # Critical memory usage
      - alert: IgnitionCriticalMemoryUsage
        expr: (jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) * 100 > 95
        for: 2m
        labels:
          severity: critical
          service: ignition
        annotations:
          summary: "Ignition Gateway critical memory usage"
          description: "Gateway {{ $labels.instance }} heap memory usage is {{ $value | humanizePercentage }} (>95%)"
          action_required: "Immediate intervention required - gateway may crash"

  - name: ignition.jvm.gc
    interval: 30s
    rules:
      # High GC frequency
      - alert: IgnitionHighGCFrequency
        expr: rate(jvm_gc_collection_seconds_count[5m]) > 0.2
        for: 10m
        labels:
          severity: warning
          service: ignition
        annotations:
          summary: "Ignition Gateway high GC frequency" 
          description: "Gateway {{ $labels.instance }} GC frequency is {{ $value | humanize }} collections/sec (>0.2/sec)"
          
      # Long GC pauses
      - alert: IgnitionLongGCPauses
        expr: rate(jvm_gc_collection_seconds[5m]) > 0.1
        for: 5m
        labels:
          severity: warning
          service: ignition
        annotations:
          summary: "Ignition Gateway long GC pauses"
          description: "Gateway {{ $labels.instance }} spending {{ $value | humanizePercentage }} of time in GC (>10%)"

  - name: ignition.jvm.threads
    interval: 30s
    rules:
      # High thread count
      - alert: IgnitionHighThreadCount
        expr: jvm_threads_current > 500
        for: 10m
        labels:
          severity: warning
          service: ignition
        annotations:
          summary: "Ignition Gateway high thread count"
          description: "Gateway {{ $labels.instance }} has {{ $value }} threads (>500)"
          
      # Thread leak detection
      - alert: IgnitionThreadLeak
        expr: increase(jvm_threads_current[30m]) > 100
        for: 15m
        labels:
          severity: critical
          service: ignition
        annotations:
          summary: "Potential thread leak in Ignition Gateway"
          description: "Thread count increased by {{ $value }} in 30 minutes on {{ $labels.instance }}"
```

## Alert Severity Levels

### Severity Classification

**Critical Alerts** - Immediate action required
- Gateway completely down
- Memory usage >95%
- Rapid thread growth indicating leaks

**Warning Alerts** - Action required within business hours  
- High memory usage (>85%)
- High GC frequency
- Performance degradation

**Info Alerts** - Monitoring and trend analysis
- Module restarts
- Configuration changes
- Performance variations

### Example Severity Configuration

```yaml
groups:
  - name: ignition.severity.critical
    rules:
      - alert: IgnitionGatewayDown
        expr: up{job="ignition-gateway"} == 0
        for: 30s
        labels:
          severity: critical
          escalation: immediate
          oncall: true
        annotations:
          summary: "CRITICAL: Gateway Down"
          
      - alert: IgnitionOutOfMemory
        expr: (jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) > 0.98
        for: 1m
        labels:
          severity: critical
          escalation: immediate
          oncall: true

  - name: ignition.severity.warning
    rules:
      - alert: IgnitionMemoryPressure
        expr: (jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) > 0.80
        for: 10m
        labels:
          severity: warning
          escalation: business_hours
          team: platform
```

## Environment-Specific Rules

### Production Environment

```yaml
groups:
  - name: ignition.production
    rules:
      # Strict thresholds for production
      - alert: IgnitionProductionMemoryHigh
        expr: (jvm_memory_bytes_used{area="heap",environment="production"} / jvm_memory_bytes_max{area="heap",environment="production"}) > 0.75
        for: 5m
        labels:
          severity: warning
          environment: production
        annotations:
          summary: "Production Gateway memory usage high"
          
      # Monitor production uptime closely
      - alert: IgnitionProductionDowntime
        expr: up{job="ignition-gateway",environment="production"} == 0  
        for: 30s
        labels:
          severity: critical
          environment: production
          escalation: immediate
```

### Development Environment

```yaml
groups:
  - name: ignition.development
    rules:
      # Relaxed thresholds for development
      - alert: IgnitionDevelopmentMemoryHigh
        expr: (jvm_memory_bytes_used{area="heap",environment="development"} / jvm_memory_bytes_max{area="heap",environment="development"}) > 0.90
        for: 15m
        labels:
          severity: info
          environment: development
        annotations:
          summary: "Development Gateway memory usage high"
          description: "Consider restarting development gateway during next maintenance window"
```

## Performance-Based Alerts

### Response Time Monitoring

```yaml
groups:
  - name: ignition.performance
    rules:
      # Slow metric scraping
      - alert: IgnitionSlowScrapes
        expr: scrape_duration_seconds{job="ignition-gateway"} > 5
        for: 5m
        labels:
          severity: warning
          service: ignition
        annotations:
          summary: "Ignition metrics scraping slowly"
          description: "Scraping {{ $labels.instance }} taking {{ $value }}s (>5s)"
          
      # High CPU usage correlation
      - alert: IgnitionHighCPUWithMemoryPressure
        expr: |
          (
            rate(process_cpu_seconds_total[5m]) > 0.8
            and
            (jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) > 0.8
          )
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Ignition Gateway under resource pressure"
          description: "{{ $labels.instance }} showing high CPU and memory usage simultaneously"
```

### Trend-Based Alerts

```yaml
groups:
  - name: ignition.trends
    rules:
      # Memory growth trend
      - alert: IgnitionMemoryGrowthTrend
        expr: |
          (
            predict_linear(jvm_memory_bytes_used{area="heap"}[1h], 4*3600) 
            / 
            jvm_memory_bytes_max{area="heap"}
          ) > 0.95
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "Ignition Gateway memory growth trend concerning"
          description: "Memory usage trending toward exhaustion in ~4 hours on {{ $labels.instance }}"
          
      # Thread count growth
      - alert: IgnitionThreadGrowthTrend
        expr: predict_linear(jvm_threads_current[2h], 6*3600) > 1000
        for: 1h
        labels:
          severity: warning
        annotations:
          summary: "Thread count growth trend detected"
          description: "Thread count trending toward 1000+ in ~6 hours on {{ $labels.instance }}"
```

## Custom Gateway Metrics Alerts

### Application-Specific Metrics

For custom metrics registered via scripting:

```yaml
groups:
  - name: ignition.custom
    rules:
      # Custom uptime tracking
      - alert: IgnitionModuleRestart
        expr: resets(ignition_custom_uptime_seconds[1h]) > 0
        labels:
          severity: info
        annotations:
          summary: "Ignition module restart detected"
          description: "Custom metrics indicate module restart on {{ $labels.instance }}"
          
      # Perspective session monitoring  
      - alert: IgnitionHighPerspectiveSessions
        expr: ignition_active_perspective_sessions > 100
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: "High number of Perspective sessions"
          description: "{{ $value }} active Perspective sessions on {{ $labels.instance }} (>100)"
```

## Alert Manager Configuration

### Route Configuration

Configure AlertManager to route Ignition alerts appropriately:

```yaml
# alertmanager.yml
route:
  group_by: ['alertname', 'service']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  receiver: 'default'
  routes:
    # Critical Ignition alerts
    - match:
        service: ignition
        severity: critical
      receiver: 'ignition-oncall'
      group_wait: 10s
      repeat_interval: 5m
      
    # Warning Ignition alerts  
    - match:
        service: ignition
        severity: warning
      receiver: 'ignition-team'
      group_interval: 15m
      repeat_interval: 4h

receivers:
  - name: 'ignition-oncall'
    pagerduty_configs:
      - service_key: 'your-pagerduty-key'
        description: "{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}"
        
  - name: 'ignition-team'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/xxx/yyy/zzz'
        channel: '#ignition-alerts'
        title: 'Ignition Gateway Alert'
        text: "{{ range .Alerts }}{{ .Annotations.description }}{{ end }}"
```

### Inhibition Rules

Prevent alert spam with inhibition rules:

```yaml
# alertmanager.yml
inhibit_rules:
  # Don't alert on high memory if gateway is already down
  - source_match:
      alertname: IgnitionGatewayDown
    target_match_re:
      alertname: Ignition.*Memory.*
    equal: ['instance']
    
  # Don't alert on performance issues if memory is critical  
  - source_match:
      severity: critical
      service: ignition
    target_match:
      severity: warning
      service: ignition
    equal: ['instance']
```

## Testing and Validation

### Rule Validation

Test alert rules before deployment:

```bash
# Validate syntax
promtool check rules /etc/prometheus/rules/ignition-alerts.yml

# Test rule evaluation
promtool query instant 'up{job="ignition-gateway"} == 0'

# Check rule evaluation in Prometheus UI
# Navigate to Status > Rules in Prometheus web interface
```

### Alert Testing

Trigger test alerts to verify configuration:

```bash
# Simulate gateway down (stop Ignition service temporarily)
sudo systemctl stop ignition

# Check alert fires in Prometheus
curl -s http://prometheus:9090/api/v1/alerts | jq '.data.alerts[] | select(.labels.alertname=="IgnitionGatewayDown")'

# Verify AlertManager receives alert
curl -s http://alertmanager:9093/api/v1/alerts | jq '.data[] | select(.labels.alertname=="IgnitionGatewayDown")'
```

### Load Testing Alerts

Test performance-based alerts:

```bash
# Simulate high memory usage (if possible in test environment)
# Use stress testing tools or memory-intensive operations

# Monitor alert evaluation
watch -n 10 "curl -s http://prometheus:9090/api/v1/alerts | jq '.data.alerts[].labels.alertname' | sort | uniq -c"
```

## Maintenance and Updates

### Rule Lifecycle Management

```bash
# Reload rules without restarting Prometheus
curl -X POST http://prometheus:9090/-/reload

# Check rule evaluation times
curl -s http://prometheus:9090/api/v1/rules | jq '.data.groups[].rules[].evaluationTime'

# Monitor rule evaluation performance
curl -s http://prometheus:9090/api/v1/query?query=prometheus_rule_evaluation_duration_seconds
```

### Alert Tuning

Monitor alert effectiveness and adjust thresholds:

```promql
# Alert firing frequency
rate(prometheus_notifications_total[1h])

# False positive analysis  
changes(up{job="ignition-gateway"}[1h]) > bool 5

# Alert resolution time
histogram_quantile(0.9, rate(alertmanager_alert_duration_seconds_bucket[1h]))
```

## Next Steps

After configuring alerting:

1. **[Set up Grafana dashboards](grafana-dashboards)** for visual monitoring
2. **[Configure notification channels](../examples/alerting-setup)** for alert delivery
3. **[Monitor alert performance](../troubleshooting/debugging)** and tune thresholds