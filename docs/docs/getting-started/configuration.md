# Configuration

The Prometheus Exporter Module requires minimal configuration and works out-of-the-box after installation. This page covers basic configuration options and common customizations.

## Default Configuration

### Zero-Configuration Operation

The module is designed to work immediately after installation with these defaults:

- **Endpoint Path**: `/system/metrics`
- **Content Type**: `text/plain; version=0.0.4; charset=utf-8`
- **Metrics Source**: All metrics from Ignition's Dropwizard MetricRegistry
- **Access Control**: Inherits Ignition's web server security settings

### Automatic Servlet Registration

During startup, the module automatically:

1. Registers a servlet at the `/system/metrics` endpoint
2. Connects to Ignition's existing MetricRegistry
3. Configures Prometheus format export
4. Enables HTTP GET access to metrics data

## Gateway Web Server Configuration

### SSL/HTTPS Configuration

The metrics endpoint inherits the gateway's web server SSL settings:

**To enable HTTPS for metrics:**
1. Navigate to **Config** → **Gateway Network** → **Web Server**
2. Configure SSL settings as needed
3. The metrics endpoint will automatically use HTTPS: `https://your-gateway:8043/system/metrics`

**Mixed HTTP/HTTPS environments:**
- HTTP: `http://your-gateway:8088/system/metrics`
- HTTPS: `https://your-gateway:8043/system/metrics`

### Security Settings

**Default Access Control**
The module respects Ignition's built-in security:
- No authentication required by default
- Access controlled by gateway network settings
- Firewall rules apply to metrics endpoint

**Restricting Access**
To limit access to the metrics endpoint:

1. **Network-level restrictions** (recommended)
   ```bash
   # Example iptables rule to allow only Prometheus server
   iptables -A INPUT -p tcp --dport 8088 -s prometheus-server-ip -j ACCEPT
   iptables -A INPUT -p tcp --dport 8088 -j DROP
   ```

2. **Gateway network settings**
   - Configure allowed IP ranges in **Config** → **Gateway Network** → **Web Server**
   - Set appropriate subnet restrictions

## Advanced Configuration Options

### Custom Endpoint Path

Currently, the endpoint path is fixed at `/system/metrics`. Future versions may support custom paths through module configuration.

### Metric Registry Selection

The module automatically uses Ignition's primary MetricRegistry. This includes:

- JVM metrics (memory, threads, GC)
- Gateway internal metrics  
- Module-specific metrics
- Custom metrics registered by scripts

### Performance Tuning

**Memory Usage**
The module has minimal memory impact:
- No metric duplication
- Reference-based metric access
- Efficient serialization

**CPU Impact**
Metrics are collected on-demand:
- No background processing
- Collection triggered by HTTP requests
- Sampling occurs during scrape operations

## Environment-Specific Configuration

### Development Environment

For development and testing:

```bash
# Quick verification
curl http://localhost:8088/system/metrics | head -20

# Pretty-print metric names only
curl -s http://localhost:8088/system/metrics | grep '^[a-zA-Z]' | cut -d' ' -f1 | sort -u
```

### Production Environment

Production considerations:

**Network Security**
```yaml
# Example Prometheus scrape config with basic auth
scrape_configs:
  - job_name: 'ignition-gateway'
    static_configs:
      - targets: ['gateway:8088']
    metrics_path: '/system/metrics'
    scheme: https
    tls_config:
      insecure_skip_verify: false
```

**Monitoring Multiple Gateways**
```yaml
# Multi-gateway configuration
scrape_configs:
  - job_name: 'ignition-gateways'
    static_configs:
      - targets: 
        - 'gateway-1:8088'
        - 'gateway-2:8088'
        - 'gateway-3:8088'
    metrics_path: '/system/metrics'
    scrape_interval: 30s
```

## Validation and Testing

### Configuration Verification

**Test Endpoint Accessibility**
```bash
# Basic connectivity test
curl -I http://your-gateway:8088/system/metrics

# Expected response headers
HTTP/1.1 200 OK
Content-Type: text/plain;version=0.0.4;charset=utf-8
Content-Length: xxxx
```

**Validate Metric Format**
```bash
# Check Prometheus format compliance
curl -s http://your-gateway:8088/system/metrics | head -50
```

Expected output format:
```
# HELP jvm_memory_bytes_used Used bytes of a given JVM memory area.
# TYPE jvm_memory_bytes_used gauge
jvm_memory_bytes_used{area="heap"} 1.234567E8
jvm_memory_bytes_used{area="nonheap"} 5.678901E7
```

### Performance Validation

**Response Time Testing**
```bash
# Measure endpoint response time
time curl -s http://your-gateway:8088/system/metrics > /dev/null

# Benchmark multiple requests
for i in {1..10}; do
  time curl -s http://your-gateway:8088/system/metrics > /dev/null
done
```

**Resource Impact Assessment**
Monitor gateway resources before and after module installation:
- Memory usage
- CPU utilization  
- Network throughput

## Troubleshooting Configuration

### Common Configuration Issues

**Endpoint Not Accessible**
```
Problem: HTTP 404 on /system/metrics
Diagnosis: Check module status and servlet registration
Solution: Restart module or gateway
```

**SSL Certificate Issues**
```
Problem: HTTPS endpoint fails with certificate errors
Diagnosis: Check gateway SSL configuration
Solution: Verify SSL certificates and cipher suites
```

**Network Connectivity**
```
Problem: Timeouts when accessing metrics
Diagnosis: Network configuration or firewall rules
Solution: Check network connectivity and security groups
```

### Diagnostic Steps

1. **Verify Module Status**
   - Check **Config** → **System** → **Modules**
   - Ensure module shows "Running" status

2. **Check Gateway Logs**
   ```bash
   grep "PrometheusExporterGatewayHook" /var/log/ignition/wrapper.log
   ```

3. **Test Local Access**
   ```bash
   # From gateway server
   curl http://localhost:8088/system/metrics
   ```

4. **Validate Network Path**
   ```bash
   # From Prometheus server
   telnet gateway-hostname 8088
   ```

## Next Steps

After configuration:

1. **[View your first metrics](first-metrics)** to confirm operation
2. **[Set up Prometheus scraping](../prometheus-integration/scrape-configuration)** 
3. **[Configure performance tuning](../configuration/performance-tuning)** if needed