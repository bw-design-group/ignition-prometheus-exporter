# Metrics Filtering

Control which metrics are exposed through the Prometheus endpoint to optimize performance and reduce data volume while maintaining essential monitoring capabilities.

## Current Filtering Capabilities

### Default Behavior

The current module version exposes all metrics from Ignition's Dropwizard MetricRegistry without filtering:

- **JVM Metrics**: Memory, threads, garbage collection, class loading
- **System Metrics**: CPU, file system, network (when available)
- **Gateway Metrics**: Internal Ignition metrics from various modules
- **Custom Metrics**: Metrics registered via scripting functions

### Metric Categories

**Core JVM Metrics** (Always Recommended)
```
jvm_memory_bytes_used
jvm_memory_bytes_max
jvm_threads_current
jvm_threads_daemon
jvm_gc_collection_seconds_total
```

**Optional System Metrics**
```
process_cpu_seconds_total
process_start_time_seconds
process_open_fds
process_max_fds
```

## Client-Side Filtering

### Prometheus Scrape Configuration

Filter metrics during collection using Prometheus configuration:

```yaml
scrape_configs:
  - job_name: 'ignition-essential'
    static_configs:
      - targets: ['gateway:8088']
    metrics_path: '/system/metrics'
    metric_relabel_configs:
      # Keep only essential JVM metrics
      - source_labels: [__name__]
        regex: 'jvm_(memory_bytes_used|memory_bytes_max|threads_current|gc_collection_seconds_total)'
        action: keep
      
      # Drop histogram buckets for some metrics
      - source_labels: [__name__]
        regex: '.*_bucket'
        action: drop
        
      # Rename metrics for consistency
      - source_labels: [__name__]
        regex: 'jvm_memory_bytes_(.*)'
        target_label: __name__
        replacement: 'ignition_jvm_memory_${1}'
```

### Advanced Relabeling

**Filter by Metric Type:**
```yaml
metric_relabel_configs:
  # Keep only gauge and counter metrics
  - source_labels: [__name__]
    regex: '.*_(total|bucket|sum|count)$'
    action: drop
    
  # Keep only memory-related metrics
  - source_labels: [__name__]
    regex: '.*memory.*'
    action: keep
```

**Filter by Labels:**
```yaml
metric_relabel_configs:
  # Keep only heap memory metrics
  - source_labels: [area]
    regex: 'heap'
    action: keep
    
  # Drop specific garbage collectors
  - source_labels: [gc]
    regex: 'G1 Old Generation'
    action: drop
```

### Recording Rules

Create derived metrics from filtered data:

```yaml
# prometheus-rules.yml
groups:
  - name: ignition-derived
    rules:
      # Memory utilization percentage
      - record: ignition:memory_utilization_percent
        expr: |
          (
            jvm_memory_bytes_used{area="heap"} /
            jvm_memory_bytes_max{area="heap"}
          ) * 100
      
      # GC frequency (collections per minute)
      - record: ignition:gc_frequency_per_minute
        expr: |
          rate(jvm_gc_collection_seconds_count[5m]) * 60
      
      # Thread utilization trend
      - record: ignition:thread_growth_rate
        expr: |
          deriv(jvm_threads_current[10m])
```

## Server-Side Filtering (Future Enhancement)

### Planned Configuration Options

Future versions may support server-side filtering through gateway configuration:

```json
{
  "prometheus": {
    "metrics": {
      "enabled_categories": [
        "jvm.memory",
        "jvm.threads", 
        "jvm.gc",
        "ignition.gateway"
      ],
      "disabled_metrics": [
        "jvm_buffer_pool_*",
        "process_files_*"
      ],
      "custom_filters": {
        "jvm_memory": {
          "areas": ["heap", "nonheap"]
        }
      }
    }
  }
}
```

### Custom Metric Registration

Control custom metrics through scripting:

```python
# Gateway script example - selective metric registration
def register_custom_metrics():
    # Only register high-value custom metrics
    if system.util.getSystemFlags() & system.util.SYSTEM_FLAG_PRODUCTION:
        # Production metrics
        system.prometheus.gauge("ignition_production_uptime_hours") \
            .set(system.date.hoursBetween(system.util.getSystemProperty("startup.time"), system.date.now()))
        
        system.prometheus.counter("ignition_production_operations_total") \
            .labels({"type": "critical"}) \
            .increment()
    else:
        # Development metrics (more verbose)
        system.prometheus.gauge("ignition_debug_memory_detailed") \
            .labels({"component": "script_manager"}) \
            .set(get_script_manager_memory_usage())
```

## Performance Impact Analysis

### Metric Volume Assessment

**Measure current metric count:**
```bash
# Count total metrics
curl -s http://gateway:8088/system/metrics | grep -c "^[a-zA-Z_]"

# Count by prefix
curl -s http://gateway:8088/system/metrics | \
grep "^[a-zA-Z_]" | cut -d'{' -f1 | cut -d' ' -f1 | \
sort | uniq -c | sort -nr
```

**Analyze response size:**
```bash
# Measure unfiltered size
curl -s http://gateway:8088/system/metrics | wc -c

# Estimate filtered size (JVM metrics only)
curl -s http://gateway:8088/system/metrics | \
grep "jvm_" | wc -c
```

### Network Bandwidth Impact

**Calculate scrape overhead:**
```bash
# Bandwidth per scrape (assuming 15s interval)
RESPONSE_SIZE=$(curl -s http://gateway:8088/system/metrics | wc -c)
echo "Bandwidth per minute: $((RESPONSE_SIZE * 4)) bytes"
echo "Bandwidth per hour: $((RESPONSE_SIZE * 240)) bytes"
echo "Bandwidth per day: $((RESPONSE_SIZE * 5760)) bytes"
```

## Filtering Strategies

### Essential Metrics Strategy

**Minimal monitoring (production)**
```yaml
metric_relabel_configs:
  # Keep only critical health metrics
  - source_labels: [__name__]
    regex: 'jvm_(memory_bytes_used|memory_bytes_max|threads_current)'
    action: keep
  - source_labels: [__name__]
    regex: 'jvm_gc_collection_seconds_(total|count)'
    action: keep
  # Drop everything else
  - regex: '.*'
    action: drop
```

**Comprehensive monitoring (development)**
```yaml
metric_relabel_configs:
  # Drop only noisy metrics
  - source_labels: [__name__]
    regex: '.*_bucket$'
    action: drop
  # Keep everything else (default behavior)
```

### Use Case-Specific Filtering

**Memory-focused monitoring:**
```yaml
metric_relabel_configs:
  - source_labels: [__name__]
    regex: '(jvm_memory.*|jvm_gc.*|process_resident_memory.*)'
    action: keep
  - regex: '.*'
    action: drop
```

**Performance monitoring:**
```yaml
metric_relabel_configs:
  - source_labels: [__name__]
    regex: '(jvm_threads.*|jvm_gc.*|process_cpu.*|http_request.*)'
    action: keep
  - regex: '.*'
    action: drop
```

## Custom Filtering Implementation

### Gateway Script Filtering

```python
# Example script to selectively register metrics
def setup_production_metrics():
    """Register only production-relevant custom metrics"""
    
    # High-level system health
    memory_used = system.util.getAvailableMemory()
    system.prometheus.gauge("ignition_system_memory_available_bytes") \
        .set(memory_used)
    
    # Critical process counts
    active_clients = len(system.perspective.getSessionInfo())
    system.prometheus.gauge("ignition_active_perspective_sessions") \
        .set(active_clients)
    
    # Skip debug metrics in production
    if not system.util.getSystemFlags() & system.util.SYSTEM_FLAG_DEBUG:
        return
    
    # Debug-only metrics
    system.prometheus.gauge("ignition_debug_script_executions_total") \
        .labels({"scope": "gateway"}) \
        .increment()
```

### Module-Level Filtering

Future enhancement to filter at the collector level:

```java
// Planned implementation concept
public class FilteredDropwizardExports extends DropwizardExports {
    private final Set<String> allowedPrefixes;
    private final Set<String> blockedMetrics;
    
    public FilteredDropwizardExports(MetricRegistry registry, 
                                   FilterConfig config) {
        super(registry);
        this.allowedPrefixes = config.getAllowedPrefixes();
        this.blockedMetrics = config.getBlockedMetrics();
    }
    
    @Override
    public List<MetricFamilySamples> collect() {
        return super.collect().stream()
            .filter(this::shouldIncludeMetric)
            .collect(Collectors.toList());
    }
}
```

## Monitoring Filter Effectiveness

### Performance Metrics

**Track filtering impact:**
```bash
# Before filtering
time curl -s http://gateway:8088/system/metrics > /tmp/before.txt
BEFORE_SIZE=$(wc -c < /tmp/before.txt)
BEFORE_COUNT=$(grep -c "^[a-zA-Z_]" /tmp/before.txt)

# After applying Prometheus filtering (simulate)
curl -s http://gateway:8088/system/metrics | \
grep "jvm_memory\|jvm_threads\|jvm_gc" > /tmp/after.txt
AFTER_SIZE=$(wc -c < /tmp/after.txt)
AFTER_COUNT=$(grep -c "^[a-zA-Z_]" /tmp/after.txt)

echo "Size reduction: $((100 - AFTER_SIZE * 100 / BEFORE_SIZE))%"
echo "Metric reduction: $((100 - AFTER_COUNT * 100 / BEFORE_COUNT))%"
```

## Best Practices

### Filtering Guidelines

1. **Start Conservative**: Begin with minimal filtering, add more as needed
2. **Monitor Impact**: Track performance improvements from filtering
3. **Document Decisions**: Maintain list of filtered metrics and reasons
4. **Test Thoroughly**: Verify essential metrics remain available
5. **Plan for Growth**: Design filters that scale with additional modules

### Common Pitfalls

**Over-filtering**: Losing critical diagnostic information
```yaml
# Avoid: Too aggressive filtering
- regex: '.*'
  action: drop  # Drops everything!
```

**Under-filtering**: Not addressing performance issues
```yaml
# Better: Targeted filtering
- source_labels: [__name__]
  regex: '.*_(bucket|quantile)$'
  action: drop  # Remove histogram detail
```

## Next Steps

After configuring metrics filtering:

1. **[Optimize performance](performance-tuning)** for your filtering strategy
2. **[Configure Prometheus scraping](../prometheus-integration/scrape-configuration)** with filtered metrics
3. **[Monitor filtering effectiveness](../troubleshooting/performance)** over time