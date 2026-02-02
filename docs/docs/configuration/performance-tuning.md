# Performance Tuning

Optimize the Prometheus Exporter Module for high-performance operation in production environments with minimal impact on gateway resources.

## Performance Characteristics

### Baseline Performance Metrics

**Resource Usage (Typical Installation)**
- **Memory Overhead**: &lt;5MB additional heap usage
- **CPU Impact**: &lt;1% during metric collection
- **Network Bandwidth**: 10-50KB per scrape (depends on metric count)
- **Response Time**: 10-100ms typical response time

**Scalability Factors**
- **Metric Count**: Linear relationship with response time and bandwidth
- **Scrape Frequency**: Higher frequency increases CPU load
- **Concurrent Scrapers**: Multiple simultaneous requests supported
- **Gateway Load**: Performance degrades under high gateway CPU usage

### Performance Monitoring

**Measure Current Performance:**

```bash
# Response time measurement
time curl -s http://gateway:8088/system/metrics > /dev/null

# Response size measurement  
curl -s http://gateway:8088/system/metrics | wc -c

# Metric count measurement
curl -s http://gateway:8088/system/metrics | grep -c "^[a-zA-Z_]"

# Throughput test
for i in {1..10}; do
  time curl -s http://gateway:8088/system/metrics > /dev/null 2>&1
done
```

## Gateway-Level Optimization

### JVM Memory Tuning

**Heap Size Configuration**

For gateways with metrics export, consider increased heap:

```bash
# /etc/systemd/system/ignition.service.d/override.conf
[Service]
Environment="JAVA_OPTS=-Xmx4g -Xms2g -XX:+UseG1GC"
```

**Garbage Collection Optimization**

G1GC settings for low-latency metrics collection:

```bash
JAVA_OPTS="-XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:G1HeapRegionSize=16m \
  -XX:+G1UseAdaptiveIHOP \
  -XX:G1MixedGCCountTarget=8"
```

### Dropwizard Registry Optimization

**Registry Configuration**

The module leverages Ignition's existing MetricRegistry efficiently:

- **No Duplication**: Direct access to existing metrics
- **Lazy Collection**: Metrics collected only during scrape requests
- **Thread-Safe Access**: Concurrent scrapes supported without locking

**Custom Metric Best Practices**

```python
# Efficient custom metric registration
def register_efficient_metrics():
    # Use static metric instances to avoid recreation
    if not hasattr(system, '_custom_metrics_registered'):
        # One-time registration
        system.prometheus.gauge("ignition_custom_uptime_seconds") \
            .set_function(lambda: system.date.secondsBetween(
                system.util.getSystemProperty("startup.time"), 
                system.date.now()
            ))
        
        system._custom_metrics_registered = True
    
    # Update existing metrics (efficient)
    system.prometheus.counter("ignition_script_executions_total") \
        .labels({"scope": "gateway"}) \
        .increment()
```

## Scrape Configuration Optimization

### Optimal Scrape Intervals

**Recommended Intervals by Use Case:**

```yaml
scrape_configs:
  # Production monitoring - balanced performance/accuracy
  - job_name: 'ignition-production'
    scrape_interval: 30s
    scrape_timeout: 10s
    static_configs:
      - targets: ['gateway:8088']
    
  # Development monitoring - higher frequency
  - job_name: 'ignition-development' 
    scrape_interval: 15s
    scrape_timeout: 5s
    static_configs:
      - targets: ['gateway-dev:8088']
    
  # Critical alerting - high frequency
  - job_name: 'ignition-critical'
    scrape_interval: 5s
    scrape_timeout: 3s
    static_configs:
      - targets: ['gateway-prod:8088']
    metric_relabel_configs:
      # Only critical metrics for high-frequency scraping
      - source_labels: [__name__]
        regex: 'jvm_(memory_bytes_used|threads_current)'
        action: keep
```

### HTTP Optimization

**Connection Reuse:**

```yaml
scrape_configs:
  - job_name: 'ignition'
    static_configs:
      - targets: ['gateway:8088']
    # Enable HTTP keep-alive
    honor_timestamps: false
    # Optimize for network efficiency
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: '(.+)'
        target_label: instance
        replacement: 'ignition-gateway'
```

**Compression Configuration:**

```bash
# Verify compression support
curl -H "Accept-Encoding: gzip" -v http://gateway:8088/system/metrics 2>&1 | \
grep "Content-Encoding"

# Measure compression benefit
UNCOMPRESSED=$(curl -s http://gateway:8088/system/metrics | wc -c)
COMPRESSED=$(curl -s -H "Accept-Encoding: gzip" http://gateway:8088/system/metrics | wc -c)
echo "Compression ratio: $((100 - COMPRESSED * 100 / UNCOMPRESSED))%"
```

## Network Performance Optimization

### Load Balancing Considerations

**Multiple Prometheus Instances:**

```yaml
# Round-robin scraping to distribute load
scrape_configs:
  - job_name: 'ignition-balanced'
    static_configs:
      - targets: ['gateway:8088']
    scrape_interval: 60s  # Longer interval with multiple scrapers
    
  # Separate instance for different metrics
  - job_name: 'ignition-jvm-only'
    static_configs:
      - targets: ['gateway:8088']  
    scrape_interval: 10s  # High frequency for JVM metrics
    metric_relabel_configs:
      - source_labels: [__name__]
        regex: 'jvm_.*'
        action: keep
```

### Federation Optimization

**Hierarchical Collection:**

```yaml
# Local Prometheus (edge)
scrape_configs:
  - job_name: 'local-ignition'
    static_configs:
      - targets: ['gateway-local:8088']
    scrape_interval: 15s

# Central Prometheus (datacenter) 
scrape_configs:
  - job_name: 'federate-ignition'
    honor_labels: true
    metrics_path: '/federate'
    params:
      'match[]':
        - '{job="local-ignition"}'
    static_configs:
      - targets: ['prometheus-edge:9090']
    scrape_interval: 60s  # Lower frequency for federated data
```

## Metric-Specific Optimizations

### High-Cardinality Metrics

**Identify High-Cardinality Metrics:**

```bash
# Find metrics with many label combinations
curl -s http://gateway:8088/system/metrics | \
grep "^[a-zA-Z_]" | cut -d'{' -f1 | sort | uniq -c | sort -nr | head -10
```

**Cardinality Control:**

```python
# Limit label cardinality in custom metrics
def register_controlled_metrics():
    # Bad: Unbounded cardinality
    # system.prometheus.counter("requests_total").labels({"user": user_id}).increment()
    
    # Good: Bounded cardinality
    user_type = "admin" if is_admin(user_id) else "user"
    system.prometheus.counter("requests_total") \
        .labels({"user_type": user_type}) \
        .increment()
```

### Memory-Efficient Metrics

**Optimize Metric Storage:**

```python
# Use appropriate metric types
def register_memory_efficient_metrics():
    # Use counters for monotonically increasing values
    system.prometheus.counter("operations_total") \
        .labels({"type": "read"}) \
        .increment()
    
    # Use gauges for current state values  
    system.prometheus.gauge("active_connections") \
        .set(get_active_connection_count())
    
    # Avoid histograms unless distribution analysis is needed
    # histogram = system.prometheus.histogram("response_time_seconds")
    # Better: Use summary or simple gauge for average
    system.prometheus.gauge("response_time_average_seconds") \
        .set(calculate_average_response_time())
```

## Monitoring Performance Impact

### Gateway Performance Metrics

**Monitor Gateway Health During Scraping:**

```bash
# CPU usage during scrape
(curl -s http://gateway:8088/system/metrics > /dev/null &
 top -p $(pgrep -f ignition) -b -n 1 | grep ignition)

# Memory usage tracking
ps -p $(pgrep -f ignition) -o pid,vsz,rss,pmem,pcpu,comm

# Network connection monitoring
netstat -an | grep :8088 | wc -l
```

**Automated Performance Testing:**

```bash
#!/bin/bash
# performance-test.sh

echo "Starting Prometheus endpoint performance test"
GATEWAY="gateway:8088"
ITERATIONS=100

# Baseline performance
echo "Testing baseline performance..."
TOTAL_TIME=0
for i in $(seq 1 $ITERATIONS); do
    START=$(date +%s.%N)
    curl -s http://$GATEWAY/system/metrics > /dev/null
    END=$(date +%s.%N)
    TIME=$(echo "$END - $START" | bc)
    TOTAL_TIME=$(echo "$TOTAL_TIME + $TIME" | bc)
done

AVERAGE=$(echo "scale=3; $TOTAL_TIME / $ITERATIONS" | bc)
echo "Average response time: ${AVERAGE}s"

# Concurrent load test
echo "Testing concurrent performance..."
ab -n 100 -c 10 http://$GATEWAY/system/metrics
```

### Prometheus Server Performance

**Monitor Prometheus Ingestion:**

```bash
# Check Prometheus metrics about itself
curl -s http://prometheus:9090/metrics | grep prometheus_tsdb_symbol_table_size_bytes
curl -s http://prometheus:9090/metrics | grep prometheus_rule_evaluation_duration_seconds
curl -s http://prometheus:9090/metrics | grep prometheus_tsdb_head_series
```

## Troubleshooting Performance Issues

### Common Performance Problems

**Slow Response Times**

```bash
# Diagnose slow responses
time curl -v http://gateway:8088/system/metrics 2>&1 | grep "Total time"

# Check for network issues
ping -c 5 gateway-host
traceroute gateway-host

# Verify gateway load
ssh gateway-host 'top -b -n 1 | grep "load average"'
```

**High Memory Usage**

```bash
# Check gateway memory usage
ssh gateway-host 'free -h && ps aux | grep ignition | grep -v grep'

# Monitor metric count growth
watch -n 5 "curl -s http://gateway:8088/system/metrics | grep -c '^[a-zA-Z_]'"
```

**Connection Issues**

```bash
# Check connection limits
ss -tuln | grep :8088
netstat -an | grep :8088 | grep ESTABLISHED | wc -l

# Test connection pooling
for i in {1..10}; do
  curl -w "@curl-format.txt" -s http://gateway:8088/system/metrics > /dev/null
done
```

### Performance Recovery

**Emergency Performance Restoration:**

```bash
# Reduce scrape frequency temporarily
# Edit prometheus.yml to increase scrape_interval to 60s

# Clear metric filters temporarily  
# Remove metric_relabel_configs to ensure all metrics accessible

# Restart services if needed
sudo systemctl restart ignition
sudo systemctl restart prometheus
```

## Performance Best Practices

### Design Principles

1. **Measure First**: Establish baseline performance before optimization
2. **Optimize Incrementally**: Make one change at a time and measure impact  
3. **Monitor Continuously**: Track performance metrics over time
4. **Plan for Scale**: Design for peak load, not average load
5. **Document Changes**: Record all performance-related configuration changes

### Capacity Planning

**Resource Requirements by Scale:**

```
Small Installation (1-10 gateways):
- Scrape Interval: 30s
- Expected Metrics: 100-500 per gateway
- Network Bandwidth: <1 Mbps
- Prometheus Storage: <10GB/month

Medium Installation (10-100 gateways):
- Scrape Interval: 60s  
- Expected Metrics: 500-2000 per gateway
- Network Bandwidth: <10 Mbps
- Prometheus Storage: <100GB/month

Large Installation (100+ gateways):
- Scrape Interval: 120s
- Expected Metrics: 1000+ per gateway  
- Network Bandwidth: <100 Mbps
- Prometheus Storage: >1TB/month
```

## Next Steps

After performance tuning:

1. **[Configure Prometheus scraping](../prometheus-integration/scrape-configuration)** with optimized settings
2. **[Set up monitoring dashboards](../prometheus-integration/grafana-dashboards)** to track performance
3. **[Implement alerting](../prometheus-integration/alerting-rules)** for performance degradation