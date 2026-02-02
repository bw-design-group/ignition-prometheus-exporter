# Debugging

Advanced debugging techniques for diagnosing issues with the Prometheus Exporter Module.

## Debug Logging Configuration

### Enable Module Debug Logging

Configure detailed logging for the Prometheus Exporter Module:

1. **Gateway Web Interface**
   - Navigate to **Config** → **Gateway Network** → **Logging**
   - Click **Add Logger**
   - Set Logger Name: `dev.bwdesigngroup.prometheus`
   - Set Level: `DEBUG` or `TRACE`
   - Save changes

2. **Via Configuration File** (Alternative)
   ```xml
   <!-- In ignition-gateway/data/logback.xml -->
   <logger name="dev.bwdesigngroup.prometheus" level="DEBUG" additivity="false">
       <appender-ref ref="STDOUT"/>
       <appender-ref ref="FILE"/>
   </logger>
   ```

### Debug Log Examples

**Module Initialization Logs**:
```
DEBUG [PrometheusExporterGatewayHook] - Initializing Prometheus Exporter Module
DEBUG [PrometheusExporterGatewayHook] - MetricRegistry obtained: com.codahale.metrics.MetricRegistry@abc123
DEBUG [PrometheusExporterGatewayHook] - DropwizardExports registered with CollectorRegistry
DEBUG [PrometheusExporterGatewayHook] - Servlet registered at path: /system/metrics
INFO  [PrometheusExporterGatewayHook] - Prometheus Exporter Gateway initialized with unified metrics registry
```

**Metric Collection Logs**:
```
TRACE [PrometheusMetricsServlet] - Processing metrics request from 192.168.1.100
TRACE [PrometheusMetricsServlet] - Collected 127 metric families
TRACE [PrometheusMetricsServlet] - Response generated in 15ms, size: 12845 bytes
DEBUG [PrometheusMetricsServlet] - Metrics request completed successfully
```

## Request-Level Debugging

### Trace Individual Requests

Add detailed request logging to diagnose specific issues:

```bash
# Enable HTTP request logging in Ignition
# This logs all web requests including metrics endpoint
curl -X POST http://gateway:8088/main/system/webserver/debug \
  -H "Content-Type: application/json" \
  -d '{"level": "TRACE", "duration": 300}'
```

### Monitor Request Processing

Track metrics endpoint requests in real-time:

```bash
# Monitor access logs for metrics requests
tail -f /var/log/ignition/wrapper.log | grep -E "(metrics|prometheus)"

# Watch for specific client requests
tail -f /var/log/ignition/wrapper.log | grep "192.168.1.100"

# Monitor response times
tail -f /var/log/ignition/wrapper.log | grep -E "completed.*[0-9]+ms"
```

### HTTP Request Tracing

Debug HTTP-level issues with detailed tracing:

```bash
# Test with verbose curl output
curl -v -H "Accept: text/plain" http://gateway:8088/system/metrics

# Trace with netcat for raw HTTP
echo -e "GET /system/metrics HTTP/1.1\r\nHost: gateway:8088\r\n\r\n" | nc gateway 8088

# Use httpie for structured debugging
http GET gateway:8088/system/metrics Accept:text/plain
```

## Metric Collection Debugging

### Dropwizard Integration Analysis

Verify Dropwizard metrics are being collected properly:

```java
// Debug script to inspect MetricRegistry contents
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Counter;
import java.util.Map;

// Get the gateway's MetricRegistry
MetricRegistry registry = system.util.getGatewayContext().getMetricRegistry();

// List all registered metrics
Map<String, com.codahale.metrics.Metric> metrics = registry.getMetrics();
system.util.getLogger("DebugMetrics").info("Total metrics in registry: " + metrics.size());

// Sample metric values
for (Map.Entry<String, com.codahale.metrics.Metric> entry : metrics.entrySet()) {
    String name = entry.getKey();
    com.codahale.metrics.Metric metric = entry.getValue();
    
    if (metric instanceof Gauge) {
        Gauge gauge = (Gauge) metric;
        system.util.getLogger("DebugMetrics").info("Gauge " + name + ": " + gauge.getValue());
    } else if (metric instanceof Counter) {
        Counter counter = (Counter) metric;
        system.util.getLogger("DebugMetrics").info("Counter " + name + ": " + counter.getCount());
    }
}
```

### Custom Metric Debugging

Debug custom metrics registered via scripting:

```python
# Debug custom metric registration
def debug_custom_metrics():
    logger = system.util.getLogger("CustomMetricsDebug")
    
    try:
        # Test counter registration
        counter = system.prometheus.counter("debug_test_counter")
        counter.increment()
        logger.info("Counter registration successful")
        
        # Test gauge registration  
        gauge = system.prometheus.gauge("debug_test_gauge")
        gauge.set(42.5)
        logger.info("Gauge registration successful")
        
        # Test histogram registration
        histogram = system.prometheus.histogram("debug_test_histogram") \
            .buckets([0.1, 1.0, 10.0])
        histogram.observe(2.3)
        logger.info("Histogram registration successful")
        
    except Exception as e:
        logger.error("Custom metric registration failed: %s" % str(e))
        
        # Print full stack trace for debugging
        import traceback
        logger.error("Stack trace: %s" % traceback.format_exc())

# Run debug test
debug_custom_metrics()
```

### Metric Format Validation

Verify metrics are properly formatted for Prometheus:

```bash
# Check Prometheus format compliance
validate_prometheus_format() {
    local endpoint="$1"
    
    echo "Validating Prometheus format for: $endpoint"
    
    # Download metrics
    curl -s "$endpoint" > /tmp/metrics.txt
    
    # Check for required elements
    if grep -q "^# HELP" /tmp/metrics.txt; then
        echo "✓ HELP comments found"
    else
        echo "✗ Missing HELP comments"
    fi
    
    if grep -q "^# TYPE" /tmp/metrics.txt; then
        echo "✓ TYPE declarations found"
    else
        echo "✗ Missing TYPE declarations"
    fi
    
    # Validate metric naming
    invalid_names=$(grep -E "^[a-zA-Z_][a-zA-Z0-9_]*\{" /tmp/metrics.txt | \
                   grep -v -E "^[a-zA-Z_][a-zA-Z0-9_]*\{.*\} [0-9]")
    
    if [ -z "$invalid_names" ]; then
        echo "✓ All metric names valid"
    else
        echo "✗ Invalid metric names found:"
        echo "$invalid_names"
    fi
    
    # Check for NaN or Inf values
    invalid_values=$(grep -E "NaN|Inf|-Inf" /tmp/metrics.txt)
    if [ -z "$invalid_values" ]; then
        echo "✓ No invalid numeric values"
    else
        echo "✗ Invalid values found:"
        echo "$invalid_values"
    fi
    
    rm /tmp/metrics.txt
}

# Run validation
validate_prometheus_format "http://gateway:8088/system/metrics"
```

## Performance Debugging

### Response Time Analysis

Identify performance bottlenecks in metrics collection:

```bash
#!/bin/bash
# performance-debug.sh

GATEWAY_URL="http://gateway:8088/system/metrics"

echo "Analyzing metrics collection performance..."

# Multiple timed requests
for i in {1..10}; do
    TIME_OUTPUT=$(curl -w "@curl-format.txt" -s "$GATEWAY_URL" -o /dev/null)
    echo "Request $i: $TIME_OUTPUT"
done

# Profile with different client configurations
echo -e "\nTesting with compression:"
curl -w "%{time_total}s\n" -H "Accept-Encoding: gzip" -s "$GATEWAY_URL" -o /dev/null

echo -e "\nTesting without keep-alive:"
curl -w "%{time_total}s\n" -H "Connection: close" -s "$GATEWAY_URL" -o /dev/null
```

Create `curl-format.txt`:
```
     time_namelookup:  %{time_namelookup}s\n
        time_connect:  %{time_connect}s\n
     time_appconnect:  %{time_appconnect}s\n
    time_pretransfer:  %{time_pretransfer}s\n
       time_redirect:  %{time_redirect}s\n
  time_starttransfer:  %{time_starttransfer}s\n
                     ----------\n
          time_total:  %{time_total}s\n
```

### Memory Usage Profiling

Monitor memory impact of metrics collection:

```bash
# Monitor JVM heap during metrics collection
monitor_memory_impact() {
    echo "Monitoring memory usage during metrics collection..."
    
    # Get baseline memory
    BEFORE=$(curl -s http://gateway:8088/system/metrics | \
            grep "jvm_memory_bytes_used.*heap" | \
            grep -o '[0-9.]*e[0-9]*')
    
    echo "Memory before: $BEFORE bytes"
    
    # Trigger multiple collections
    for i in {1..20}; do
        curl -s http://gateway:8088/system/metrics > /dev/null
        sleep 1
    done
    
    # Get memory after
    AFTER=$(curl -s http://gateway:8088/system/metrics | \
           grep "jvm_memory_bytes_used.*heap" | \
           grep -o '[0-9.]*e[0-9]*')
    
    echo "Memory after: $AFTER bytes"
    
    # Calculate difference (requires bc)
    DIFF=$(echo "$AFTER - $BEFORE" | bc)
    echo "Memory difference: $DIFF bytes"
}

monitor_memory_impact
```

### Concurrency Testing

Test behavior under concurrent load:

```bash
#!/bin/bash
# concurrency-test.sh

GATEWAY_URL="http://gateway:8088/system/metrics"
CONCURRENT_REQUESTS=10

echo "Testing concurrent access with $CONCURRENT_REQUESTS requests..."

# Function to make request and time it
make_request() {
    local id=$1
    local start=$(date +%s.%N)
    
    if curl -s "$GATEWAY_URL" > /tmp/metrics_$id.txt 2>/dev/null; then
        local end=$(date +%s.%N)
        local duration=$(echo "$end - $start" | bc)
        echo "Request $id: ${duration}s - $(wc -l < /tmp/metrics_$id.txt) lines"
        rm /tmp/metrics_$id.txt
    else
        echo "Request $id: FAILED"
    fi
}

# Launch concurrent requests
for i in $(seq 1 $CONCURRENT_REQUESTS); do
    make_request $i &
done

# Wait for all requests to complete
wait

echo "Concurrent test completed"
```

## Network-Level Debugging

### TCP Connection Analysis

Debug network connectivity issues:

```bash
# Monitor TCP connections to metrics endpoint
netstat -an | grep :8088

# Use tcpdump to capture metrics requests
sudo tcpdump -i any -A -s 0 'host gateway and port 8088'

# Monitor with ss (modern alternative to netstat)
ss -tuln | grep :8088
ss -tup | grep :8088  # Show processes

# Test connection establishment
timeout 5 bash -c '</dev/tcp/gateway/8088' && echo "Port 8088 open" || echo "Port 8088 closed"
```

### SSL/TLS Debugging

Debug HTTPS connectivity issues:

```bash
# Test SSL handshake
openssl s_client -connect gateway:8043 -servername gateway

# Debug certificate issues
openssl s_client -connect gateway:8043 -verify_return_error

# Check certificate details
echo | openssl s_client -connect gateway:8043 2>/dev/null | \
    openssl x509 -noout -dates -subject

# Test with different TLS versions
curl -v --tlsv1.2 https://gateway:8043/system/metrics
curl -v --tlsv1.3 https://gateway:8043/system/metrics
```

### Packet Analysis

Deep packet inspection for troubleshooting:

```bash
# Capture HTTP requests to metrics endpoint
sudo tcpdump -i any -s 0 -w metrics_traffic.pcap 'host gateway and port 8088'

# Analyze captured traffic
tcpdump -r metrics_traffic.pcap -A | grep -E "(GET|POST|HTTP)"

# Use tshark for detailed analysis
tshark -r metrics_traffic.pcap -T fields -e http.request.uri -e http.response.code
```

## Prometheus Integration Debugging

### Scrape Configuration Validation

Debug Prometheus scraping issues:

```yaml
# Test scrape configuration
scrape_configs:
  - job_name: 'debug-ignition'
    static_configs:
      - targets: ['gateway:8088']
    metrics_path: '/system/metrics'
    scrape_interval: 30s
    scrape_timeout: 10s
    
    # Enable debug logging for this job
    honor_labels: true
    metric_relabel_configs:
      - source_labels: [__name__]
        target_label: debug_original_name
        regex: '(.*)'
        replacement: '${1}'
```

### Query Debugging

Debug PromQL queries and data issues:

```bash
# Test basic connectivity
curl -s 'http://prometheus:9090/api/v1/query?query=up{job="ignition-gateway"}'

# Debug query execution
curl -s 'http://prometheus:9090/api/v1/query?query=jvm_memory_bytes_used&debug=true'

# Check metric metadata
curl -s 'http://prometheus:9090/api/v1/metadata?metric=jvm_memory_bytes_used'

# Analyze time series data
curl -s 'http://prometheus:9090/api/v1/query_range?query=rate(jvm_gc_collection_seconds_total[5m])&start=1609459200&end=1609462800&step=60'
```

### Scrape Target Analysis

Monitor scrape target health:

```bash
# Check all targets status
curl -s http://prometheus:9090/api/v1/targets | jq '.data.activeTargets[] | select(.job=="ignition-gateway")'

# Get scrape pool information  
curl -s http://prometheus:9090/api/v1/targets | \
    jq '.data.activeTargets[] | {job: .job, instance: .labels.instance, health: .health, lastScrape: .lastScrape}'

# Monitor scrape duration
curl -s 'http://prometheus:9090/api/v1/query?query=scrape_duration_seconds{job="ignition-gateway"}'
```

## Advanced Debugging Techniques

### JVM-Level Debugging

Enable detailed JVM debugging for the gateway:

```bash
# Add JVM debugging flags (add to ignition.conf)
wrapper.java.additional.10=-XX:+PrintGC
wrapper.java.additional.11=-XX:+PrintGCDetails
wrapper.java.additional.12=-XX:+PrintGCTimeStamps
wrapper.java.additional.13=-XX:+UseG1GC
wrapper.java.additional.14=-XX:+PrintStringDeduplicationStatistics

# Restart gateway to apply changes
sudo systemctl restart ignition
```

### Thread Dump Analysis

Capture thread dumps during performance issues:

```bash
# Get Ignition PID
IGNITION_PID=$(pgrep -f ignition)

# Capture thread dump
jstack $IGNITION_PID > /tmp/ignition-threaddump-$(date +%Y%m%d-%H%M%S).txt

# Or use kill signal
kill -3 $IGNITION_PID  # Creates thread dump in log files
```

### Memory Dump Analysis

Capture heap dumps for memory analysis:

```bash
# Capture heap dump
jmap -dump:live,format=b,file=/tmp/ignition-heapdump-$(date +%Y%m%d-%H%M%S).hprof $IGNITION_PID

# Analyze with jhat (basic)
jhat /tmp/ignition-heapdump-*.hprof

# Or use Eclipse MAT for advanced analysis
```

### Profiling Metrics Collection

Profile the metrics collection process:

```python
# Gateway script for profiling metrics collection
import system.util
import system.date

def profile_metrics_collection():
    """Profile custom metrics collection performance"""
    logger = system.util.getLogger("MetricsProfiler")
    
    # Start timing
    start_time = system.date.now()
    
    try:
        # Simulate metric collection workload
        for i in range(100):
            system.prometheus.counter("profile_test_counter") \
                .labels({"iteration": str(i % 10)}) \
                .increment()
            
            system.prometheus.gauge("profile_test_gauge") \
                .labels({"batch": str(i / 10)}) \
                .set(i * 0.5)
        
        # End timing
        end_time = system.date.now()
        duration = system.date.secondsBetween(start_time, end_time)
        
        logger.info("Metrics collection completed in %f seconds" % duration)
        logger.info("Rate: %f metrics/second" % (200.0 / duration))
        
    except Exception as e:
        logger.error("Profiling failed: %s" % str(e))
        import traceback
        logger.error(traceback.format_exc())

# Run profiling
profile_metrics_collection()
```

## Debug Tools and Scripts

### Comprehensive Debug Script

```bash
#!/bin/bash
# debug-prometheus-exporter.sh

GATEWAY_HOST="${1:-localhost}"
GATEWAY_PORT="${2:-8088}"
OUTPUT_DIR="/tmp/prometheus-debug-$(date +%Y%m%d-%H%M%S)"

mkdir -p "$OUTPUT_DIR"
echo "Starting debug data collection in $OUTPUT_DIR"

# Basic connectivity test
echo "=== Connectivity Test ===" > "$OUTPUT_DIR/connectivity.txt"
curl -v "http://$GATEWAY_HOST:$GATEWAY_PORT/system/metrics" >> "$OUTPUT_DIR/connectivity.txt" 2>&1

# Metrics sample
echo "Collecting metrics sample..."
curl -s "http://$GATEWAY_HOST:$GATEWAY_PORT/system/metrics" > "$OUTPUT_DIR/metrics-sample.txt"

# Performance test
echo "=== Performance Test ===" > "$OUTPUT_DIR/performance.txt"
for i in {1..5}; do
    time curl -s "http://$GATEWAY_HOST:$GATEWAY_PORT/system/metrics" > /dev/null
done 2>> "$OUTPUT_DIR/performance.txt"

# Network analysis
echo "=== Network Analysis ===" > "$OUTPUT_DIR/network.txt"
netstat -an | grep ":$GATEWAY_PORT" >> "$OUTPUT_DIR/network.txt"
ss -tuln | grep ":$GATEWAY_PORT" >> "$OUTPUT_DIR/network.txt"

# System information
echo "=== System Information ===" > "$OUTPUT_DIR/system-info.txt"
uname -a >> "$OUTPUT_DIR/system-info.txt"
date >> "$OUTPUT_DIR/system-info.txt"

# Metric analysis
echo "=== Metric Analysis ===" > "$OUTPUT_DIR/metric-analysis.txt"
echo "Total metrics: $(grep -c '^[a-zA-Z_]' "$OUTPUT_DIR/metrics-sample.txt")" >> "$OUTPUT_DIR/metric-analysis.txt"
echo "JVM metrics: $(grep -c '^jvm_' "$OUTPUT_DIR/metrics-sample.txt")" >> "$OUTPUT_DIR/metric-analysis.txt"
echo "Process metrics: $(grep -c '^process_' "$OUTPUT_DIR/metrics-sample.txt")" >> "$OUTPUT_DIR/metric-analysis.txt"

# Create summary
echo "Debug data collected in: $OUTPUT_DIR"
echo "Files created:"
ls -la "$OUTPUT_DIR"

# Create archive
tar -czf "$OUTPUT_DIR.tar.gz" -C /tmp "$(basename $OUTPUT_DIR)"
echo "Archive created: $OUTPUT_DIR.tar.gz"
```

### Interactive Debugging Session

```python
# Gateway script for interactive debugging
def interactive_debug_session():
    """Interactive debugging session for metrics"""
    import system.util
    
    logger = system.util.getLogger("InteractiveDebug")
    
    # Test metric registration
    logger.info("=== Testing Metric Registration ===")
    
    try:
        counter = system.prometheus.counter("debug_session_counter")
        counter.increment()
        logger.info("✓ Counter registration successful")
    except Exception as e:
        logger.error("✗ Counter registration failed: %s" % str(e))
    
    try:
        gauge = system.prometheus.gauge("debug_session_gauge")
        gauge.set(123.456)
        logger.info("✓ Gauge registration successful")
    except Exception as e:
        logger.error("✗ Gauge registration failed: %s" % str(e))
    
    # Test HTTP endpoint
    logger.info("=== Testing HTTP Endpoint ===")
    
    try:
        import urllib2
        response = urllib2.urlopen("http://localhost:8088/system/metrics")
        content = response.read()
        logger.info("✓ HTTP endpoint accessible, %d bytes returned" % len(content))
        
        # Check for our debug metrics
        if "debug_session_counter" in content:
            logger.info("✓ Custom counter found in output")
        else:
            logger.warn("✗ Custom counter not found in output")
            
        if "debug_session_gauge" in content:
            logger.info("✓ Custom gauge found in output")
        else:
            logger.warn("✗ Custom gauge not found in output")
            
    except Exception as e:
        logger.error("✗ HTTP endpoint test failed: %s" % str(e))
    
    logger.info("=== Debug Session Complete ===")

# Run interactive debugging
interactive_debug_session()
```

## Next Steps

After debugging:

1. **[Implement performance optimizations](performance)** based on findings
2. **[Monitor resolved issues](common-issues)** to prevent regression
3. **[Document solutions](../examples/basic-monitoring)** for future reference