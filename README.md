# Prometheus Exporter Ignition Module

A comprehensive Ignition module that enables Prometheus metrics collection and export from Inductive Automation's Ignition platform. This module provides both automatic system metrics export and programmable custom metrics via scripting functions.

## Features

- **Automatic System Metrics Export**: Exposes Ignition's internal Dropwizard metrics in Prometheus format
- **Custom Metrics Scripting**: Create and manage Counter, Gauge, and Histogram metrics via `system.prometheus.*` functions
- **Multi-Scope Support**: Works across Gateway, Designer, and Client scopes with RPC communication
- **Thread-Safe**: Separate metric registries ensure safe concurrent access
- **REST API Endpoints**: Test endpoints for validation and debugging

## Installation

1. Build the module:
   ```bash
   ./gradlew build
   ```

2. Deploy to your Ignition gateway:
   ```bash
   ./gradlew deployModl
   ```

3. The module will be available at `/system/metrics` for Prometheus scraping and `system.prometheus.*` scripting functions will be available in all scopes.

## Metrics Endpoint

**System Metrics**: `https://your-gateway/system/metrics`
- Exposes Ignition's internal Dropwizard metrics in Prometheus format
- Includes JVM, threading, memory, and Ignition-specific metrics
- Automatically available once module is installed

## Scripting API

The module provides `system.prometheus.*` functions available in Gateway, Designer, and Client scopes.

### Counter Metrics

Counters are monotonically increasing metrics, perfect for counting events, requests, errors, etc.

```python
# Create a counter
system.prometheus.createCounter('http_requests_total', 'Total HTTP requests', ['method', 'endpoint'])

# Increment counter
system.prometheus.incrementCounter('http_requests_total', {'method': 'GET', 'endpoint': '/api'}, 1.0)

# Increment without labels
system.prometheus.incrementCounter('http_requests_total', 5.0)

# Get current value
current_value = system.prometheus.getMetricValue('http_requests_total', {'method': 'GET', 'endpoint': '/api'})
```

### Gauge Metrics

Gauges can go up and down, ideal for measuring current values like temperature, CPU usage, queue size, etc.

```python
# Create a gauge
system.prometheus.createGauge('temperature_celsius', 'Current temperature', ['sensor', 'location'])

# Set gauge value
system.prometheus.setGauge('temperature_celsius', 23.5, {'sensor': 'room1', 'location': 'office'})

# Increment gauge
system.prometheus.incrementGauge('temperature_celsius', 1.2, {'sensor': 'room1', 'location': 'office'})

# Decrement gauge
system.prometheus.decrementGauge('temperature_celsius', 0.8, {'sensor': 'room1', 'location': 'office'})

# Get current value
current_temp = system.prometheus.getMetricValue('temperature_celsius', {'sensor': 'room1', 'location': 'office'})
```

### Histogram Metrics

Histograms track distributions of values, perfect for response times, request sizes, etc.

```python
# Create histogram with default buckets
system.prometheus.createHistogram('response_time_seconds', 'HTTP response time', ['service', 'method'])

# Create histogram with custom buckets
custom_buckets = [0.01, 0.05, 0.1, 0.5, 1.0, 2.0, 5.0]
system.prometheus.createHistogram('custom_latency', 'Custom latency distribution', ['endpoint'], custom_buckets)

# Observe values
system.prometheus.observeHistogram('response_time_seconds', 0.142, {'service': 'auth', 'method': 'POST'})
system.prometheus.observeHistogram('response_time_seconds', 0.089, {'service': 'auth', 'method': 'GET'})
```

### Utility Functions

```python
# List all custom metrics
metrics = system.prometheus.listMetrics()
print("Custom metrics:", metrics)

# Remove a metric
system.prometheus.removeMetric('old_metric_name')
```

## Example Usage Scenarios

### 1. Track Tag Change Events

```python
# In a tag change script
def valueChanged(tag, tagPath, previousValue, currentValue, initialChange, missedEvents):
    if not initialChange:
        # Increment counter for tag changes
        system.prometheus.incrementCounter('tag_changes_total', {
            'tag_path': str(tagPath),
            'tag_provider': tagPath.getSource()
        })
        
        # Update gauge with current value if numeric
        if isinstance(currentValue.value, (int, float)):
            system.prometheus.setGauge('tag_current_value', 
                currentValue.value, 
                {'tag_path': str(tagPath)}
            )
```

### 2. Monitor Script Execution Time

```python
# In a scheduled script
import time

def execute():
    start_time = time.time()
    
    try:
        # Your script logic here
        do_some_work()
        
        # Track successful executions
        system.prometheus.incrementCounter('script_executions_total', {'script': 'data_processor', 'status': 'success'})
        
    except Exception as e:
        # Track failures
        system.prometheus.incrementCounter('script_executions_total', {'script': 'data_processor', 'status': 'error'})
        raise
        
    finally:
        # Record execution time
        execution_time = time.time() - start_time
        system.prometheus.observeHistogram('script_duration_seconds', execution_time, {'script': 'data_processor'})
```

### 3. Track Database Connection Pool

```python
# Monitor database connections
def updateConnectionMetrics():
    # Get connection pool info (example)
    active_connections = system.db.getConnectionInfo().activeConnections
    max_connections = system.db.getConnectionInfo().maxConnections
    
    # Update gauges
    system.prometheus.setGauge('db_connections_active', active_connections)
    system.prometheus.setGauge('db_connections_max', max_connections)
    system.prometheus.setGauge('db_connections_utilization', 
        float(active_connections) / max_connections * 100.0
    )
```

## Best Practices

### Naming Conventions
- Use descriptive names with units: `http_requests_total`, `temperature_celsius`
- Follow Prometheus naming conventions: use underscores, lowercase, end counters with `_total`
- Include base unit in name: `_seconds`, `_bytes`, `_ratio`

### Label Management
- Keep label cardinality low (< 1000 combinations per metric)
- Use consistent label names across metrics
- Avoid high-cardinality labels like timestamps or user IDs

### Metric Types
- **Counters**: Use for things that only increase (requests, errors, bytes sent)
- **Gauges**: Use for values that can go up/down (temperature, CPU usage, queue size)
- **Histograms**: Use for measuring distributions (latencies, request sizes)

### Performance Tips
- Create metrics once at startup, not on every use
- Client/Designer operations have RPC overhead compared to Gateway operations
- Use appropriate bucket sizes for histograms based on your data distribution

## Testing Endpoints

The module includes REST endpoints for testing and validation:

- `/test-utils` - Utility operations (list metrics, cleanup, etc.)
- `/test-counter` - Counter operations testing
- `/test-gauge` - Gauge operations testing  
- `/test-histogram` - Histogram operations testing

Example usage:
```bash
# List available operations
curl "https://your-gateway/test-utils?action=list_actions"

# Create and test a counter
curl "https://your-gateway/test-counter?action=create&name=test_requests&help=Test counter"
curl "https://your-gateway/test-counter?action=increment&name=test_requests&method=GET&endpoint=/api"

# List all metrics
curl "https://your-gateway/test-utils?action=list"
```

## Integration with Prometheus

### Prometheus Configuration

Add this to your `prometheus.yml`:

```yaml
scrape_configs:
  - job_name: 'ignition'
    static_configs:
      - targets: ['your-ignition-gateway:8088']
    metrics_path: '/system/metrics'
    scrape_interval: 15s
```

### Common Queries

```promql
# Rate of HTTP requests per second
rate(http_requests_total[5m])

# 95th percentile response time
histogram_quantile(0.95, rate(response_time_seconds_bucket[5m]))

# Current temperature readings
temperature_celsius{sensor="room1"}

# Script execution failure rate
rate(script_executions_total{status="error"}[5m]) / rate(script_executions_total[5m])
```

## Module Architecture

- **Common**: Shared interfaces and RPC script module
- **Gateway**: Core metric operations using Prometheus Java client
- **Client/Designer**: RPC-based script modules for remote operations
- **Thread-Safe**: Separate CollectorRegistry for custom metrics
- **Multi-Scope**: Works consistently across all Ignition execution contexts

## Troubleshooting

### Script Functions Not Available
- Verify module is installed and enabled
- Check Designer console for `system.prometheus.*` availability
- Restart Designer if functions don't appear

### Metrics Not Appearing
- Check Gateway logs for errors
- Verify metric names follow Prometheus conventions
- Ensure labels aren't too high cardinality

### Performance Issues
- Reduce label cardinality
- Create metrics once, not repeatedly
- Use Gateway scope for high-frequency operations

## Version Information

- **Ignition SDK**: 8.1.44
- **Prometheus Client**: 0.16.0
- **Scope Support**: Gateway (G), Client (C), Designer (D)
- **Module ID**: `dev.bwdesigngroup.prometheus.PrometheusExporter`

## License

This module is provided as-is for development and educational purposes.