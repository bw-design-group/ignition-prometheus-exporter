# Alerting Setup

Complete guide for implementing robust alerting strategies using metrics from the Prometheus Exporter Module.

## Alerting Strategy Overview

### Alert Classification

**Critical Alerts (Immediate Response)**
- Gateway completely down
- Out of memory conditions
- Rapid resource exhaustion

**Warning Alerts (Business Hours Response)**
- High resource usage trending toward limits
- Performance degradation
- Configuration drift

**Info Alerts (Monitoring and Trends)**
- Capacity planning indicators
- Maintenance reminders
- Performance baselines

## Production Alert Rules

### Critical Infrastructure Alerts

```yaml
# alerts/critical-infrastructure.yml
groups:
  - name: ignition.critical
    interval: 15s
    rules:
      - alert: IgnitionGatewayDown
        expr: up{job="ignition-gateway"} == 0
        for: 30s
        labels:
          severity: critical
          escalation: immediate
          team: infrastructure
        annotations:
          summary: "Ignition Gateway is down"
          description: "Gateway {{ $labels.instance }} has been unreachable for more than 30 seconds. Check network connectivity and gateway service status."
          runbook_url: "https://docs.company.com/runbooks/gateway-down"
          dashboard_url: "https://grafana.company.com/d/ignition-overview?var-instance={{ $labels.instance }}"

      - alert: IgnitionOutOfMemory
        expr: (jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) > 0.98
        for: 1m
        labels:
          severity: critical
          escalation: immediate
        annotations:
          summary: "Ignition Gateway critically low on memory"
          description: "Gateway {{ $labels.instance }} memory usage is {{ $value | humanizePercentage }}. Gateway crash imminent."
          action_required: "Immediate restart may be required"

      - alert: IgnitionThreadExhaustion
        expr: jvm_threads_current > 1000
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Ignition Gateway thread exhaustion"
          description: "Gateway {{ $labels.instance }} has {{ $value }} threads (>1000). Possible thread leak."

      - alert: IgnitionDatabaseConnectionFailure
        expr: up{job="ignition-gateway"} == 1 and (ignition_database_connections{state="active"} == 0)
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Database connections exhausted"
          description: "No active database connections available on {{ $labels.instance }}"
```

### Performance Warning Alerts

```yaml
# alerts/performance-warnings.yml
groups:
  - name: ignition.performance
    interval: 30s
    rules:
      - alert: IgnitionHighMemoryUsage
        expr: (jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) > 0.85
        for: 10m
        labels:
          severity: warning
          team: operations
        annotations:
          summary: "High memory usage on Ignition Gateway"
          description: "Memory usage is {{ $value | humanizePercentage }} on {{ $labels.instance }} for over 10 minutes"
          
      - alert: IgnitionHighGCActivity
        expr: rate(jvm_gc_collection_seconds{gc=~".*Old.*"}[5m]) > 0.05
        for: 15m
        labels:
          severity: warning
        annotations:
          summary: "High old generation GC activity"
          description: "Old gen GC time is {{ $value | humanizePercentage }} on {{ $labels.instance }}"
          
      - alert: IgnitionThreadCountGrowth
        expr: increase(jvm_threads_current[1h]) > 50
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "Rapid thread count growth"
          description: "Thread count increased by {{ $value }} in the last hour on {{ $labels.instance }}"

      - alert: IgnitionSlowMetricCollection
        expr: scrape_duration_seconds{job="ignition-gateway"} > 5
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Slow metrics collection"
          description: "Metrics collection taking {{ $value }}s (>5s) on {{ $labels.instance }}"
```

### Business Logic Alerts

```yaml
# alerts/business-logic.yml
groups:
  - name: ignition.business
    interval: 60s
    rules:
      - alert: IgnitionProductionLineDown
        expr: ignition_production_oee_percent < 10
        for: 5m
        labels:
          severity: critical
          team: production
        annotations:
          summary: "Production line effectively stopped"
          description: "OEE is {{ $value }}% on {{ $labels.line }} - line may be down"

      - alert: IgnitionHighDefectRate
        expr: ignition_quality_defect_rate_percent > 5
        for: 15m
        labels:
          severity: warning
          team: quality
        annotations:
          summary: "High defect rate detected"
          description: "Defect rate is {{ $value }}% for {{ $labels.product }} (>5%)"

      - alert: IgnitionEnergyConsumptionAnomaly
        expr: |
          (
            ignition_power_consumption_kw > 
            (avg_over_time(ignition_power_consumption_kw[7d]) * 1.5)
          )
        for: 30m
        labels:
          severity: warning
          team: maintenance
        annotations:
          summary: "Abnormal energy consumption"
          description: "Power consumption is {{ $value }}kW, 50% above 7-day average"
```

## AlertManager Configuration

### Complete AlertManager Setup

```yaml
# alertmanager.yml
global:
  smtp_smarthost: 'smtp.company.com:587'
  smtp_from: 'alertmanager@company.com'
  smtp_auth_username: 'alertmanager@company.com'
  smtp_auth_password: 'smtp_password'

route:
  group_by: ['alertname', 'instance', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 12h
  receiver: 'default'
  
  routes:
    # Critical alerts - immediate escalation
    - match:
        severity: critical
      receiver: 'critical-alerts'
      group_wait: 10s
      repeat_interval: 5m
      routes:
        # Production critical alerts go to on-call
        - match:
            environment: production
          receiver: 'production-oncall'
        # Development critical alerts to team chat
        - match:
            environment: development
          receiver: 'dev-team-chat'
    
    # Warning alerts during business hours
    - match:
        severity: warning
      receiver: 'warning-alerts'
      active_time_intervals: ['business-hours']
      
    # Business logic alerts to specific teams
    - match:
        team: production
      receiver: 'production-team'
    - match:
        team: quality
      receiver: 'quality-team'
    - match:
        team: maintenance
      receiver: 'maintenance-team'

time_intervals:
  - name: business-hours
    time_intervals:
      - times:
        - start_time: '08:00'
          end_time: '18:00'
        weekdays: ['monday:friday']

receivers:
  - name: 'default'
    email_configs:
      - to: 'admin@company.com'
        subject: 'Ignition Alert: {{ .GroupLabels.alertname }}'
        
  - name: 'critical-alerts'
    email_configs:
      - to: 'oncall@company.com'
        subject: '🚨 CRITICAL: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          Instance: {{ .Labels.instance }}
          Severity: {{ .Labels.severity }}
          
          Dashboard: {{ .Annotations.dashboard_url }}
          Runbook: {{ .Annotations.runbook_url }}
          {{ end }}
    pagerduty_configs:
      - service_key: 'your-pagerduty-service-key'
        description: "{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}"

  - name: 'production-oncall'
    pagerduty_configs:
      - service_key: 'production-pagerduty-key'
        severity: 'critical'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/xxx/yyy/zzz'
        channel: '#production-alerts'
        title: '🚨 Production Critical Alert'
        text: |
          {{ range .Alerts }}
          *{{ .Annotations.summary }}*
          {{ .Annotations.description }}
          Instance: `{{ .Labels.instance }}`
          {{ end }}
        actions:
          - type: button
            text: 'View Dashboard'
            url: '{{ (index .Alerts 0).Annotations.dashboard_url }}'

  - name: 'warning-alerts'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/aaa/bbb/ccc'
        channel: '#ignition-monitoring'
        title: '⚠️ Ignition Warning'

inhibit_rules:
  # Don't alert on memory if gateway is down
  - source_match:
      alertname: IgnitionGatewayDown
    target_match_re:
      alertname: Ignition.*Memory.*
    equal: ['instance']
  
  # Don't alert on performance if out of memory
  - source_match:
      severity: critical
    target_match:
      severity: warning
    equal: ['instance']
```

## Advanced Alerting Patterns

### Multi-Condition Alerts

```yaml
groups:
  - name: ignition.complex
    rules:
      - alert: IgnitionSystemUnderStress
        expr: |
          (
            (jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) > 0.8
            and
            rate(process_cpu_seconds_total[5m]) > 0.8
            and
            jvm_threads_current > 300
          )
        for: 10m
        labels:
          severity: critical
        annotations:
          summary: "System under multiple resource stress"

      - alert: IgnitionCascadingFailure
        expr: |
          (
            count by (environment) (up{job="ignition-gateway",environment=~".+"} == 0)
            /
            count by (environment) (up{job="ignition-gateway",environment=~".+"})
          ) > 0.5
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Cascading gateway failures detected"
          description: "More than 50% of gateways down in {{ $labels.environment }}"
```

### Predictive Alerts

```yaml
groups:
  - name: ignition.predictive
    rules:
      - alert: IgnitionMemoryExhaustionPredicted
        expr: |
          predict_linear(jvm_memory_bytes_used{area="heap"}[2h], 6*3600) >
          jvm_memory_bytes_max{area="heap"} * 0.95
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "Memory exhaustion predicted within 6 hours"

      - alert: IgnitionDiskSpaceRunningOut
        expr: |
          predict_linear(node_filesystem_avail_bytes{mountpoint="/"}[1h], 24*3600) < 0
        for: 1h
        labels:
          severity: warning
        annotations:
          summary: "Disk space exhaustion predicted within 24 hours"
```

## Notification Channels

### Slack Integration

```yaml
# Enhanced Slack configuration
slack_configs:
  - api_url: 'https://hooks.slack.com/services/xxx/yyy/zzz'
    channel: '#ignition-alerts'
    username: 'Prometheus AlertManager'
    icon_emoji: ':warning:'
    title: |
      {{ if eq .Status "firing" }}🔥{{ else }}✅{{ end }} 
      {{ .GroupLabels.alertname }} - {{ .GroupLabels.instance }}
    text: |
      {{ range .Alerts }}
      *Summary:* {{ .Annotations.summary }}
      *Description:* {{ .Annotations.description }}
      *Severity:* {{ .Labels.severity | upper }}
      *Environment:* {{ .Labels.environment | default "unknown" }}
      {{ if .Annotations.dashboard_url }}
      <{{ .Annotations.dashboard_url }}|📊 View Dashboard>
      {{ end }}
      {{ if .Annotations.runbook_url }}
      <{{ .Annotations.runbook_url }}|📖 Runbook>
      {{ end }}
      ---
      {{ end }}
    actions:
      - type: button
        text: 'Acknowledge'
        url: 'https://alertmanager.company.com/#/alerts'
      - type: button  
        text: 'View Grafana'
        url: 'https://grafana.company.com'
```

### Microsoft Teams Integration

```yaml
# teams webhook configuration  
webhook_configs:
  - url: 'https://company.webhook.office.com/webhookb2/xxx'
    send_resolved: true
    http_config:
      bearer_token: 'your-teams-token'
    title: 'Ignition Alert: {{ .GroupLabels.alertname }}'
    message: |
      {{ range .Alerts }}
      **{{ .Annotations.summary }}**
      
      {{ .Annotations.description }}
      
      - Instance: {{ .Labels.instance }}
      - Severity: {{ .Labels.severity }}
      - Status: {{ .Status }}
      {{ end }}
```

### Email Templates

```yaml
email_configs:
  - to: 'team@company.com'
    from: 'alertmanager@company.com'
    subject: |
      {{ if eq .Status "firing" }}[FIRING] {{ else }}[RESOLVED] {{ end }}
      {{ .GroupLabels.alertname }} ({{ .Alerts | len }} alerts)
    html: |
      <!DOCTYPE html>
      <html>
      <head>
        <style>
          body { font-family: Arial, sans-serif; }
          .critical { background-color: #ffebee; border-left: 5px solid #f44336; }
          .warning { background-color: #fff3e0; border-left: 5px solid #ff9800; }
          .resolved { background-color: #e8f5e8; border-left: 5px solid #4caf50; }
          .alert { margin: 10px 0; padding: 10px; }
          .header { font-weight: bold; margin-bottom: 5px; }
        </style>
      </head>
      <body>
        <h2>Ignition Gateway Alerts</h2>
        {{ range .Alerts }}
        <div class="alert {{ .Labels.severity }}">
          <div class="header">{{ .Annotations.summary }}</div>
          <p>{{ .Annotations.description }}</p>
          <p><strong>Instance:</strong> {{ .Labels.instance }}</p>
          <p><strong>Started:</strong> {{ .StartsAt.Format "2006-01-02 15:04:05" }}</p>
          {{ if .Annotations.dashboard_url }}
          <p><a href="{{ .Annotations.dashboard_url }}">View Dashboard</a></p>
          {{ end }}
        </div>
        {{ end }}
      </body>
      </html>
```

## Alert Testing and Validation

### Automated Alert Testing

```bash
#!/bin/bash
# test-alerts.sh

ALERTMANAGER_URL="http://localhost:9093"
PROMETHEUS_URL="http://localhost:9090"

echo "Testing Ignition alert rules..."

# Test gateway down alert
echo "Testing gateway down detection..."
curl -XPOST "$ALERTMANAGER_URL/api/v1/alerts" -H "Content-Type: application/json" -d '[
  {
    "labels": {
      "alertname": "IgnitionGatewayDown",
      "instance": "test-gateway:8088",
      "job": "ignition-gateway",
      "severity": "critical"
    },
    "annotations": {
      "summary": "Test alert - Ignition Gateway is down",
      "description": "This is a test alert for gateway down condition"
    },
    "startsAt": "'$(date -Iseconds)'",
    "endsAt": "'$(date -d '+5 minutes' -Iseconds)'"
  }
]'

# Verify alert shows up
sleep 5
curl -s "$ALERTMANAGER_URL/api/v1/alerts" | jq '.data[] | select(.labels.alertname=="IgnitionGatewayDown")'

echo "Alert testing complete"
```

### Load Testing Alerts

```bash
#!/bin/bash
# load-test-alerts.sh

# Simulate high memory usage alert
echo "Simulating high memory condition..."

# This would typically involve actually stressing the gateway
# For demonstration, we'll inject test metrics

cat << 'EOF' | curl -X POST http://localhost:9090/api/v1/admin/tsdb/delete_series
match[]=jvm_memory_bytes_used{instance="test-gateway:8088"}
match[]=jvm_memory_bytes_max{instance="test-gateway:8088"}
EOF

# Send test metrics indicating high memory usage
curl -X POST http://localhost:9090/api/v1/write \
  --data-binary 'jvm_memory_bytes_used{area="heap",instance="test-gateway:8088",job="ignition-gateway"} 900000000
jvm_memory_bytes_max{area="heap",instance="test-gateway:8088",job="ignition-gateway"} 1000000000'

echo "Test metrics sent. Check Prometheus for alert evaluation."
```

## Alert Maintenance

### Alert Rule Management

```bash
#!/bin/bash
# manage-alert-rules.sh

PROMETHEUS_URL="http://localhost:9090"

# Reload alert rules
reload_rules() {
    echo "Reloading Prometheus rules..."
    curl -X POST "$PROMETHEUS_URL/-/reload"
    echo "Rules reloaded"
}

# Validate rule syntax
validate_rules() {
    echo "Validating alert rules..."
    promtool check rules alerts/*.yml
}

# List active alerts
list_alerts() {
    echo "Active alerts:"
    curl -s "$PROMETHEUS_URL/api/v1/alerts" | \
        jq -r '.data.alerts[] | "\(.labels.alertname): \(.labels.instance) (\(.state))"'
}

case "$1" in
    reload) reload_rules ;;
    validate) validate_rules ;;
    list) list_alerts ;;
    *) echo "Usage: $0 {reload|validate|list}" ;;
esac
```

### Alert Metrics and Monitoring

Monitor the alerting system itself:

```yaml
# Monitor AlertManager
scrape_configs:
  - job_name: 'alertmanager'
    static_configs:
      - targets: ['localhost:9093']

# Alert on AlertManager issues
groups:
  - name: alertmanager.meta
    rules:
      - alert: AlertManagerDown
        expr: up{job="alertmanager"} == 0
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "AlertManager is down"

      - alert: AlertManagerHighNotificationFailures
        expr: rate(alertmanager_notifications_failed_total[5m]) > 0.1
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "High notification failure rate"
```

## Alert Optimization

### Reducing Alert Noise

```yaml
# Use appropriate for durations to avoid flapping
- alert: IgnitionMemoryFlapping
  expr: (jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) > 0.8
  for: 15m  # Long enough to avoid false positives
  
# Use inhibition rules to prevent cascading alerts
inhibit_rules:
  - source_match:
      alertname: IgnitionGatewayDown
    target_match_re:
      alertname: Ignition.*
    equal: ['instance']
```

### Alert Effectiveness Metrics

```promql
# Alert firing frequency
rate(prometheus_notifications_total[1h])

# Time to resolution
histogram_quantile(0.95, 
  rate(alertmanager_notification_duration_seconds_bucket[1h]))

# False positive rate (alerts that resolve within 5 minutes)
rate(prometheus_notifications_total{state="resolved"}[1h]) 
/ rate(prometheus_notifications_total{state="firing"}[1h])
```

## Next Steps

After implementing alerting:

1. **[Monitor alert effectiveness](../troubleshooting/debugging)** and tune thresholds
2. **[Create runbooks](../troubleshooting/common-issues)** for each alert condition
3. **[Implement alert dashboard](dashboard-creation)** for ops team visibility