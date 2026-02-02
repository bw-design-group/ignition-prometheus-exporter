# Grafana Dashboards

Create comprehensive Grafana dashboards to visualize Ignition gateway metrics collected through the Prometheus Exporter Module.

## Basic Dashboard Setup

### Data Source Configuration

Configure Prometheus as a data source in Grafana:

```json
{
  "name": "Prometheus-Ignition",
  "type": "prometheus", 
  "url": "http://prometheus:9090",
  "access": "proxy",
  "isDefault": true,
  "jsonData": {
    "timeInterval": "30s",
    "queryTimeout": "60s",
    "httpMethod": "POST"
  }
}
```

### Dashboard Template Variables

Create template variables for flexible dashboard filtering:

```json
{
  "templating": {
    "list": [
      {
        "name": "instance",
        "type": "query",
        "query": "label_values(up{job=\"ignition-gateway\"}, instance)",
        "refresh": 2,
        "includeAll": true,
        "allValue": ".*"
      },
      {
        "name": "environment", 
        "type": "query",
        "query": "label_values(up{job=\"ignition-gateway\"}, environment)",
        "refresh": 2,
        "includeAll": true
      },
      {
        "name": "interval",
        "type": "interval",
        "auto": true,
        "auto_count": 30,
        "auto_min": "10s",
        "options": ["30s", "1m", "5m", "15m", "1h"]
      }
    ]
  }
}
```

## Gateway Overview Dashboard

### High-Level Status Panel

Create a stat panel showing gateway status:

```json
{
  "title": "Gateway Status",
  "type": "stat",
  "targets": [
    {
      "expr": "up{job=\"ignition-gateway\",instance=~\"$instance\"}",
      "legendFormat": "{{instance}}"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "mappings": [
        {"options": {"0": {"text": "Down", "color": "red"}},
        {"options": {"1": {"text": "Up", "color": "green"}}
      ],
      "thresholds": {
        "steps": [
          {"color": "red", "value": 0},
          {"color": "green", "value": 1}
        ]
      }
    }
  }
}
```

### Memory Usage Visualization

Create a time series panel for memory trends:

```json
{
  "title": "JVM Memory Usage",
  "type": "timeseries",
  "targets": [
    {
      "expr": "jvm_memory_bytes_used{area=\"heap\",instance=~\"$instance\"} / 1024 / 1024",
      "legendFormat": "{{instance}} - Heap Used (MB)"
    },
    {
      "expr": "jvm_memory_bytes_max{area=\"heap\",instance=~\"$instance\"} / 1024 / 1024", 
      "legendFormat": "{{instance}} - Heap Max (MB)"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "unit": "decbytes",
      "custom": {
        "drawStyle": "line",
        "lineInterpolation": "smooth",
        "fillOpacity": 10
      }
    }
  },
  "options": {
    "legend": {
      "displayMode": "table",
      "values": ["current", "max", "mean"]
    }
  }
}
```

### Complete Dashboard JSON

Full dashboard configuration for Ignition Gateway Overview:

```json
{
  "dashboard": {
    "title": "Ignition Gateway Overview",
    "tags": ["ignition", "gateway", "prometheus"],
    "timezone": "browser",
    "refresh": "30s",
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "panels": [
      {
        "id": 1,
        "title": "Gateway Status",
        "type": "stat",
        "gridPos": {"h": 4, "w": 6, "x": 0, "y": 0},
        "targets": [{
          "expr": "up{job=\"ignition-gateway\",instance=~\"$instance\"}",
          "instant": true
        }]
      },
      {
        "id": 2,
        "title": "Active Gateways",
        "type": "stat", 
        "gridPos": {"h": 4, "w": 6, "x": 6, "y": 0},
        "targets": [{
          "expr": "count(up{job=\"ignition-gateway\",instance=~\"$instance\"} == 1)",
          "instant": true
        }]
      },
      {
        "id": 3,
        "title": "JVM Memory Usage",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 4},
        "targets": [
          {
            "expr": "jvm_memory_bytes_used{area=\"heap\",instance=~\"$instance\"}",
            "legendFormat": "{{instance}} - Heap Used"
          },
          {
            "expr": "jvm_memory_bytes_max{area=\"heap\",instance=~\"$instance\"}",
            "legendFormat": "{{instance}} - Heap Max"
          }
        ]
      },
      {
        "id": 4,
        "title": "Thread Count",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 4},
        "targets": [{
          "expr": "jvm_threads_current{instance=~\"$instance\"}",
          "legendFormat": "{{instance}} - Threads"
        }]
      },
      {
        "id": 5,
        "title": "GC Activity", 
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 12},
        "targets": [
          {
            "expr": "rate(jvm_gc_collection_seconds_count{instance=~\"$instance\"}[$interval])",
            "legendFormat": "{{instance}} - {{gc}} Collections/sec"
          },
          {
            "expr": "rate(jvm_gc_collection_seconds{instance=~\"$instance\"}[$interval])",
            "legendFormat": "{{instance}} - {{gc}} Time/sec"
          }
        ]
      }
    ]
  }
}
```

## Performance Monitoring Dashboard

### CPU and Memory Correlation

```json
{
  "title": "Performance Correlation",
  "type": "timeseries",
  "targets": [
    {
      "expr": "rate(process_cpu_seconds_total{instance=~\"$instance\"}[$interval]) * 100",
      "legendFormat": "{{instance}} - CPU %",
      "yAxis": 1
    },
    {
      "expr": "(jvm_memory_bytes_used{area=\"heap\",instance=~\"$instance\"} / jvm_memory_bytes_max{area=\"heap\",instance=~\"$instance\"}) * 100",
      "legendFormat": "{{instance}} - Memory %", 
      "yAxis": 2
    }
  ],
  "fieldConfig": {
    "overrides": [
      {
        "matcher": {"id": "byName", "options": "CPU %"},
        "properties": [{"id": "custom.axisPlacement", "value": "left"}]
      },
      {
        "matcher": {"id": "byName", "options": "Memory %"},
        "properties": [{"id": "custom.axisPlacement", "value": "right"}]
      }
    ]
  }
}
```

### Response Time Heatmap

```json
{
  "title": "Scrape Duration Heatmap",
  "type": "heatmap", 
  "targets": [{
    "expr": "scrape_duration_seconds{job=\"ignition-gateway\",instance=~\"$instance\"}",
    "format": "heatmap",
    "legendFormat": "{{instance}}"
  }],
  "heatmap": {
    "xBucketSize": "1m",
    "yBucketSize": "0.1s",
    "yBucketBound": "upper"
  }
}
```

## JVM Deep Dive Dashboard

### Memory Areas Breakdown

```json
{
  "title": "Memory Areas Detail",
  "type": "timeseries",
  "targets": [
    {
      "expr": "jvm_memory_bytes_used{instance=~\"$instance\"}",
      "legendFormat": "{{instance}} - {{area}} Used"
    },
    {
      "expr": "jvm_memory_bytes_committed{instance=~\"$instance\"}",
      "legendFormat": "{{instance}} - {{area}} Committed"  
    },
    {
      "expr": "jvm_memory_bytes_max{instance=~\"$instance\"}",
      "legendFormat": "{{instance}} - {{area}} Max"
    }
  ],
  "fieldConfig": {
    "defaults": {
      "unit": "bytes",
      "custom": {
        "stacking": {"mode": "none"},
        "fillOpacity": 5
      }
    }
  }
}
```

### Garbage Collection Analysis

```json
{
  "title": "GC Performance Analysis",
  "type": "table",
  "targets": [
    {
      "expr": "rate(jvm_gc_collection_seconds_count{instance=~\"$instance\"}[5m])",
      "format": "table",
      "instant": true
    },
    {
      "expr": "rate(jvm_gc_collection_seconds{instance=~\"$instance\"}[5m])",
      "format": "table", 
      "instant": true
    }
  ],
  "transformations": [
    {
      "id": "merge",
      "options": {}
    },
    {
      "id": "organize",
      "options": {
        "excludeByName": {"Time": true},
        "renameByName": {
          "Value #A": "Collections/sec",
          "Value #B": "Time/sec",
          "instance": "Instance",
          "gc": "GC Type"
        }
      }
    }
  ]
}
```

## Custom Metrics Dashboard

### Application-Specific Panels

For custom metrics registered via scripting:

```json
{
  "title": "Custom Application Metrics",
  "panels": [
    {
      "title": "Perspective Sessions",
      "type": "stat",
      "targets": [{
        "expr": "ignition_active_perspective_sessions{instance=~\"$instance\"}",
        "legendFormat": "{{instance}}"
      }],
      "fieldConfig": {
        "defaults": {
          "color": {"mode": "thresholds"},
          "thresholds": {
            "steps": [
              {"color": "green", "value": 0},
              {"color": "yellow", "value": 50},
              {"color": "red", "value": 100}
            ]
          }
        }
      }
    },
    {
      "title": "Operation Counters",
      "type": "timeseries",
      "targets": [{
        "expr": "rate(ignition_operations_total{instance=~\"$instance\"}[$interval])",
        "legendFormat": "{{instance}} - {{operation}}"
      }]
    }
  ]
}
```

## Multi-Gateway Comparison Dashboard

### Gateway Fleet Overview

```json
{
  "title": "Gateway Fleet Status",
  "type": "table",
  "targets": [
    {
      "expr": "up{job=\"ignition-gateway\"}",
      "format": "table",
      "instant": true
    },
    {
      "expr": "(jvm_memory_bytes_used{area=\"heap\"} / jvm_memory_bytes_max{area=\"heap\"}) * 100",
      "format": "table",
      "instant": true
    },
    {
      "expr": "jvm_threads_current",
      "format": "table", 
      "instant": true
    }
  ],
  "transformations": [
    {
      "id": "merge"
    },
    {
      "id": "organize",
      "options": {
        "renameByName": {
          "Value #A": "Status",
          "Value #B": "Memory %",
          "Value #C": "Threads"
        }
      }
    }
  ],
  "fieldConfig": {
    "overrides": [
      {
        "matcher": {"id": "byName", "options": "Status"},
        "properties": [{
          "id": "mappings",
          "value": [
            {"options": {"0": {"text": "Down", "color": "red"}}},
            {"options": {"1": {"text": "Up", "color": "green"}}}
          ]
        }]
      },
      {
        "matcher": {"id": "byName", "options": "Memory %"},
        "properties": [{
          "id": "color",
          "value": {
            "mode": "thresholds",
            "thresholds": [
              {"color": "green", "value": 0},
              {"color": "yellow", "value": 75},
              {"color": "red", "value": 90}
            ]
          }
        }]
      }
    ]
  }
}
```

## Alert Integration

### Alert Status Panel

Display current alerts in dashboard:

```json
{
  "title": "Active Alerts",
  "type": "alertlist",
  "options": {
    "showOptions": "current",
    "maxItems": 20,
    "sortOrder": 1,
    "dashboardAlerts": false,
    "alertInstanceLabelFilter": "{instance=~\"$instance\"}"
  }
}
```

### Alert History Panel

```json
{
  "title": "Alert Timeline",
  "type": "timeseries",
  "targets": [{
    "expr": "ALERTS{job=\"ignition-gateway\",instance=~\"$instance\"}",
    "legendFormat": "{{alertname}} - {{instance}}"
  }],
  "fieldConfig": {
    "defaults": {
      "custom": {
        "drawStyle": "points",
        "pointSize": 8
      }
    }
  }
}
```

## Dashboard Automation

### Provisioned Dashboard Configuration

Create `/etc/grafana/provisioning/dashboards/ignition.yml`:

```yaml
apiVersion: 1

providers:
  - name: 'ignition-dashboards'
    type: file
    disableDeletion: true
    updateIntervalSeconds: 30
    allowUiUpdates: true
    options:
      path: /etc/grafana/dashboards/ignition
```

### Dashboard Export Script

```bash
#!/bin/bash
# export-dashboards.sh

GRAFANA_URL="http://localhost:3000"
API_KEY="your-api-key"
OUTPUT_DIR="/tmp/dashboards"

# Create output directory
mkdir -p $OUTPUT_DIR

# Get list of dashboards
curl -H "Authorization: Bearer $API_KEY" \
  "$GRAFANA_URL/api/search?tag=ignition" | \
  jq -r '.[] | .uid' | \
  while read uid; do
    echo "Exporting dashboard: $uid"
    curl -H "Authorization: Bearer $API_KEY" \
      "$GRAFANA_URL/api/dashboards/uid/$uid" | \
      jq '.dashboard' > "$OUTPUT_DIR/$uid.json"
  done
```

## Performance Optimization

### Dashboard Performance Best Practices

1. **Use appropriate time ranges**: Avoid excessive historical data
2. **Limit panel queries**: Use template variables for filtering  
3. **Optimize query intervals**: Match data resolution to visualization needs
4. **Use query caching**: Enable Prometheus query caching

### Query Optimization Examples

**Efficient memory usage query:**
```promql
# Good: Specific time range and aggregation
avg_over_time(jvm_memory_bytes_used{area="heap"}[5m])

# Avoid: Too broad or high-resolution queries
jvm_memory_bytes_used{area="heap"}[24h:1s]
```

**Efficient multi-gateway queries:**
```promql
# Good: Use template variables for filtering
up{job="ignition-gateway",instance=~"$instance"}

# Avoid: Hardcoded instance lists
up{job="ignition-gateway",instance=~"gateway-1|gateway-2|gateway-3"}
```

## Troubleshooting Dashboards

### Common Issues

**Missing Data Points**
- Check Prometheus data retention
- Verify scrape intervals match dashboard refresh rates
- Confirm metric names and labels are correct

**Slow Dashboard Loading**
- Reduce time range or increase query intervals
- Use recording rules for complex calculations
- Enable query result caching

**Visualization Problems**
- Verify unit configurations match data types
- Check threshold settings for color coding
- Confirm legend formatting is appropriate

### Debugging Queries

Use Prometheus query browser to test queries:

```bash
# Test query in Prometheus directly
curl -G http://prometheus:9090/api/v1/query \
  --data-urlencode 'query=jvm_memory_bytes_used{area="heap"}' \
  --data-urlencode 'time=2024-01-01T12:00:00Z'

# Check for data availability
curl -G http://prometheus:9090/api/v1/label/__name__/values | \
  grep jvm_memory
```

## Next Steps

After creating dashboards:

1. **[Set up alerting integration](alerting-rules)** for dashboard-driven alerts
2. **[Configure dashboard sharing](../examples/dashboard-creation)** for team access
3. **[Monitor dashboard performance](../troubleshooting/performance)** and optimize queries