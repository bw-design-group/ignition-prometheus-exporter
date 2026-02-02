# First Metrics

After installing and configuring the Prometheus Exporter Module, verify it's working correctly by viewing the exported metrics and understanding the data format.

## Accessing the Metrics Endpoint

### Basic Access

Open your web browser or use curl to access the metrics endpoint:

```bash
curl http://your-gateway:8088/system/metrics
```

For HTTPS-enabled gateways:
```bash
curl https://your-gateway:8043/system/metrics
```

### Expected Response Format

The response should be in Prometheus exposition format:

```
# HELP jvm_memory_bytes_used Used bytes of a given JVM memory area.
# TYPE jvm_memory_bytes_used gauge
jvm_memory_bytes_used{area="heap"} 1.23456789E8
jvm_memory_bytes_used{area="nonheap"} 5.6789012E7

# HELP jvm_memory_bytes_max Max bytes of a given JVM memory area.
# TYPE jvm_memory_bytes_max gauge
jvm_memory_bytes_max{area="heap"} 2.34567890E8
jvm_memory_bytes_max{area="nonheap"} 1.23456789E8
```

## Understanding the Metric Format

### Metric Structure

Each metric follows the Prometheus exposition format:

```
# HELP metric_name Description of what this metric measures
# TYPE metric_name metric_type
metric_name{label1="value1",label2="value2"} metric_value timestamp
```

**Components:**
- **HELP**: Human-readable description of the metric
- **TYPE**: Metric type (gauge, counter, histogram, summary)
- **Labels**: Key-value pairs that add dimensions to metrics
- **Value**: The actual measurement
- **Timestamp**: Optional Unix timestamp (usually omitted)

### Common Metric Types

**Gauge Metrics** (values that can go up or down)
```
jvm_memory_bytes_used{area="heap"} 1.234E8
jvm_threads_current 42
```

**Counter Metrics** (values that only increase)
```
jvm_gc_collection_seconds_total{gc="G1 Young Generation"} 1.234
http_requests_total{method="GET",status="200"} 1542
```

**Histogram Metrics** (distribution of observations)
```
http_request_duration_seconds_bucket{le="0.1"} 24054
http_request_duration_seconds_bucket{le="0.2"} 26651
http_request_duration_seconds_bucket{le="+Inf"} 27547
http_request_duration_seconds_sum 53423
http_request_duration_seconds_count 27547
```

## Key Metrics Categories

### JVM Memory Metrics

Monitor Java Virtual Machine memory usage:

```bash
# Filter for memory metrics
curl -s http://your-gateway:8088/system/metrics | grep "jvm_memory"
```

**Important memory metrics:**
- `jvm_memory_bytes_used{area="heap"}` - Current heap memory usage
- `jvm_memory_bytes_max{area="heap"}` - Maximum heap memory available
- `jvm_memory_bytes_used{area="nonheap"}` - Non-heap memory (method area, code cache)

### JVM Thread Metrics

Track thread pool health:

```bash
# Filter for thread metrics  
curl -s http://your-gateway:8088/system/metrics | grep "jvm_threads"
```

**Key thread metrics:**
- `jvm_threads_current` - Current number of live threads
- `jvm_threads_daemon` - Current number of daemon threads
- `jvm_threads_peak` - Peak number of live threads since JVM start
- `jvm_threads_started_total` - Total number of threads created and started

### Garbage Collection Metrics

Monitor GC performance:

```bash
# Filter for GC metrics
curl -s http://your-gateway:8088/system/metrics | grep "jvm_gc"
```

**GC metrics:**
- `jvm_gc_collection_seconds_total{gc="G1 Young Generation"}` - Total GC time
- `jvm_gc_collection_seconds_count{gc="G1 Young Generation"}` - Number of GC runs

### Gateway-Specific Metrics

Ignition internal metrics from Dropwizard registry:

```bash
# Look for Ignition-specific metrics
curl -s http://your-gateway:8088/system/metrics | grep -E "(ignition|gateway|module)"
```

Examples may include:
- Connection pool metrics
- Module-specific counters  
- Internal service metrics
- Custom application metrics

## Interpreting Metric Values

### Memory Usage Analysis

```bash
# Calculate heap utilization percentage
curl -s http://your-gateway:8088/system/metrics | \
grep "jvm_memory_bytes.*heap" | \
awk 'BEGIN{used=0;max=0} 
/used.*heap/ {used=$2} 
/max.*heap/ {max=$2} 
END {printf "Heap Usage: %.1f%%\n", (used/max)*100}'
```

### Thread Pool Health

```bash
# Check thread metrics
curl -s http://your-gateway:8088/system/metrics | \
grep "jvm_threads_current" | \
awk '{print "Current Threads:", $2}'
```

### GC Performance

```bash
# Calculate average GC time
curl -s http://your-gateway:8088/system/metrics | \
grep "jvm_gc.*seconds" | \
awk 'BEGIN{total_time=0;total_count=0}
/_total/ {total_time+=$2}
/_count/ {total_count+=$2}
END {if(total_count>0) printf "Average GC Time: %.3f seconds\n", total_time/total_count}'
```

## Validation Checks

### Metric Count Verification

Count the total number of metrics exposed:

```bash
# Count unique metric names
curl -s http://your-gateway:8088/system/metrics | \
grep -E "^[a-zA-Z_][a-zA-Z0-9_]*\{" | \
cut -d'{' -f1 | sort -u | wc -l
```

Typical gateway installations expose 50-200+ metrics depending on:
- Installed modules
- Active connections
- Custom metrics registered by scripts

### Format Validation

Verify Prometheus format compliance:

```bash
# Check for proper HELP and TYPE declarations
curl -s http://your-gateway:8088/system/metrics | \
grep -E "^# (HELP|TYPE)" | head -10
```

### Data Quality Checks

**Check for valid numeric values:**
```bash
# Look for non-numeric or invalid values
curl -s http://your-gateway:8088/system/metrics | \
grep -E "^[a-zA-Z_]" | \
grep -v -E "[0-9]+(\.[0-9]+)?(E[+-]?[0-9]+)?\s*$" | head -5
```

**Verify timestamps are optional:**
```bash
# Most metrics should not have timestamps
curl -s http://your-gateway:8088/system/metrics | \
grep -E "^[a-zA-Z_].*[0-9]+\s+[0-9]{10}" | wc -l
```

## Common Issues and Solutions

### No Metrics Returned

**Problem**: Empty response or HTTP 404 error

**Diagnosis**:
```bash
# Check module status
curl -I http://your-gateway:8088/system/metrics

# Expected: HTTP 200 with content-type header
# Actual: HTTP 404 or connection refused
```

**Solutions**:
1. Verify module is installed and running
2. Check gateway web server is accessible
3. Confirm correct hostname and port

### Incomplete Metrics

**Problem**: Very few metrics returned

**Diagnosis**:
```bash
# Count metrics - should be >50 for typical installation
curl -s http://your-gateway:8088/system/metrics | grep -c "^[a-zA-Z_]"
```

**Solutions**:
1. Wait for gateway to fully initialize
2. Check for module startup errors in logs
3. Verify Dropwizard registry is populated

### Format Issues

**Problem**: Invalid Prometheus format

**Diagnosis**:
```bash
# Check for format compliance
curl -s http://your-gateway:8088/system/metrics | head -20
```

**Solutions**:
1. Restart the module
2. Check for custom metrics with invalid names
3. Verify no conflicting servlet registrations

## Next Steps

After confirming metrics are working:

1. **[Set up Prometheus scraping](../prometheus-integration/scrape-configuration)** to collect data
2. **[Explore available metrics](../metrics/available-metrics)** in detail
3. **[Create monitoring dashboards](../examples/dashboard-creation)** for visualization
4. **[Configure alerting rules](../examples/alerting-setup)** for proactive monitoring