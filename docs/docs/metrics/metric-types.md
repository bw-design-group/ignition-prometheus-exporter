# Prometheus Metric Types

Understanding Prometheus metric types is essential for properly interpreting and alerting on metrics exported from Ignition gateways.

## Metric Type Overview

### Four Core Types

Prometheus supports four fundamental metric types, each serving specific monitoring use cases:

1. **Counter** - Monotonically increasing values
2. **Gauge** - Values that can go up and down
3. **Histogram** - Distribution of observations  
4. **Summary** - Similar to histogram with client-side quantiles

## Counter Metrics

### Definition and Characteristics

Counters represent cumulative values that only increase (or reset to zero):

```
# HELP jvm_gc_collection_seconds_total Time spent in a given JVM garbage collector in seconds
# TYPE jvm_gc_collection_seconds_total counter
jvm_gc_collection_seconds_total{gc="G1 Young Generation"} 2.847
jvm_gc_collection_seconds_total{gc="G1 Old Generation"} 0.123
```

**Key Properties:**
- **Monotonic**: Values only increase or reset to zero
- **Cumulative**: Represents total since process start
- **Rate Calculation**: Use `rate()` or `increase()` functions for analysis

### Common Counter Examples from Ignition

```
# GC collection counts
jvm_gc_collection_seconds_count{gc="G1 Young Generation"} 156

# Class loading totals  
jvm_classes_loaded_total 18247
jvm_classes_unloaded_total 123

# Process CPU time
process_cpu_seconds_total 1847.23

# Custom application counters
gateway_tag_reads_total 12847
gateway_database_queries_total 3421
```

### Counter Query Patterns

**Rate Calculation (per-second rate)**
```promql
# GC collections per second over 5 minutes
rate(jvm_gc_collection_seconds_count[5m])

# CPU utilization percentage
rate(process_cpu_seconds_total[1m]) * 100
```

**Increase Calculation (total increase)**
```promql
# Total GC collections in last hour
increase(jvm_gc_collection_seconds_count[1h])

# Classes loaded in last 10 minutes
increase(jvm_classes_loaded_total[10m])
```

## Gauge Metrics

### Definition and Characteristics

Gauges represent instantaneous values that can increase or decrease:

```
# HELP jvm_memory_bytes_used Used bytes of a given JVM memory area
# TYPE jvm_memory_bytes_used gauge
jvm_memory_bytes_used{area="heap"} 1.25663e+08
jvm_memory_bytes_used{area="nonheap"} 9.1234567e+07
```

**Key Properties:**
- **Instantaneous**: Current value at time of collection
- **Bidirectional**: Can increase or decrease
- **Direct Interpretation**: Value represents actual measurement

### Common Gauge Examples from Ignition

```
# Memory usage
jvm_memory_bytes_used{area="heap"} 1.25663e+08
jvm_memory_bytes_max{area="heap"} 2.147484e+09
jvm_memory_bytes_committed{area="heap"} 2.68435456e+08

# Thread counts
jvm_threads_current 147
jvm_threads_daemon 24
jvm_threads_peak 152

# Class loading
jvm_classes_loaded 18247

# Process information
process_start_time_seconds 1.70434123e+09
process_open_fds 89
process_resident_memory_bytes 4.12345678e+08

# Custom application gauges
gateway_active_connections 25
gateway_tag_subscriptions_active 156
```

### Gauge Query Patterns

**Current Value Analysis**
```promql
# Current memory utilization percentage
(jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) * 100

# Available heap memory
jvm_memory_bytes_max{area="heap"} - jvm_memory_bytes_used{area="heap"}
```

**Trend Analysis**
```promql
# Memory usage trend over time
avg_over_time(jvm_memory_bytes_used{area="heap"}[1h])

# Thread count growth rate
deriv(jvm_threads_current[10m])
```

**Threshold Alerting**
```promql
# High memory usage alert
(jvm_memory_bytes_used{area="heap"} / jvm_memory_bytes_max{area="heap"}) > 0.85

# High thread count alert
jvm_threads_current > 500
```

## Histogram Metrics

### Definition and Characteristics

Histograms sample observations and count them in configurable buckets:

```
# HELP http_request_duration_seconds Request duration histogram
# TYPE http_request_duration_seconds histogram
http_request_duration_seconds_bucket{le="0.1"} 24054
http_request_duration_seconds_bucket{le="0.2"} 26651
http_request_duration_seconds_bucket{le="0.5"} 27547
http_request_duration_seconds_bucket{le="1.0"} 27547
http_request_duration_seconds_bucket{le="+Inf"} 27547
http_request_duration_seconds_sum 1423.0
http_request_duration_seconds_count 27547
```

**Key Components:**
- **Buckets**: Cumulative counters for different thresholds
- **Sum**: Total of all observed values  
- **Count**: Total number of observations

### Histogram Examples in Ignition

Histograms are less common in basic Ignition deployments but may appear in:

```
# Module execution times (if instrumented)
module_execution_duration_seconds_bucket{module="perspective",le="0.01"} 1234
module_execution_duration_seconds_bucket{module="perspective",le="0.1"} 1456
module_execution_duration_seconds_bucket{module="perspective",le="+Inf"} 1500
module_execution_duration_seconds_sum{module="perspective"} 15.67
module_execution_duration_seconds_count{module="perspective"} 1500

# Database query times
database_query_duration_seconds_bucket{pool="default",le="0.1"} 8934
database_query_duration_seconds_bucket{pool="default",le="0.5"} 8976
database_query_duration_seconds_sum{pool="default"} 234.56
database_query_duration_seconds_count{pool="default"} 9000
```

### Histogram Query Patterns

**Percentile Calculation**
```promql
# 95th percentile response time
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# 99th percentile database query time
histogram_quantile(0.99, rate(database_query_duration_seconds_bucket[5m]))
```

**Rate Analysis**
```promql
# Request rate (requests per second)
rate(http_request_duration_seconds_count[5m])

# Average response time
rate(http_request_duration_seconds_sum[5m]) / rate(http_request_duration_seconds_count[5m])
```

**SLA Monitoring**
```promql
# Percentage of requests faster than 100ms
(
  rate(http_request_duration_seconds_bucket{le="0.1"}[5m]) /
  rate(http_request_duration_seconds_count[5m])
) * 100
```

## Summary Metrics

### Definition and Characteristics

Summaries are similar to histograms but calculate quantiles on the client side:

```
# HELP rpc_duration_seconds RPC latency summary
# TYPE rpc_duration_seconds summary
rpc_duration_seconds{quantile="0.5"} 0.023
rpc_duration_seconds{quantile="0.9"} 0.076
rpc_duration_seconds{quantile="0.99"} 0.234
rpc_duration_seconds_sum 1423.0
rpc_duration_seconds_count 27547
```

**Key Components:**
- **Quantiles**: Pre-calculated percentiles (0.5 = median, 0.9 = 90th percentile)
- **Sum**: Total of all observed values
- **Count**: Total number of observations

### Summary vs Histogram Trade-offs

**Summary Advantages:**
- Accurate quantiles calculated on client
- Lower storage requirements (no buckets)
- Better for precise percentile monitoring

**Histogram Advantages:**  
- Server-side quantile calculation flexibility
- Aggregation across multiple instances
- Historical quantile analysis

## Metric Type Identification

### Recognizing Types in Output

**Counter Identification**
```bash
# Look for _total suffix and TYPE counter
curl -s http://gateway:8088/system/metrics | grep -A1 "TYPE.*counter"
```

**Gauge Identification**  
```bash
# Look for TYPE gauge
curl -s http://gateway:8088/system/metrics | grep -A1 "TYPE.*gauge"
```

**Histogram Identification**
```bash
# Look for _bucket, _sum, _count metrics
curl -s http://gateway:8088/system/metrics | grep -E "_bucket|_sum.*histogram|_count.*histogram"
```

### Type-Specific Analysis

**Validate Counter Behavior**
```bash
# Check that counter values only increase
curl -s http://gateway:8088/system/metrics | grep jvm_gc_collection_seconds_total
# Wait 30 seconds  
curl -s http://gateway:8088/system/metrics | grep jvm_gc_collection_seconds_total
# Values should be equal or higher, never lower
```

**Verify Gauge Fluctuation**
```bash
# Memory usage should fluctuate
watch -n 5 "curl -s http://gateway:8088/system/metrics | grep 'jvm_memory_bytes_used.*heap'"
```

## Query Function Selection

### Counter Functions

**Rate Functions** (for counters)
```promql
# Per-second rate over time window
rate(jvm_gc_collection_seconds_total[5m])

# Total increase over time window  
increase(jvm_gc_collection_seconds_total[1h])

# Instantaneous rate (1 sample)
irate(jvm_gc_collection_seconds_total[1m])
```

### Gauge Functions

**Aggregation Functions** (for gauges)
```promql
# Current value (no function needed)
jvm_memory_bytes_used{area="heap"}

# Average over time
avg_over_time(jvm_memory_bytes_used{area="heap"}[1h])

# Maximum over time
max_over_time(jvm_threads_current[1d])

# Change detection
delta(jvm_threads_current[10m])
```

## Best Practices by Type

### Counter Best Practices

1. **Always Use Rate Functions**: Raw counter values are rarely useful
2. **Choose Appropriate Time Windows**: Match window to desired resolution  
3. **Handle Counter Resets**: Use `increase()` for totals across resets
4. **Monitor Rate Trends**: Look for unusual increases in rate

### Gauge Best Practices

1. **Direct Value Monitoring**: Gauges represent current state
2. **Trend Analysis**: Use time-based functions for patterns
3. **Threshold Alerting**: Set alerts on absolute or percentage values
4. **Ratio Calculations**: Combine related gauges for utilization metrics

### Histogram Best Practices

1. **Use Appropriate Buckets**: Design buckets for expected value distribution
2. **Calculate Percentiles**: Use `histogram_quantile()` for SLA monitoring
3. **Monitor Both Rate and Latency**: Track volume and performance
4. **Aggregate Carefully**: Sum buckets across instances for fleet-wide percentiles

## Troubleshooting Metric Types

### Common Mistakes

**Using Counter Functions on Gauges**
```promql
# Wrong: rate() on gauge
rate(jvm_memory_bytes_used[5m])  # This will likely be near zero

# Right: gauge aggregation
avg_over_time(jvm_memory_bytes_used[5m])
```

**Using Gauge Functions on Counters**
```promql
# Wrong: direct counter value
jvm_gc_collection_seconds_total  # Raw cumulative value, not useful

# Right: rate calculation
rate(jvm_gc_collection_seconds_total[5m])
```

### Type Validation Queries

**Verify Counter Monotonicity**
```promql
# Should always be >= 0 (or absent during resets)
increase(jvm_gc_collection_seconds_total[1h]) < 0
```

**Check Gauge Reasonableness**
```promql
# Memory usage should be positive and <= max
jvm_memory_bytes_used{area="heap"} > jvm_memory_bytes_max{area="heap"}
```

## Next Steps

After understanding metric types:

1. **[Implement custom metrics](custom-metrics)** with appropriate types
2. **[Create effective dashboards](../examples/dashboard-creation)** using type-specific visualizations  
3. **[Set up proper alerting](../examples/alerting-setup)** based on metric type behavior