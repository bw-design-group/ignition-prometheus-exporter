# Dropwizard Metrics

The Prometheus Exporter Module leverages Ignition's built-in Dropwizard MetricRegistry to expose comprehensive JVM and system metrics without additional overhead.

## Dropwizard Integration Architecture

### How It Works

The module uses the `DropwizardExports` collector from the Prometheus Java client library to automatically convert Dropwizard metrics to Prometheus format:

```java
// From PrometheusExporterGatewayHook.java:66-67
MetricRegistry metricRegistry = context.getMetricRegistry();
collectorRegistry.register(new DropwizardExports(metricRegistry));
```

### Automatic Metric Discovery

The integration provides zero-configuration metric collection:

- **No Manual Registration**: All metrics automatically discovered from Ignition's MetricRegistry
- **Real-Time Updates**: Metrics reflect current gateway state
- **Type Preservation**: Maintains semantic meaning of different metric types
- **Label Mapping**: Dropwizard metric names converted to Prometheus labels

## Standard JVM Metrics

### Memory Metrics

**Heap Memory Usage**
```
# HELP jvm_memory_bytes_used Used bytes of a given JVM memory area
# TYPE jvm_memory_bytes_used gauge
jvm_memory_bytes_used{area="heap"} 1.25663e+08

# HELP jvm_memory_bytes_max Max bytes of a given JVM memory area
# TYPE jvm_memory_bytes_max gauge
jvm_memory_bytes_max{area="heap"} 2.147484e+09
```

**Memory Pool Details**
```
# Non-heap memory areas
jvm_memory_bytes_used{area="nonheap"} 9.1234567e+07
jvm_memory_bytes_committed{area="heap"} 2.68435456e+08
jvm_memory_bytes_committed{area="nonheap"} 9.8765432e+07
```

**Key Memory Metrics:**
- `jvm_memory_bytes_used{area="heap"}` - Current heap memory usage
- `jvm_memory_bytes_max{area="heap"}` - Maximum heap memory available
- `jvm_memory_bytes_committed{area="heap"}` - Memory guaranteed to be available
- `jvm_memory_bytes_used{area="nonheap"}` - Method area, code cache, compressed class space

### Thread Metrics

**Thread Pool Information**
```
# HELP jvm_threads_current Current thread count of a JVM
# TYPE jvm_threads_current gauge
jvm_threads_current 147

# HELP jvm_threads_daemon Daemon thread count of a JVM
# TYPE jvm_threads_daemon gauge
jvm_threads_daemon 24
```

**Complete Thread Metrics:**
- `jvm_threads_current` - Total number of live threads
- `jvm_threads_daemon` - Number of daemon threads
- `jvm_threads_peak` - Peak number of live threads since JVM start
- `jvm_threads_started_total` - Total threads created and started since JVM start
- `jvm_threads_deadlocked` - Number of deadlocked threads
- `jvm_threads_deadlocked_monitor` - Number of threads deadlocked waiting for monitors

### Garbage Collection Metrics

**GC Collection Statistics**
```
# HELP jvm_gc_collection_seconds_total Time spent in a given JVM garbage collector in seconds
# TYPE jvm_gc_collection_seconds_total counter
jvm_gc_collection_seconds_total{gc="G1 Young Generation"} 2.847

# HELP jvm_gc_collection_seconds_count Total number of collections that have occurred
# TYPE jvm_gc_collection_seconds_count counter
jvm_gc_collection_seconds_count{gc="G1 Young Generation"} 156
```

**GC Metric Analysis:**
- `jvm_gc_collection_seconds_total{gc="G1 Young Generation"}` - Total time spent in young generation GC
- `jvm_gc_collection_seconds_total{gc="G1 Old Generation"}` - Total time spent in old generation GC
- `jvm_gc_collection_seconds_count` - Number of GC collections performed

### Class Loading Metrics

**Class Loading Activity**
```
# HELP jvm_classes_loaded The number of classes that are currently loaded in the JVM
# TYPE jvm_classes_loaded gauge
jvm_classes_loaded 18247

# HELP jvm_classes_loaded_total The total number of classes that have been loaded since the JVM has started
# TYPE jvm_classes_loaded_total counter
jvm_classes_loaded_total 18247
```

**Class Metrics:**
- `jvm_classes_loaded` - Currently loaded classes
- `jvm_classes_loaded_total` - Total classes loaded since JVM start
- `jvm_classes_unloaded_total` - Total classes unloaded since JVM start

## System-Level Metrics

### Process Metrics

**Process Resource Usage**
```
# HELP process_cpu_seconds_total Total user and system CPU time spent in seconds
# TYPE process_cpu_seconds_total counter
process_cpu_seconds_total 1847.23

# HELP process_start_time_seconds Start time of the process since unix epoch in seconds
# TYPE process_start_time_seconds gauge
process_start_time_seconds 1.70434123e+09
```

**Process Information:**
- `process_cpu_seconds_total` - CPU time consumed by the process
- `process_start_time_seconds` - Process start timestamp
- `process_open_fds` - Number of open file descriptors (Linux/macOS)
- `process_max_fds` - Maximum number of file descriptors (Linux/macOS)
- `process_virtual_memory_bytes` - Virtual memory size
- `process_resident_memory_bytes` - Resident memory size

### File System Metrics

**Disk Usage (when available)**
```
# HELP jvm_buffer_pool_used_bytes Used bytes of a given JVM buffer pool
# TYPE jvm_buffer_pool_used_bytes gauge
jvm_buffer_pool_used_bytes{pool="direct"} 16384

# HELP jvm_buffer_pool_capacity_bytes Bytes capacity of a given JVM buffer pool
# TYPE jvm_buffer_pool_capacity_bytes gauge
jvm_buffer_pool_capacity_bytes{pool="direct"} 16384
```

## Ignition-Specific Metrics

### Gateway Internal Metrics

Ignition registers additional metrics in its MetricRegistry that become available through the exporter:

**Connection Pool Metrics**
```
# Example metrics that may be present
gateway_database_connections_active{pool="default"} 5
gateway_database_connections_idle{pool="default"} 15
gateway_database_connection_requests_total{pool="default"} 1247
```

**Module Performance Metrics**
```
# Module-specific performance counters
gateway_module_execution_time_seconds{module="perspective"} 0.023
gateway_module_errors_total{module="perspective"} 2
```

### Tag System Metrics

**Tag Database Operations**
```
# Tag database performance metrics
gateway_tag_reads_total 12847
gateway_tag_writes_total 8394
gateway_tag_subscriptions_active 156
```

**Tag Quality Metrics**
```
# Tag quality distribution
gateway_tag_quality_good_total 12234
gateway_tag_quality_bad_total 45  
gateway_tag_quality_uncertain_total 12
```

## Metric Naming Conventions

### Dropwizard to Prometheus Conversion

The `DropwizardExports` collector automatically converts Dropwizard naming to Prometheus format:

**Timer Metrics Conversion**
```
Dropwizard Timer: com.company.service.request.timer
Prometheus Output:
- com_company_service_request_timer_seconds (histogram)
- com_company_service_request_timer_seconds_count
- com_company_service_request_timer_seconds_sum
```

**Histogram Metrics Conversion**
```
Dropwizard Histogram: com.company.service.response.size
Prometheus Output:
- com_company_service_response_size (histogram)
- com_company_service_response_size_count
- com_company_service_response_size_sum
```

### Standard Metric Prefixes

Common prefixes in exported metrics:

- `jvm_*` - JVM-related metrics (memory, threads, GC, classes)
- `process_*` - Process-level system metrics
- `gateway_*` - Ignition gateway-specific metrics
- `module_*` - Individual module performance metrics
- Custom prefixes from application-specific metrics

## Metric Collection Performance

### Collection Efficiency

**Zero-Copy Architecture**
The Dropwizard integration provides efficient metric collection:

- **Direct Registry Access**: No metric duplication or copying
- **Lazy Evaluation**: Metrics computed only during scrape requests
- **Thread-Safe Operations**: Concurrent access supported without blocking
- **Memory Efficient**: Reference-based access to existing metric objects

**Performance Characteristics**
```bash
# Typical collection performance
curl -w "%{time_total}\n" -s http://gateway:8088/system/metrics > /dev/null
# Expected: 0.050-0.200 seconds for 100-500 metrics
```

### Scaling Considerations

**Metric Volume Growth**
As Ignition deployments grow, metric count scales with:
- Number of installed modules
- Active database connections
- Tag system complexity
- Custom application metrics

**Performance Impact**
```bash
# Monitor metric collection overhead
time curl -s http://gateway:8088/system/metrics | wc -l
# Track response time vs metric count correlation
```

## Troubleshooting Dropwizard Integration

### Missing Metrics

**Common Causes:**
1. **MetricRegistry Not Initialized**: Gateway not fully started
2. **Module Registration Issues**: Dropwizard exports not registered properly
3. **Metric Lifecycle**: Some metrics only appear after specific operations

**Diagnostic Steps:**
```bash
# Verify basic JVM metrics are present
curl -s http://gateway:8088/system/metrics | grep jvm_memory_bytes_used

# Check for any metrics at all
curl -s http://gateway:8088/system/metrics | grep -c "TYPE.*gauge\|TYPE.*counter"

# Look for error indicators
curl -s http://gateway:8088/system/metrics | grep -i error
```

### Metric Quality Issues

**Inconsistent Values**
```bash
# Check for metrics with invalid values
curl -s http://gateway:8088/system/metrics | grep -E "NaN|Inf|-Inf"

# Verify timestamp consistency (most metrics shouldn't have timestamps)
curl -s http://gateway:8088/system/metrics | grep -E "^[a-zA-Z_].*[0-9]+\s+[0-9]{10}"
```

**Label Problems**
```bash
# Check for malformed labels
curl -s http://gateway:8088/system/metrics | grep -E "\{[^}]*\{|\}[^,}]*\}"

# Verify label value formatting
curl -s http://gateway:8088/system/metrics | grep -E 'area="[^"]*"'
```

## Best Practices

### Monitoring Strategy

1. **Focus on Key Metrics**: Prioritize JVM memory, GC, and thread metrics
2. **Understand Baselines**: Establish normal operating ranges for each metric
3. **Monitor Trends**: Use time-series analysis for capacity planning
4. **Correlate Metrics**: Analyze relationships between memory, CPU, and GC activity

### Performance Optimization

1. **Regular Monitoring**: Track metric collection performance over time  
2. **Threshold Setting**: Configure appropriate alert thresholds based on historical data
3. **Capacity Planning**: Use memory and thread trend analysis for resource planning
4. **GC Tuning**: Optimize JVM garbage collection based on metric insights

## Next Steps

After understanding Dropwizard metrics:

1. **[Learn Prometheus metric types](metric-types)** for proper interpretation
2. **[Implement custom metrics](custom-metrics)** for application-specific monitoring
3. **[Create targeted dashboards](../examples/dashboard-creation)** for key metrics