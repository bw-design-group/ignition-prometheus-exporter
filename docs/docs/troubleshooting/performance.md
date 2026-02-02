# Performance Troubleshooting

Comprehensive guide for diagnosing and resolving performance issues with the Prometheus Exporter Module.

## Performance Baseline Establishment

### Expected Performance Metrics

**Typical Performance Ranges**:
```
Metric Collection Response Time: 50-200ms
Memory Overhead: <10MB additional heap usage
CPU Impact: <1% during collection
Network Bandwidth: 10-50KB per scrape
Concurrent Scrapes: 10+ simultaneous requests supported
```

### Baseline Measurement

Establish performance baselines for your environment:

```bash
#!/bin/bash
# baseline-performance.sh

GATEWAY_URL="http://localhost:8088/system/metrics"
ITERATIONS=20

echo "Establishing performance baseline..."
echo "Gateway: $GATEWAY_URL"
echo "Iterations: $ITERATIONS"
echo ""

# Response time measurements
echo "=== Response Time Analysis ==="
TOTAL_TIME=0
MIN_TIME=999
MAX_TIME=0

for i in $(seq 1 $ITERATIONS); do
    TIME=$(curl -w '%{time_total}' -s "$GATEWAY_URL" -o /tmp/metrics_$i.txt)
    SIZE=$(wc -c < /tmp/metrics_$i.txt)
    METRICS=$(grep -c '^[a-zA-Z_]' /tmp/metrics_$i.txt)
    
    echo "Request $i: ${TIME}s, ${SIZE} bytes, ${METRICS} metrics"
    
    TOTAL_TIME=$(echo "$TOTAL_TIME + $TIME" | bc)
    
    if (( $(echo "$TIME < $MIN_TIME" | bc -l) )); then
        MIN_TIME=$TIME
    fi
    
    if (( $(echo "$TIME > $MAX_TIME" | bc -l) )); then
        MAX_TIME=$TIME
    fi
    
    rm /tmp/metrics_$i.txt
done

AVERAGE_TIME=$(echo "scale=3; $TOTAL_TIME / $ITERATIONS" | bc)

echo ""
echo "=== Performance Summary ==="
echo "Average Response Time: ${AVERAGE_TIME}s"
echo "Minimum Response Time: ${MIN_TIME}s" 
echo "Maximum Response Time: ${MAX_TIME}s"
echo "Total Requests: $ITERATIONS"

# Memory usage
echo ""
echo "=== Memory Usage ==="
HEAP_USED=$(curl -s "$GATEWAY_URL" | grep 'jvm_memory_bytes_used.*heap' | grep -o '[0-9.]*e[0-9]*')
HEAP_MAX=$(curl -s "$GATEWAY_URL" | grep 'jvm_memory_bytes_max.*heap' | grep -o '[0-9.]*e[0-9]*')

if [ ! -z "$HEAP_USED" ] && [ ! -z "$HEAP_MAX" ]; then
    HEAP_PERCENT=$(echo "scale=2; ($HEAP_USED / $HEAP_MAX) * 100" | bc)
    echo "Heap Usage: $HEAP_PERCENT%"
    echo "Heap Used: $HEAP_USED bytes"
    echo "Heap Max: $HEAP_MAX bytes"
fi
```

## Performance Issue Identification

### Response Time Problems

**Symptoms**: Metrics endpoint responding slowly (>5 seconds)

**Diagnostic Approach**:

```bash
# Detailed response time analysis
analyze_response_time() {
    local gateway_url="$1"
    
    echo "Analyzing response time components..."
    
    # Use curl with detailed timing
    curl -w "@curl-timing-format.txt" -s "$gateway_url" -o /tmp/metrics.txt
    
    echo "Response size: $(wc -c < /tmp/metrics.txt) bytes"
    echo "Metric count: $(grep -c '^[a-zA-Z_]' /tmp/metrics.txt)"
    
    # Check for specific slow components
    echo ""
    echo "Analyzing metric types..."
    echo "JVM metrics: $(grep -c '^jvm_' /tmp/metrics.txt)"
    echo "Process metrics: $(grep -c '^process_' /tmp/metrics.txt)"
    echo "Custom metrics: $(grep -c '^ignition_' /tmp/metrics.txt)"
    
    rm /tmp/metrics.txt
}

# Create curl timing format file
cat > curl-timing-format.txt << 'EOF'
     time_namelookup:  %{time_namelookup}s
        time_connect:  %{time_connect}s
     time_appconnect:  %{time_appconnect}s
    time_pretransfer:  %{time_pretransfer}s
       time_redirect:  %{time_redirect}s
  time_starttransfer:  %{time_starttransfer}s
          time_total:  %{time_total}s
EOF

analyze_response_time "http://gateway:8088/system/metrics"
```

**Common Causes & Solutions**:

1. **High Metric Cardinality**
   ```bash
   # Identify high-cardinality metrics
   curl -s http://gateway:8088/system/metrics | \
   grep '^[a-zA-Z_]' | cut -d'{' -f1 | sort | uniq -c | sort -nr | head -20
   
   # Look for metrics with many label combinations
   curl -s http://gateway:8088/system/metrics | \
   grep '^ignition_' | wc -l
   ```

2. **Memory Pressure**
   ```bash
   # Check if GC is impacting performance
   curl -s http://gateway:8088/system/metrics | \
   grep 'jvm_gc_collection_seconds{' | \
   awk '{print $2}' | \
   awk '{sum+=$1} END {print "Total GC time: " sum "s"}'
   ```

3. **Custom Metric Inefficiency**
   ```python
   # Profile custom metric registration
   import system.date
   
   def profile_custom_metrics():
       start = system.date.now()
       
       # Time expensive custom metric operations
       result = expensive_calculation()  # Your custom logic
       system.prometheus.gauge("expensive_metric").set(result)
       
       duration = system.date.secondsBetween(start, system.date.now())
       
       if duration > 1.0:  # Flag metrics taking >1 second
           logger = system.util.getLogger("PerformanceProfiler")
           logger.warn("Slow custom metric: %s seconds" % duration)
   ```

### Memory Usage Issues

**Symptoms**: Increasing gateway memory usage after module installation

**Memory Analysis Tools**:

```bash
# Monitor memory usage over time
monitor_memory_usage() {
    echo "Monitoring memory usage for 10 minutes..."
    
    for i in {1..20}; do
        TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')
        
        # Get JVM memory metrics
        HEAP_USED=$(curl -s http://gateway:8088/system/metrics | \
                   grep 'jvm_memory_bytes_used.*heap' | \
                   grep -o '[0-9.]*e[0-9]*')
        
        HEAP_MAX=$(curl -s http://gateway:8088/system/metrics | \
                  grep 'jvm_memory_bytes_max.*heap' | \
                  grep -o '[0-9.]*e[0-9]*')
        
        if [ ! -z "$HEAP_USED" ] && [ ! -z "$HEAP_MAX" ]; then
            HEAP_PCT=$(echo "scale=2; ($HEAP_USED / $HEAP_MAX) * 100" | bc)
            echo "$TIMESTAMP - Heap: $HEAP_PCT% ($HEAP_USED / $HEAP_MAX bytes)"
        fi
        
        # Get process memory from system
        PID=$(pgrep -f ignition)
        if [ ! -z "$PID" ]; then
            RSS=$(ps -p $PID -o rss --no-headers)
            VSZ=$(ps -p $PID -o vsz --no-headers)
            echo "$TIMESTAMP - Process RSS: ${RSS}KB, VSZ: ${VSZ}KB"
        fi
        
        sleep 30
    done
}

monitor_memory_usage
```

**Memory Leak Detection**:

```python
# Gateway script to detect memory leaks in custom metrics
def detect_memory_leaks():
    import system.util
    import system.date
    
    logger = system.util.getLogger("MemoryLeakDetector")
    
    # Track metric registry size over time
    registry = system.util.getGatewayContext().getMetricRegistry()
    metric_count = len(registry.getMetrics())
    
    logger.info("Current metric count: %d" % metric_count)
    
    # Check for metrics that should be bounded
    suspicious_patterns = [
        "user_id",      # Unbounded user identifiers
        "session_id",   # Session-specific metrics
        "timestamp",    # Time-based labels
        "ip_address"    # Client IP addresses
    ]
    
    # Scan for potentially problematic custom metrics
    for metric_name in registry.getMetrics().keySet():
        metric_str = str(metric_name)
        if metric_str.startswith("ignition_"):
            for pattern in suspicious_patterns:
                if pattern in metric_str.lower():
                    logger.warn("Suspicious metric pattern: %s contains %s" % (metric_str, pattern))

# Run memory leak detection
detect_memory_leaks()
```

### CPU Usage Problems

**Symptoms**: High CPU usage when metrics are being collected

**CPU Profiling**:

```bash
# Profile CPU usage during metrics collection
profile_cpu_usage() {
    local gateway_url="$1"
    local duration="${2:-60}"  # Default 60 seconds
    
    echo "Profiling CPU usage for ${duration} seconds..."
    
    # Get PID
    PID=$(pgrep -f ignition)
    if [ -z "$PID" ]; then
        echo "Error: Ignition process not found"
        return 1
    fi
    
    # Baseline CPU measurement
    echo "Taking baseline measurement..."
    CPU_BEFORE=$(ps -p $PID -o %cpu --no-headers)
    
    # Start continuous metrics collection
    echo "Starting metrics collection load..."
    for i in {1..10}; do
        curl -s "$gateway_url" > /dev/null &
    done
    
    sleep 5  # Allow load to stabilize
    
    # Measure CPU under load
    CPU_DURING=$(ps -p $PID -o %cpu --no-headers)
    echo "CPU usage during load: $CPU_DURING%"
    
    # Wait for requests to complete
    wait
    
    sleep 5  # Allow CPU to settle
    
    # Final measurement
    CPU_AFTER=$(ps -p $PID -o %cpu --no-headers)
    echo "CPU usage after load: $CPU_AFTER%"
    
    echo "CPU impact: $(echo "$CPU_DURING - $CPU_BEFORE" | bc)%"
}

profile_cpu_usage "http://gateway:8088/system/metrics"
```

**CPU Optimization Techniques**:

1. **Reduce Custom Metric Complexity**
   ```python
   # Avoid expensive calculations in metric collection
   # Bad: Complex calculation in metric collection
   def get_complex_metric():
       result = 0
       for i in range(10000):
           result += complex_calculation(i)
       return result
   
   system.prometheus.gauge("complex_metric").set(get_complex_metric())
   
   # Good: Pre-calculate and cache results
   if not hasattr(system, '_cached_complex_result'):
       system._cached_complex_result = get_complex_metric()
       system._cache_timestamp = system.date.now()
   
   # Refresh cache every 5 minutes
   if system.date.secondsBetween(system._cache_timestamp, system.date.now()) > 300:
       system._cached_complex_result = get_complex_metric()
       system._cache_timestamp = system.date.now()
   
   system.prometheus.gauge("complex_metric").set(system._cached_complex_result)
   ```

2. **Optimize Label Usage**
   ```python
   # Avoid dynamic label creation
   # Bad: Creates new labels each time
   user_count = get_user_count_by_id()
   for user_id, count in user_count.items():
       system.prometheus.gauge("user_sessions").labels({"user_id": user_id}).set(count)
   
   # Good: Use bounded label values
   user_count_by_role = aggregate_by_role(user_count)
   for role, count in user_count_by_role.items():
       system.prometheus.gauge("user_sessions").labels({"role": role}).set(count)
   ```

## Network Performance Issues

### High Bandwidth Usage

**Symptoms**: Metrics endpoint consuming excessive network bandwidth

**Bandwidth Analysis**:

```bash
# Measure bandwidth usage
measure_bandwidth_usage() {
    local gateway_url="$1"
    local scrape_interval="${2:-30}"  # seconds
    local duration="${3:-300}"        # 5 minutes default
    
    echo "Measuring bandwidth usage over ${duration} seconds..."
    
    TOTAL_BYTES=0
    SCRAPE_COUNT=0
    
    END_TIME=$(($(date +%s) + duration))
    
    while [ $(date +%s) -lt $END_TIME ]; do
        RESPONSE_SIZE=$(curl -s "$gateway_url" | wc -c)
        TOTAL_BYTES=$((TOTAL_BYTES + RESPONSE_SIZE))
        SCRAPE_COUNT=$((SCRAPE_COUNT + 1))
        
        echo "Scrape $SCRAPE_COUNT: $RESPONSE_SIZE bytes"
        sleep $scrape_interval
    done
    
    AVERAGE_SIZE=$((TOTAL_BYTES / SCRAPE_COUNT))
    BYTES_PER_SECOND=$((TOTAL_BYTES / duration))
    
    echo ""
    echo "=== Bandwidth Analysis ==="
    echo "Total bytes transferred: $TOTAL_BYTES"
    echo "Number of scrapes: $SCRAPE_COUNT"
    echo "Average response size: $AVERAGE_SIZE bytes"
    echo "Bandwidth usage: $BYTES_PER_SECOND bytes/second"
    echo "Daily bandwidth estimate: $((BYTES_PER_SECOND * 86400)) bytes/day"
}

measure_bandwidth_usage "http://gateway:8088/system/metrics"
```

**Bandwidth Optimization**:

1. **Enable Compression**
   ```bash
   # Test compression effectiveness
   UNCOMPRESSED=$(curl -s http://gateway:8088/system/metrics | wc -c)
   COMPRESSED=$(curl -s -H "Accept-Encoding: gzip" http://gateway:8088/system/metrics | wc -c)
   
   COMPRESSION_RATIO=$(echo "scale=2; (1 - $COMPRESSED / $UNCOMPRESSED) * 100" | bc)
   echo "Compression ratio: $COMPRESSION_RATIO%"
   
   # Configure Prometheus to request compression
   # prometheus.yml:
   scrape_configs:
     - job_name: 'ignition-gateway'
       static_configs:
         - targets: ['gateway:8088']
       # Prometheus automatically requests gzip compression
   ```

2. **Metric Filtering**
   ```yaml
   # Filter metrics to reduce bandwidth
   scrape_configs:
     - job_name: 'ignition-essential'
       static_configs:
         - targets: ['gateway:8088']
       metric_relabel_configs:
         # Keep only essential metrics
         - source_labels: [__name__]
           regex: 'jvm_(memory_bytes_used|memory_bytes_max|threads_current|gc_collection_seconds_total)'
           action: keep
         # Drop everything else
         - regex: '.*'
           action: drop
   ```

### Connection Pooling Issues

**Symptoms**: Connection timeouts or "too many open files" errors

**Connection Analysis**:

```bash
# Monitor connection usage
monitor_connections() {
    local gateway_port="8088"
    local duration="${1:-60}"
    
    echo "Monitoring connections for ${duration} seconds..."
    
    for i in $(seq 1 $duration); do
        TIMESTAMP=$(date '+%H:%M:%S')
        ESTABLISHED=$(netstat -an | grep ":$gateway_port.*ESTABLISHED" | wc -l)
        TIME_WAIT=$(netstat -an | grep ":$gateway_port.*TIME_WAIT" | wc -l)
        CLOSE_WAIT=$(netstat -an | grep ":$gateway_port.*CLOSE_WAIT" | wc -l)
        
        echo "$TIMESTAMP - EST: $ESTABLISHED, TIME_WAIT: $TIME_WAIT, CLOSE_WAIT: $CLOSE_WAIT"
        
        sleep 1
    done
}

monitor_connections
```

**Connection Optimization**:

```yaml
# Configure Prometheus for efficient connection usage
scrape_configs:
  - job_name: 'ignition-gateway'
    static_configs:
      - targets: ['gateway:8088']
    # Use longer scrape intervals to reduce connection pressure
    scrape_interval: 60s
    scrape_timeout: 30s
    # Enable HTTP/2 if supported
    honor_timestamps: false
```

## Scaling Performance Issues

### Multiple Gateway Monitoring

**Symptoms**: Performance degradation when monitoring many gateways

**Scaling Analysis**:

```bash
# Test performance with multiple targets
test_multiple_gateways() {
    local base_url="$1"
    local max_gateways="${2:-10}"
    
    echo "Testing performance with multiple gateways (up to $max_gateways)..."
    
    for gateway_count in $(seq 1 $max_gateways); do
        echo "Testing with $gateway_count gateways..."
        
        START_TIME=$(date +%s.%N)
        
        # Simulate concurrent scraping
        for i in $(seq 1 $gateway_count); do
            curl -s "$base_url/system/metrics" > /dev/null &
        done
        
        wait  # Wait for all requests to complete
        
        END_TIME=$(date +%s.%N)
        DURATION=$(echo "$END_TIME - $START_TIME" | bc)
        
        echo "  $gateway_count gateways: ${DURATION}s total time"
        echo "  Average per gateway: $(echo "scale=3; $DURATION / $gateway_count" | bc)s"
        echo ""
    done
}

test_multiple_gateways "http://gateway"
```

**Scaling Solutions**:

1. **Federation Architecture**
   ```yaml
   # Local Prometheus instances for edge locations
   # Central Prometheus for aggregation
   scrape_configs:
     - job_name: 'federate-edge-gateways'
       honor_labels: true
       metrics_path: '/federate'
       params:
         'match[]':
           - '{job="ignition-gateway"}'
       static_configs:
         - targets: ['prometheus-edge-1:9090', 'prometheus-edge-2:9090']
   ```

2. **Load Balancing**
   ```yaml
   # Distribute gateway monitoring across Prometheus instances
   # prometheus-1.yml
   scrape_configs:
     - job_name: 'ignition-gateways-group-1'
       static_configs:
         - targets: ['gateway-1:8088', 'gateway-2:8088']
   
   # prometheus-2.yml  
   scrape_configs:
     - job_name: 'ignition-gateways-group-2'
       static_configs:
         - targets: ['gateway-3:8088', 'gateway-4:8088']
   ```

### High-Frequency Monitoring

**Symptoms**: Performance impact from frequent metric collection

**Frequency Analysis**:

```bash
# Test different scrape intervals
test_scrape_frequencies() {
    local gateway_url="$1"
    
    for interval in 5 10 15 30 60 120; do
        echo "Testing ${interval}s scrape interval..."
        
        TOTAL_TIME=0
        ITERATIONS=10
        
        for i in $(seq 1 $ITERATIONS); do
            START_TIME=$(date +%s.%N)
            curl -s "$gateway_url" > /dev/null
            END_TIME=$(date +%s.%N)
            
            DURATION=$(echo "$END_TIME - $START_TIME" | bc)
            TOTAL_TIME=$(echo "$TOTAL_TIME + $DURATION" | bc)
            
            sleep $interval
        done
        
        AVERAGE_TIME=$(echo "scale=3; $TOTAL_TIME / $ITERATIONS" | bc)
        echo "  Average response time: ${AVERAGE_TIME}s"
        echo "  Overhead ratio: $(echo "scale=3; $AVERAGE_TIME / $interval" | bc)"
        echo ""
    done
}

test_scrape_frequencies "http://gateway:8088/system/metrics"
```

## Performance Monitoring and Alerting

### Performance Metrics Tracking

Monitor the performance of the monitoring system itself:

```yaml
# Monitor metrics collection performance
groups:
  - name: prometheus-exporter.performance
    rules:
      - alert: SlowMetricsCollection
        expr: scrape_duration_seconds{job="ignition-gateway"} > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Slow metrics collection on {{ $labels.instance }}"
          description: "Metrics collection taking {{ $value }}s (>5s)"

      - alert: LargeMetricsResponse
        expr: scrape_samples_scraped{job="ignition-gateway"} > 1000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Large metrics response from {{ $labels.instance }}"
          description: "{{ $value }} metrics collected (>1000)"

      - alert: HighMetricCardinality
        expr: increase(prometheus_tsdb_symbol_table_size_bytes[1h]) > 10000000
        for: 30m
        labels:
          severity: warning
        annotations:
          summary: "High metric cardinality growth"
          description: "Metric cardinality growing rapidly"
```

### Automated Performance Tuning

```bash
#!/bin/bash
# auto-tune-performance.sh

GATEWAY_URL="http://localhost:8088/system/metrics"
PROMETHEUS_CONFIG="/etc/prometheus/prometheus.yml"

echo "Starting automated performance tuning..."

# Measure current performance
CURRENT_TIME=$(curl -w '%{time_total}' -s "$GATEWAY_URL" -o /dev/null)
CURRENT_SIZE=$(curl -s "$GATEWAY_URL" | wc -c)
CURRENT_METRICS=$(curl -s "$GATEWAY_URL" | grep -c '^[a-zA-Z_]')

echo "Current performance:"
echo "  Response time: ${CURRENT_TIME}s"
echo "  Response size: $CURRENT_SIZE bytes"
echo "  Metric count: $CURRENT_METRICS"

# Determine optimal scrape interval
if (( $(echo "$CURRENT_TIME > 2.0" | bc -l) )); then
    RECOMMENDED_INTERVAL="60s"
elif (( $(echo "$CURRENT_TIME > 1.0" | bc -l) )); then
    RECOMMENDED_INTERVAL="30s"
else
    RECOMMENDED_INTERVAL="15s"
fi

echo "Recommended scrape interval: $RECOMMENDED_INTERVAL"

# Check if high cardinality metrics are present
HIGH_CARDINALITY=$(curl -s "$GATEWAY_URL" | grep '^ignition_' | wc -l)
if [ "$HIGH_CARDINALITY" -gt 100 ]; then
    echo "Warning: High cardinality custom metrics detected ($HIGH_CARDINALITY)"
    echo "Consider implementing metric filtering"
fi

# Generate optimized Prometheus configuration
cat > "$PROMETHEUS_CONFIG.optimized" << EOF
global:
  scrape_interval: $RECOMMENDED_INTERVAL
  evaluation_interval: $RECOMMENDED_INTERVAL

scrape_configs:
  - job_name: 'ignition-gateway-optimized'
    static_configs:
      - targets: ['gateway:8088']
    metrics_path: '/system/metrics'
    scrape_interval: $RECOMMENDED_INTERVAL
    scrape_timeout: $(echo "$CURRENT_TIME * 2" | bc)s
EOF

echo "Optimized configuration written to $PROMETHEUS_CONFIG.optimized"
```

## Performance Best Practices Summary

### Do's and Don'ts

**DO:**
- Monitor baseline performance metrics
- Use appropriate scrape intervals (30-60s for production)
- Enable compression for bandwidth optimization
- Implement metric filtering for large deployments
- Cache expensive custom metric calculations
- Use bounded label cardinality

**DON'T:**
- Scrape too frequently (&lt;15s intervals)
- Create unbounded label values
- Perform expensive calculations during metric collection
- Ignore memory usage growth
- Skip performance testing before production deployment

### Performance Checklist

Before deploying to production:

- [ ] Baseline performance measurements taken
- [ ] Response time &lt;2 seconds under normal load
- [ ] Memory usage increase &lt;50MB after module installation
- [ ] CPU impact &lt;5% during metric collection
- [ ] Network bandwidth usage acceptable
- [ ] Concurrent scrape performance tested
- [ ] Custom metrics optimized for cardinality
- [ ] Monitoring alerts configured for performance degradation

## Next Steps

After resolving performance issues:

1. **[Implement monitoring](../examples/basic-monitoring)** for performance metrics
2. **[Set up alerts](../examples/alerting-setup)** for performance degradation
3. **[Document performance baselines](common-issues)** for future reference