# Common Issues

Troubleshooting guide for frequent problems encountered with the Prometheus Exporter Module.

## Installation and Configuration Issues

### Module Not Loading

**Symptom**: Module appears in gateway but shows "Error" status or doesn't start

**Diagnostic Steps**:
```bash
# Check gateway logs for module loading errors
grep -i "prometheus" /var/log/ignition/wrapper.log

# Check for Java version compatibility
java -version

# Verify module file integrity
ls -la /path/to/ignition/user-lib/modules/Prometheus-Exporter.modl
```

**Common Causes & Solutions**:

1. **Incompatible Ignition Version**
   ```
   Error: Module requires Ignition 8.1.44 or later
   Solution: Upgrade Ignition or use compatible module version
   ```

2. **Missing Dependencies**
   ```
   Error: ClassNotFoundException: io.prometheus.client.CollectorRegistry
   Solution: Module should include all dependencies. Reinstall module.
   ```

3. **File Permissions**
   ```bash
   # Fix module file permissions
   sudo chown ignition:ignition /path/to/ignition/user-lib/modules/Prometheus-Exporter.modl
   sudo chmod 644 /path/to/ignition/user-lib/modules/Prometheus-Exporter.modl
   ```

4. **Memory Constraints**
   ```bash
   # Increase JVM heap size
   # Edit ignition.conf
   wrapper.java.additional.1=-Xmx2g
   sudo systemctl restart ignition
   ```

### Endpoint Not Accessible

**Symptom**: HTTP 404 error when accessing `/system/metrics`

**Diagnostic Steps**:
```bash
# Test local access from gateway server
curl -I http://localhost:8088/system/metrics

# Check if web server is running
netstat -tuln | grep 8088

# Verify module initialization
grep "PrometheusExporterGatewayHook" /var/log/ignition/wrapper.log
```

**Solutions**:

1. **Module Not Started**
   ```
   Check: Config → System → Modules
   Action: Restart module or gateway
   ```

2. **Web Server Configuration**
   ```bash
   # Check web server is enabled
   # Gateway → Config → Gateway Network → Web Server
   # Verify "Web Server Enabled" is checked
   ```

3. **Servlet Registration Failed**
   ```bash
   # Look for servlet registration errors
   grep -i "servlet.*metrics" /var/log/ignition/wrapper.log
   
   # Restart gateway to retry registration
   sudo systemctl restart ignition
   ```

## Metrics Collection Issues

### No Metrics Returned

**Symptom**: Endpoint accessible but returns empty response or only headers

**Diagnostic Commands**:
```bash
# Check response content
curl -v http://gateway:8088/system/metrics

# Verify content length
curl -I http://gateway:8088/system/metrics | grep Content-Length

# Check for basic JVM metrics
curl -s http://gateway:8088/system/metrics | grep jvm_memory_bytes_used
```

**Root Causes & Fixes**:

1. **MetricRegistry Not Initialized**
   ```java
   // Gateway may not be fully started
   // Wait 2-3 minutes after gateway startup
   // Check gateway status: Config → Status → Gateway Status
   ```

2. **Dropwizard Integration Issue**
   ```bash
   # Check for registration errors
   grep "DropwizardExports" /var/log/ignition/wrapper.log
   
   # Restart module to retry registration
   # Config → System → Modules → Prometheus Exporter → Restart
   ```

3. **Collector Registry Problems**
   ```bash
   # Look for collector registration issues
   grep -i "collector.*register" /var/log/ignition/wrapper.log
   ```

### Incomplete Metrics

**Symptom**: Some metrics missing or inconsistent metric count

**Investigation Steps**:
```bash
# Count available metrics
curl -s http://gateway:8088/system/metrics | grep -c "^[a-zA-Z_]"

# Compare with baseline (should be >50 for typical installation)
# Check specific metric categories
curl -s http://gateway:8088/system/metrics | grep -c "jvm_"
curl -s http://gateway:8088/system/metrics | grep -c "process_"
```

**Common Issues**:

1. **Partial Gateway Initialization**
   ```
   Solution: Wait for complete gateway startup (2-5 minutes)
   Check: All modules loaded and running
   ```

2. **Module Loading Order**
   ```
   Issue: Some modules may not be initialized when metrics collected
   Solution: Restart gateway to ensure proper loading order
   ```

3. **Custom Metric Registration Errors**
   ```python
   # Check for script errors in custom metrics
   # Gateway → Config → Scripting → Gateway Event Scripts
   # Look for exceptions in script execution logs
   ```

## Performance Issues

### Slow Response Times

**Symptom**: `/system/metrics` endpoint responds slowly (>5 seconds)

**Performance Analysis**:
```bash
# Measure response time
time curl -s http://gateway:8088/system/metrics > /dev/null

# Check response size
curl -s http://gateway:8088/system/metrics | wc -c

# Monitor during collection
top -p $(pgrep -f ignition) -b -n 1
```

**Optimization Actions**:

1. **Reduce Metric Cardinality**
   ```bash
   # Identify high-cardinality metrics
   curl -s http://gateway:8088/system/metrics | \
   grep "^[a-zA-Z_]" | cut -d'{' -f1 | sort | uniq -c | sort -nr
   
   # Look for metrics with many label combinations
   ```

2. **Memory Pressure**
   ```bash
   # Check JVM memory usage
   curl -s http://gateway:8088/system/metrics | \
   grep "jvm_memory_bytes_used.*heap"
   
   # If memory usage >80%, increase heap size
   ```

3. **Custom Metric Optimization**
   ```python
   # Optimize script-based metrics
   # Avoid expensive calculations in metric collection
   # Pre-calculate values in separate scheduled scripts
   ```

### High Memory Usage from Metrics

**Symptom**: Gateway memory usage increases after installing module

**Memory Analysis**:
```bash
# Monitor memory before/after module installation
ps aux | grep ignition

# Check for memory leaks in custom metrics
curl -s http://gateway:8088/system/metrics | grep -c "ignition_"
```

**Solutions**:

1. **Custom Metric Cleanup**
   ```python
   # Avoid creating unbounded label combinations
   # Bad: system.prometheus.gauge("temp").labels({"sensor": sensor_id})
   # Good: system.prometheus.gauge("temp").labels({"zone": zone_name})
   ```

2. **Metric Registration Optimization**
   ```python
   # Register metrics once, reuse instances
   if not hasattr(system, '_metrics_initialized'):
       system._custom_counter = system.prometheus.counter("operations_total")
       system._metrics_initialized = True
   
   system._custom_counter.increment()
   ```

## Network and Connectivity Issues

### Prometheus Can't Reach Endpoint

**Symptom**: Prometheus shows target as "down" or connection timeouts

**Network Diagnosis**:
```bash
# Test connectivity from Prometheus server
telnet gateway-host 8088

# Check firewall rules
sudo iptables -L | grep 8088

# Verify DNS resolution
nslookup gateway-host

# Test from Prometheus container (if using Docker)
docker exec prometheus wget -qO- http://gateway-host:8088/system/metrics
```

**Solutions**:

1. **Firewall Configuration**
   ```bash
   # Allow Prometheus access
   sudo iptables -A INPUT -p tcp --dport 8088 -s prometheus-server-ip -j ACCEPT
   
   # Or open to monitoring subnet
   sudo iptables -A INPUT -p tcp --dport 8088 -s 10.0.1.0/24 -j ACCEPT
   ```

2. **Docker Network Issues**
   ```yaml
   # Update Prometheus configuration for Docker
   scrape_configs:
     - job_name: 'ignition-gateway'
       static_configs:
         - targets: ['host.docker.internal:8088']  # For Docker Desktop
         # or
         - targets: ['gateway-container-name:8088']  # For Docker networks
   ```

3. **SSL/TLS Certificate Issues**
   ```bash
   # Test SSL connectivity
   openssl s_client -connect gateway-host:8043
   
   # Use insecure skip verify in Prometheus if needed
   tls_config:
     insecure_skip_verify: true
   ```

### Authentication Problems

**Symptom**: HTTP 401/403 errors when accessing endpoint

**Authentication Diagnosis**:
```bash
# Test without authentication
curl -I http://gateway:8088/system/metrics

# Check if gateway requires authentication
curl -I http://gateway:8088/main
```

**Solutions**:

1. **Gateway Authentication**
   ```
   Issue: Gateway web interface requires authentication
   Solution: Use reverse proxy with authentication
   ```

2. **Network-Level Security**
   ```bash
   # Check if IP-based restrictions are in place
   # Test from allowed IP range
   ```

## Data Quality Issues

### Metrics Show Invalid Values

**Symptom**: Metrics contain NaN, Inf, or unrealistic values

**Data Validation**:
```bash
# Check for invalid numeric values
curl -s http://gateway:8088/system/metrics | grep -E "NaN|Inf|-Inf"

# Verify metric ranges are reasonable
curl -s http://gateway:8088/system/metrics | grep jvm_memory_bytes_used

# Check for negative values where inappropriate
curl -s http://gateway:8088/system/metrics | grep -- "-[0-9]"
```

**Common Causes**:

1. **Division by Zero in Custom Metrics**
   ```python
   # Fix division by zero
   denominator = get_denominator_value()
   if denominator != 0:
       ratio = numerator / denominator
       system.prometheus.gauge("ratio_metric").set(ratio)
   else:
       system.prometheus.gauge("ratio_metric").set(0)
   ```

2. **Uninitialized Variables**
   ```python
   # Initialize metrics properly
   try:
       value = get_metric_value()
       system.prometheus.gauge("my_metric").set(value)
   except:
       # Set to 0 or skip update rather than setting invalid value
       system.prometheus.gauge("my_metric").set(0)
   ```

### Inconsistent Timestamps

**Symptom**: Metrics appear with timestamps or time inconsistencies

**Timestamp Analysis**:
```bash
# Check for explicit timestamps (most metrics shouldn't have them)
curl -s http://gateway:8088/system/metrics | grep -E "^[a-zA-Z_].*[0-9]+\s+[0-9]{10}"

# Verify system time synchronization
date
ntpq -p  # If using NTP
```

**Solutions**:

1. **Remove Explicit Timestamps**
   ```python
   # Don't set timestamps in custom metrics
   # Prometheus will use scrape time automatically
   system.prometheus.gauge("metric").set(value)  # Correct
   # Not: system.prometheus.gauge("metric").set(value, timestamp)
   ```

2. **System Time Sync**
   ```bash
   # Synchronize system time
   sudo ntpdate -s time.nist.gov
   # Or configure NTP daemon
   ```

## Module Update Issues

### Update Failures

**Symptom**: Module update fails or causes gateway instability

**Pre-Update Checklist**:
```bash
# Backup current configuration
cp /path/to/ignition/user-lib/modules/Prometheus-Exporter.modl \
   /backup/Prometheus-Exporter-$(date +%Y%m%d).modl

# Stop any active scraping during update
# Notify Prometheus maintainers about planned downtime
```

**Update Process**:
1. **Graceful Module Shutdown**
   ```
   Config → System → Modules → Prometheus Exporter → Stop
   Wait for clean shutdown (check logs)
   ```

2. **Install New Version**
   ```
   Config → System → Modules → Install or Upgrade a Module
   Select new .modl file
   ```

3. **Verify Update**
   ```bash
   # Check module version and status
   curl -s http://gateway:8088/system/metrics | head -10
   
   # Verify metric count similar to before
   curl -s http://gateway:8088/system/metrics | grep -c "^[a-zA-Z_]"
   ```

### Configuration Migration Issues

**Symptom**: Custom metrics or configuration lost after update

**Recovery Steps**:
```python
# Re-register custom metrics if they disappeared
# Check Gateway → Config → Scripting → Gateway Event Scripts
# Verify startup scripts are still active

def re_register_custom_metrics():
    """Re-register custom metrics after module update"""
    try:
        # Re-initialize all custom metrics
        system.prometheus.counter("operations_total").increment(0)
        system.prometheus.gauge("system_health").set(100)
        
        logger = system.util.getLogger("MetricRecovery")
        logger.info("Custom metrics re-registered successfully")
    except Exception as e:
        logger.error("Failed to re-register metrics: %s" % str(e))
```

## Prometheus Integration Issues

### Scrape Failures

**Symptom**: Prometheus can't scrape metrics or gets partial data

**Prometheus Diagnostics**:
```bash
# Check Prometheus target status
curl -s http://prometheus:9090/api/v1/targets | \
jq '.data.activeTargets[] | select(.job=="ignition-gateway")'

# Verify scrape configuration
curl -s http://prometheus:9090/api/v1/status/config | jq '.data.yaml'

# Check recent scrapes
curl -s http://prometheus:9090/api/v1/query?query=up{job="ignition-gateway"}
```

**Configuration Fixes**:
```yaml
# Fix common Prometheus configuration issues
scrape_configs:
  - job_name: 'ignition-gateway'
    static_configs:
      - targets: ['gateway:8088']  # Ensure correct hostname/IP
    metrics_path: '/system/metrics'  # Correct path
    scrape_interval: 30s  # Not too frequent
    scrape_timeout: 10s   # Reasonable timeout
```

### Data Retention Issues

**Symptom**: Historical metrics data missing or inconsistent

**Investigation**:
```bash
# Check Prometheus retention settings
curl -s http://prometheus:9090/api/v1/status/runtimeinfo | jq '.data.storageRetention'

# Verify disk space
df -h /prometheus/data

# Check for gaps in data
curl -s 'http://prometheus:9090/api/v1/query_range?query=up{job="ignition-gateway"}&start=1609459200&end=1609545600&step=300'
```

## Getting Help

### Log Collection

When reporting issues, collect these logs:

```bash
#!/bin/bash
# collect-logs.sh

LOG_DIR="/tmp/ignition-prometheus-logs-$(date +%Y%m%d-%H%M)"
mkdir -p $LOG_DIR

# Ignition gateway logs
tail -n 1000 /var/log/ignition/wrapper.log > $LOG_DIR/gateway.log
grep -i prometheus /var/log/ignition/wrapper.log > $LOG_DIR/prometheus-specific.log

# Current metrics sample
curl -s http://localhost:8088/system/metrics > $LOG_DIR/current-metrics.txt

# System information
uname -a > $LOG_DIR/system-info.txt
java -version 2>&1 > $LOG_DIR/java-version.txt

# Ignition module information
# Export from Config → System → Modules → Export Module List

echo "Logs collected in $LOG_DIR"
tar -czf "$LOG_DIR.tar.gz" -C /tmp "$(basename $LOG_DIR)"
```

### Diagnostic Report

Create a diagnostic report for troubleshooting:

```python
# Gateway script to generate diagnostic report
def generate_diagnostic_report():
    """Generate comprehensive diagnostic report"""
    import system.util
    import system.date
    
    report = []
    report.append("=== Prometheus Exporter Diagnostic Report ===")
    report.append("Generated: %s" % str(system.date.now()))
    report.append("")
    
    # Module status
    report.append("Module Status:")
    try:
        # Test metric registration
        system.prometheus.gauge("diagnostic_test").set(1)
        report.append("  ✓ Custom metrics registration working")
    except Exception as e:
        report.append("  ✗ Custom metrics registration failed: %s" % str(e))
    
    # System information
    report.append("\nSystem Information:")
    report.append("  JVM Memory: %d MB available" % (system.util.getAvailableMemory() / 1024 / 1024))
    
    # Gateway status
    report.append("\nGateway Status:")
    report.append("  Uptime: %s" % str(system.util.getSystemProperty("gateway.uptime")))
    
    # Write to file
    report_text = "\n".join(report)
    print report_text
    
    return report_text

# Run diagnostic
diagnostic_result = generate_diagnostic_report()
```

### Community Resources

- **GitHub Issues**: Report bugs and feature requests
- **Documentation**: Check for updated troubleshooting guides  
- **Community Forums**: Search for similar issues and solutions
- **Support Contacts**: Escalate critical production issues

## Preventive Measures

### Regular Health Checks

```bash
#!/bin/bash
# health-check.sh

GATEWAY_URL="http://localhost:8088"
METRICS_ENDPOINT="$GATEWAY_URL/system/metrics"

echo "Performing health check..."

# Test endpoint accessibility
if curl -f -s "$METRICS_ENDPOINT" > /dev/null; then
    echo "✓ Metrics endpoint accessible"
else
    echo "✗ Metrics endpoint failed"
    exit 1
fi

# Check metric count
METRIC_COUNT=$(curl -s "$METRICS_ENDPOINT" | grep -c "^[a-zA-Z_]")
if [ "$METRIC_COUNT" -gt 50 ]; then
    echo "✓ Metrics count normal: $METRIC_COUNT"
else
    echo "⚠ Low metric count: $METRIC_COUNT"
fi

# Check response time
RESPONSE_TIME=$(curl -w '%{time_total}' -s "$METRICS_ENDPOINT" -o /dev/null)
if [ "$(echo "$RESPONSE_TIME < 2.0" | bc)" -eq 1 ]; then
    echo "✓ Response time good: ${RESPONSE_TIME}s"
else
    echo "⚠ Slow response time: ${RESPONSE_TIME}s"
fi

echo "Health check complete"
```

### Monitoring the Monitor

Set up alerts for the monitoring system itself:

```yaml
# Monitor Prometheus Exporter module health
groups:
  - name: prometheus-exporter.health
    rules:
      - alert: PrometheusExporterScrapeFailure
        expr: up{job="ignition-gateway"} == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Prometheus Exporter endpoint not responding"

      - alert: PrometheusExporterLowMetricCount
        expr: scrape_samples_scraped{job="ignition-gateway"} < 50
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Low metric count from Ignition gateway"
```

## Next Steps

After resolving common issues:

1. **[Enable debug logging](debugging)** for deeper troubleshooting
2. **[Monitor performance metrics](performance)** to prevent future issues  
3. **[Implement health checks](../examples/basic-monitoring)** for proactive monitoring