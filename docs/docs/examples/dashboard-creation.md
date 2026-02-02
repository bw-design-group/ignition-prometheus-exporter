# Dashboard Creation

Step-by-step guide for creating effective Grafana dashboards to visualize Ignition gateway metrics from the Prometheus Exporter Module.

## Dashboard Design Principles

### Effective Dashboard Layout

**Information Hierarchy**
1. **Top Row**: High-level status indicators (gateway up/down, critical alerts)
2. **Middle Rows**: Key performance metrics (memory, CPU, threads)
3. **Bottom Rows**: Detailed analysis (GC activity, trends, correlations)

**Visual Guidelines**
- Use consistent color schemes across related metrics
- Place related metrics in logical groupings
- Include context through appropriate time ranges
- Provide drill-down capabilities with links between dashboards

## Complete Dashboard Example

### Gateway Overview Dashboard

This comprehensive dashboard provides a complete view of Ignition gateway health:

```json
{
  "dashboard": {
    "id": null,
    "title": "Ignition Gateway Overview",
    "tags": ["ignition", "gateway", "overview"],
    "timezone": "browser",
    "editable": true,
    "hideControls": false,
    "refresh": "30s",
    "time": {
      "from": "now-1h",
      "to": "now"
    },
    "templating": {
      "list": [
        {
          "name": "instance",
          "type": "query",
          "query": "label_values(up{job=\"ignition-gateway\"}, instance)",
          "refresh": 2,
          "includeAll": true,
          "allValue": ".*",
          "multi": true
        },
        {
          "name": "interval",
          "type": "interval",
          "auto": true,
          "auto_count": 30,
          "auto_min": "10s",
          "options": ["30s", "1m", "5m", "10m", "30m", "1h"]
        }
      ]
    },
    "panels": [
      {
        "id": 1,
        "title": "Gateway Status",
        "type": "stat",
        "gridPos": {"h": 4, "w": 4, "x": 0, "y": 0},
        "targets": [{
          "expr": "up{job=\"ignition-gateway\",instance=~\"$instance\"}",
          "legendFormat": "{{instance}}"
        }],
        "fieldConfig": {
          "defaults": {
            "mappings": [
              {"options": {"0": {"text": "Down", "color": "red"}},
               "options": {"1": {"text": "Up", "color": "green"}}
            ],
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "green", "value": 1}
              ]
            }
          }
        }
      },
      {
        "id": 2,
        "title": "Memory Usage %",
        "type": "stat",
        "gridPos": {"h": 4, "w": 4, "x": 4, "y": 0},
        "targets": [{
          "expr": "(jvm_memory_bytes_used{area=\"heap\",instance=~\"$instance\"} / jvm_memory_bytes_max{area=\"heap\",instance=~\"$instance\"}) * 100",
          "legendFormat": "{{instance}}"
        }],
        "fieldConfig": {
          "defaults": {
            "unit": "percent",
            "thresholds": {
              "steps": [
                {"color": "green", "value": 0},
                {"color": "yellow", "value": 70},
                {"color": "red", "value": 85}
              ]
            }
          }
        }
      },
      {
        "id": 3,
        "title": "Thread Count",
        "type": "stat",
        "gridPos": {"h": 4, "w": 4, "x": 8, "y": 0},
        "targets": [{
          "expr": "jvm_threads_current{instance=~\"$instance\"}",
          "legendFormat": "{{instance}}"
        }],
        "fieldConfig": {
          "defaults": {
            "thresholds": {
              "steps": [
                {"color": "green", "value": 0},
                {"color": "yellow", "value": 300},
                {"color": "red", "value": 500}
              ]
            }
          }
        }
      },
      {
        "id": 4,
        "title": "GC Rate (collections/min)",
        "type": "stat",
        "gridPos": {"h": 4, "w": 4, "x": 12, "y": 0},
        "targets": [{
          "expr": "rate(jvm_gc_collection_seconds_count{instance=~\"$instance\"}[$interval]) * 60",
          "legendFormat": "{{instance}}"
        }],
        "fieldConfig": {
          "defaults": {
            "unit": "cpm",
            "thresholds": {
              "steps": [
                {"color": "green", "value": 0},
                {"color": "yellow", "value": 5},
                {"color": "red", "value": 10}
              ]
            }
          }
        }
      },
      {
        "id": 5,
        "title": "JVM Memory Usage",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 0, "y": 4},
        "targets": [
          {
            "expr": "jvm_memory_bytes_used{area=\"heap\",instance=~\"$instance\"} / 1024 / 1024",
            "legendFormat": "{{instance}} - Heap Used (MB)"
          },
          {
            "expr": "jvm_memory_bytes_max{area=\"heap\",instance=~\"$instance\"} / 1024 / 1024",
            "legendFormat": "{{instance}} - Heap Max (MB)"
          },
          {
            "expr": "jvm_memory_bytes_used{area=\"nonheap\",instance=~\"$instance\"} / 1024 / 1024", 
            "legendFormat": "{{instance}} - Non-Heap Used (MB)"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "unit": "decbytes",
            "custom": {
              "drawStyle": "line",
              "lineInterpolation": "smooth",
              "fillOpacity": 10,
              "pointSize": 5
            }
          }
        },
        "options": {
          "legend": {
            "displayMode": "table",
            "values": ["current", "max", "mean"]
          }
        }
      },
      {
        "id": 6,
        "title": "Thread Activity",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 12, "x": 12, "y": 4},
        "targets": [
          {
            "expr": "jvm_threads_current{instance=~\"$instance\"}",
            "legendFormat": "{{instance}} - Current"
          },
          {
            "expr": "jvm_threads_daemon{instance=~\"$instance\"}",
            "legendFormat": "{{instance}} - Daemon"
          },
          {
            "expr": "jvm_threads_peak{instance=~\"$instance\"}",
            "legendFormat": "{{instance}} - Peak"
          }
        ],
        "fieldConfig": {
          "defaults": {
            "custom": {
              "drawStyle": "line",
              "lineWidth": 2
            }
          }
        }
      },
      {
        "id": 7,
        "title": "Garbage Collection Activity",
        "type": "timeseries",
        "gridPos": {"h": 8, "w": 24, "x": 0, "y": 12},
        "targets": [
          {
            "expr": "rate(jvm_gc_collection_seconds_count{instance=~\"$instance\"}[$interval])",
            "legendFormat": "{{instance}} - {{gc}} Collections/sec"
          },
          {
            "expr": "rate(jvm_gc_collection_seconds{instance=~\"$instance\"}[$interval]) * 1000",
            "legendFormat": "{{instance}} - {{gc}} Time (ms/sec)",
            "yAxis": 2
          }
        ],
        "fieldConfig": {
          "overrides": [
            {
              "matcher": {"id": "byRegexp", "options": ".*Time.*"},
              "properties": [
                {"id": "custom.axisPlacement", "value": "right"},
                {"id": "unit", "value": "ms"}
              ]
            }
          ]
        }
      }
    ],
    "annotations": {
      "list": [
        {
          "name": "Gateway Restarts",
          "datasource": "Prometheus",
          "enable": true,
          "expr": "changes(process_start_time_seconds{job=\"ignition-gateway\"}[1m]) > 0",
          "iconColor": "red",
          "titleFormat": "Gateway Restart",
          "textFormat": "Gateway {{$labels.instance}} restarted"
        }
      ]
    }
  }
}
```

## Specialized Dashboards

### Performance Analysis Dashboard

Create focused dashboards for specific analysis:

```json
{
  "dashboard": {
    "title": "Ignition Performance Analysis",
    "panels": [
      {
        "id": 1,
        "title": "CPU vs Memory Correlation",
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
      },
      {
        "id": 2,
        "title": "GC Impact on Performance",
        "type": "timeseries",
        "targets": [
          {
            "expr": "rate(jvm_gc_collection_seconds{instance=~\"$instance\"}[$interval]) * 100",
            "legendFormat": "{{instance}} - {{gc}} GC Time %"
          },
          {
            "expr": "scrape_duration_seconds{job=\"ignition-gateway\",instance=~\"$instance\"}",
            "legendFormat": "{{instance}} - Scrape Duration",
            "yAxis": 2
          }
        ]
      }
    ]
  }
}
```

### Multi-Gateway Comparison Dashboard

Compare metrics across multiple gateways:

```json
{
  "dashboard": {
    "title": "Gateway Fleet Comparison",
    "panels": [
      {
        "id": 1,
        "title": "Memory Usage Comparison",
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
          {"id": "merge"},
          {
            "id": "organize",
            "options": {
              "excludeByName": {"Time": true, "__name__": true, "job": true},
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
                  {"options": {"0": {"text": "Down", "color": "red"}},
                   "options": {"1": {"text": "Up", "color": "green"}}
                ]
              }]
            }
          ]
        }
      }
    ]
  }
}
```

## Custom Business Metrics Dashboard

### Production Monitoring Dashboard

For custom metrics created via scripting:

```json
{
  "dashboard": {
    "title": "Production Monitoring",
    "panels": [
      {
        "id": 1,
        "title": "Overall Equipment Effectiveness (OEE)",
        "type": "gauge",
        "targets": [{
          "expr": "ignition_production_oee_percent{instance=~\"$instance\"}",
          "legendFormat": "{{line}}"
        }],
        "fieldConfig": {
          "defaults": {
            "unit": "percent",
            "min": 0,
            "max": 100,
            "thresholds": {
              "steps": [
                {"color": "red", "value": 0},
                {"color": "yellow", "value": 60},
                {"color": "green", "value": 85}
              ]
            }
          }
        }
      },
      {
        "id": 2,
        "title": "Energy Consumption",
        "type": "timeseries",
        "targets": [{
          "expr": "ignition_power_consumption_kw{instance=~\"$instance\"}",
          "legendFormat": "{{facility}} - {{department}}"
        }],
        "fieldConfig": {
          "defaults": {
            "unit": "kwatt"
          }
        }
      },
      {
        "id": 3,
        "title": "Quality Metrics",
        "type": "stat",
        "targets": [{
          "expr": "ignition_quality_defect_rate_percent{instance=~\"$instance\"}",
          "legendFormat": "{{product}}"
        }],
        "fieldConfig": {
          "defaults": {
            "unit": "percent",
            "thresholds": {
              "steps": [
                {"color": "green", "value": 0},
                {"color": "yellow", "value": 2},
                {"color": "red", "value": 5}
              ]
            }
          }
        }
      }
    ]
  }
}
```

## Dashboard Creation Workflow

### Step 1: Planning

**Define Dashboard Purpose**
- Identify target audience (operators, engineers, management)
- Determine key metrics and KPIs to display
- Plan information hierarchy and layout

**Gather Requirements**
- What decisions will this dashboard support?
- What time ranges are most relevant?
- What alert conditions should be visible?

### Step 2: Create Basic Structure

```bash
# Create new dashboard in Grafana
curl -X POST \
  http://admin:admin123@localhost:3000/api/dashboards/db \
  -H 'Content-Type: application/json' \
  -d '{
    "dashboard": {
      "title": "New Ignition Dashboard",
      "tags": ["ignition"],
      "refresh": "30s"
    },
    "overwrite": false
  }'
```

### Step 3: Add Template Variables

Create reusable dashboards with variables:

```json
{
  "templating": {
    "list": [
      {
        "name": "environment",
        "type": "custom",
        "options": [
          {"text": "Production", "value": "production"},
          {"text": "Development", "value": "development"},
          {"text": "Test", "value": "test"}
        ]
      },
      {
        "name": "gateway",
        "type": "query",
        "query": "label_values(up{job=\"ignition-gateway\",environment=\"$environment\"}, instance)"
      }
    ]
  }
}
```

### Step 4: Build Panels Incrementally

Start with essential panels and add detail:

```json
{
  "panels": [
    {
      "title": "System Health Score",
      "type": "stat",
      "targets": [{
        "expr": "((up{instance=~\"$gateway\"} * 100) + ((jvm_memory_bytes_max{area=\"heap\"} - jvm_memory_bytes_used{area=\"heap\"}) / jvm_memory_bytes_max{area=\"heap\"} * 100)) / 2"
      }]
    }
  ]
}
```

## Dashboard Best Practices

### Performance Optimization

**Efficient Queries**
```promql
# Good: Specific time range and labels
rate(jvm_gc_collection_seconds_count{instance=~"$instance"}[5m])

# Avoid: Overly broad queries
{__name__=~"jvm_.*"}
```

**Appropriate Refresh Rates**
- Real-time monitoring: 10-30 seconds
- Historical analysis: 1-5 minutes
- Executive dashboards: 5-15 minutes

### Visual Design Guidelines

**Color Usage**
- Use consistent color schemes (green=good, yellow=warning, red=critical)
- Limit color palette to 5-7 colors maximum
- Consider colorblind accessibility

**Panel Organization**
- Group related metrics visually
- Use consistent panel sizes where possible
- Maintain alignment and spacing

## Advanced Dashboard Features

### Alert Integration

Display alert status on dashboards:

```json
{
  "id": 10,
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

### Dynamic Thresholds

Use query results to set thresholds:

```json
{
  "fieldConfig": {
    "defaults": {
      "thresholds": {
        "steps": [
          {"color": "green", "value": null},
          {
            "color": "red", 
            "value": {"expr": "ignition_memory_alert_threshold * 0.9"}
          }
        ]
      }
    }
  }
}
```

### Drill-Down Links

Connect related dashboards:

```json
{
  "options": {
    "dataLinks": [
      {
        "title": "View Detailed Performance",
        "url": "/d/detailed-performance?var-instance=${__field.labels.instance}"
      }
    ]
  }
}
```

## Dashboard Management

### Version Control

Export and version control dashboards:

```bash
#!/bin/bash
# export-dashboards.sh

GRAFANA_URL="http://localhost:3000"
API_KEY="your-api-key"
OUTPUT_DIR="./dashboards"

mkdir -p $OUTPUT_DIR

# List all dashboards
curl -H "Authorization: Bearer $API_KEY" \
  "$GRAFANA_URL/api/search?type=dash-db" | \
  jq -r '.[] | "\(.uid):\(.title)"' | \
  while IFS=':' read uid title; do
    echo "Exporting: $title"
    curl -H "Authorization: Bearer $API_KEY" \
      "$GRAFANA_URL/api/dashboards/uid/$uid" | \
      jq '.dashboard' > "$OUTPUT_DIR/${title// /_}.json"
  done
```

### Automated Deployment

Deploy dashboards via provisioning:

```yaml
# grafana/provisioning/dashboards/dashboards.yml
apiVersion: 1

providers:
  - name: 'ignition-dashboards'
    type: file
    disableDeletion: false
    updateIntervalSeconds: 30
    allowUiUpdates: true
    options:
      path: /var/lib/grafana/dashboards
```

### Testing Dashboards

Validate dashboard functionality:

```bash
# Test dashboard API
curl -H "Authorization: Bearer $API_KEY" \
  "http://localhost:3000/api/dashboards/uid/ignition-overview"

# Validate panel queries
curl -H "Authorization: Bearer $API_KEY" \
  -d '{"range":{"from":"now-1h","to":"now"},"targets":[{"expr":"up{job=\"ignition-gateway\"}"}]}' \
  "http://localhost:3000/api/tsdb/query"
```

## Troubleshooting Dashboards

### Common Issues

**No Data Displayed**
- Verify Prometheus data source configuration
- Check metric names and labels in queries
- Confirm time range includes available data

**Slow Dashboard Loading**
- Review query complexity and time ranges
- Check for high-cardinality metrics
- Consider using recording rules for complex calculations

**Inconsistent Data**  
- Verify template variable dependencies
- Check for query caching issues
- Confirm metric collection intervals

## Next Steps

After creating effective dashboards:

1. **[Set up comprehensive alerting](alerting-setup)** based on dashboard insights
2. **[Implement custom metrics](../metrics/custom-metrics)** for business-specific dashboards
3. **[Optimize performance](../troubleshooting/performance)** for large-scale deployments