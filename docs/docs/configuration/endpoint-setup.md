# Endpoint Setup

Configure the Prometheus metrics endpoint to meet your specific requirements including security, performance, and network access patterns.

## Default Endpoint Configuration

### Automatic Registration

The module automatically registers the metrics endpoint during gateway startup:

- **Path**: `/system/metrics`
- **Method**: HTTP GET only
- **Content-Type**: `text/plain; version=0.0.4; charset=utf-8`
- **Port**: Inherits gateway web server port (default 8088 HTTP, 8043 HTTPS)

### Servlet Implementation

The endpoint is implemented as a lightweight servlet that:

1. Accesses the shared CollectorRegistry
2. Serializes metrics in Prometheus exposition format
3. Returns formatted response with appropriate headers
4. Supports HTTP compression when requested by client

## Security Configuration

### Access Control Options

**Network-Level Security** (Recommended)

Restrict access using firewall rules:

```bash
# Allow access only from Prometheus server
sudo iptables -A INPUT -p tcp --dport 8088 -s 192.168.1.100 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8088 -j DROP

# Allow access from monitoring subnet
sudo iptables -A INPUT -p tcp --dport 8088 -s 10.0.1.0/24 -j ACCEPT
sudo iptables -A INPUT -p tcp --dport 8088 -j DROP
```

**Gateway Web Server Restrictions**

Configure IP restrictions in Ignition:

1. Navigate to **Config** → **Gateway Network** → **Web Server**
2. Set **Bind Address** to specific interface if needed
3. Configure **Max Threads** to limit concurrent access

### SSL/TLS Configuration

**Enable HTTPS for metrics endpoint:**

1. **Configure SSL in Gateway**
   - Go to **Config** → **Gateway Network** → **Web Server**  
   - Enable **HTTPS Enabled**
   - Configure SSL certificate and key
   - Set **HTTPS Port** (default 8043)

2. **Access via HTTPS**
   ```bash
   curl https://your-gateway:8043/system/metrics
   ```

3. **Mixed Environment Support**
   The module supports both HTTP and HTTPS simultaneously:
   - HTTP: `http://your-gateway:8088/system/metrics`
   - HTTPS: `https://your-gateway:8043/system/metrics`

### Authentication Considerations

The module does not implement application-level authentication. For authenticated access:

**Reverse Proxy with Authentication**

Use nginx or Apache as reverse proxy:

```nginx
# nginx configuration example
server {
    listen 9090;
    server_name monitoring.company.com;
    
    location /metrics {
        auth_basic "Prometheus Metrics";
        auth_basic_user_file /etc/nginx/.htpasswd;
        proxy_pass http://ignition-gateway:8088/system/metrics;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## Performance Configuration

### Response Optimization

**HTTP Compression**

The servlet supports compression when requested:

```bash
# Request compressed response
curl -H "Accept-Encoding: gzip" http://your-gateway:8088/system/metrics
```

**Connection Pooling**

For high-frequency scraping, use HTTP keep-alive:

```yaml
# Prometheus configuration
scrape_configs:
  - job_name: 'ignition'
    static_configs:
      - targets: ['gateway:8088']
    metrics_path: '/system/metrics'
    scrape_interval: 15s
    scrape_timeout: 10s
    honor_timestamps: false
```

### Memory and CPU Impact

**Resource Usage Characteristics:**

- **Memory**: No metric caching, direct registry access
- **CPU**: Minimal overhead during metric collection
- **Network**: Efficient text serialization
- **Disk**: No persistent storage requirements

**Performance Monitoring:**

```bash
# Monitor endpoint response time
time curl -s http://your-gateway:8088/system/metrics > /dev/null

# Monitor gateway resource usage during scraping
top -p $(pgrep -f ignition)
```

## Network Configuration

### Multi-Gateway Deployments

**Individual Gateway Access**

Each gateway exposes its own metrics endpoint:

```yaml
# Prometheus configuration for multiple gateways
scrape_configs:
  - job_name: 'ignition-gateways'
    static_configs:
      - targets: 
        - 'gateway-primary:8088'
        - 'gateway-backup:8088'
        - 'gateway-edge:8088'
    metrics_path: '/system/metrics'
    relabel_configs:
      - source_labels: [__address__]
        target_label: gateway
        regex: '([^:]+):.*'
```

**Federation Support**

For centralized metric collection:

```yaml
# Prometheus federation configuration
scrape_configs:
  - job_name: 'federate'
    scrape_interval: 15s
    honor_labels: true
    metrics_path: '/federate'
    params:
      'match[]':
        - '{job="ignition-gateways"}'
    static_configs:
      - targets: ['prometheus-central:9090']
```

### Load Balancer Integration

**Health Check Configuration**

Configure load balancers to use metrics endpoint for health checks:

```yaml
# HAProxy example
backend ignition_gateways
    balance roundrobin
    option httpchk GET /system/metrics
    http-check expect status 200
    server gateway1 192.168.1.101:8088 check
    server gateway2 192.168.1.102:8088 check
```

## Advanced Configuration

### Custom Headers

The endpoint returns standard headers:

```http
HTTP/1.1 200 OK
Content-Type: text/plain; version=0.0.4; charset=utf-8
Content-Length: 12345
Date: Mon, 01 Jan 2024 12:00:00 GMT
Server: Jetty(9.4.x)
```

### Error Handling

**Common HTTP Response Codes:**

- **200 OK**: Successful metric collection
- **404 Not Found**: Module not installed or endpoint disabled
- **500 Internal Server Error**: Metric collection failure
- **503 Service Unavailable**: Gateway overloaded

**Error Response Format:**

```http
HTTP/1.1 500 Internal Server Error
Content-Type: text/plain
Content-Length: 45

Error collecting metrics: Registry unavailable
```

### Debugging Configuration

**Enable Debug Logging**

Add logging configuration for troubleshooting:

1. Navigate to **Config** → **Gateway Network** → **Logging**
2. Add logger: `dev.bwdesigngroup.prometheus`
3. Set level to `DEBUG` or `TRACE`

**Debug Output Examples:**

```
DEBUG [PrometheusExporterGatewayHook] - Servlet registered at /system/metrics
DEBUG [PrometheusMetricsServlet] - Processing metrics request from 192.168.1.100
TRACE [PrometheusMetricsServlet] - Collected 127 metrics in 15ms
```

## Validation and Testing

### Endpoint Validation

**Basic Connectivity Test:**

```bash
# Test endpoint availability
curl -I http://your-gateway:8088/system/metrics

# Expected response
HTTP/1.1 200 OK
Content-Type: text/plain; version=0.0.4; charset=utf-8
```

**Format Validation:**

```bash
# Validate Prometheus format
curl -s http://your-gateway:8088/system/metrics | head -20 | \
grep -E "^# (HELP|TYPE)|^[a-zA-Z_][a-zA-Z0-9_]*\{"
```

### Performance Testing

**Load Testing:**

```bash
# Simple load test with ab (Apache Bench)
ab -n 1000 -c 10 http://your-gateway:8088/system/metrics

# Monitor during load test
watch -n 1 "curl -s http://your-gateway:8088/system/metrics | wc -l"
```

**Response Size Analysis:**

```bash
# Measure response size
curl -s http://your-gateway:8088/system/metrics | wc -c

# Compressed vs uncompressed
curl -s http://your-gateway:8088/system/metrics | wc -c
curl -s -H "Accept-Encoding: gzip" http://your-gateway:8088/system/metrics | wc -c
```

## Troubleshooting

### Common Configuration Issues

**Servlet Registration Failures:**

Check gateway logs for servlet conflicts:

```bash
grep -i "servlet.*metrics" /var/log/ignition/wrapper.log
```

**Network Access Issues:**

Verify network connectivity and firewall rules:

```bash
# From Prometheus server
telnet gateway-host 8088
nmap -p 8088 gateway-host
```

**SSL Certificate Problems:**

Test SSL configuration:

```bash
openssl s_client -connect gateway-host:8043 -servername gateway-host
curl -k https://gateway-host:8043/system/metrics
```

## Next Steps

After endpoint configuration:

1. **[Configure metrics filtering](metrics-filtering)** to customize exposed metrics
2. **[Optimize performance](performance-tuning)** for high-frequency scraping
3. **[Set up Prometheus scraping](../prometheus-integration/scrape-configuration)** to collect metrics