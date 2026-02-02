# Installation

Install the Prometheus Exporter Module on your Ignition gateway to begin exposing metrics in Prometheus format.

## Prerequisites

Before installing the module, ensure your environment meets these requirements:

### System Requirements

- **Ignition Gateway**: Version 8.1.44 or later
- **Java**: OpenJDK 11 or later (included with Ignition)
- **Memory**: Minimal additional memory usage (&lt;10MB)
- **Network**: HTTP access to gateway web interface for configuration

### Access Requirements

- Gateway administrator credentials
- Access to Ignition Gateway web interface
- File system access to download module file (if installing manually)

## Installation Methods

### Method 1: Gateway Web Interface (Recommended)

1. **Access Gateway Configuration**
   ```
   https://your-gateway:8088/main/config/modules
   ```

2. **Navigate to Modules**
   - Log in with administrator credentials
   - Go to **Config** → **System** → **Modules**

3. **Install Module**
   - Click **Install or Upgrade a Module**
   - Browse and select `Prometheus-Exporter.modl` file
   - Click **Install**

4. **Verify Installation**
   - Module should appear in the installed modules list
   - Status should show as **Running**

### Method 2: File System Installation

For automated deployments or CI/CD pipelines:

1. **Copy Module File**
   ```bash
   cp Prometheus-Exporter.modl /path/to/ignition/user-lib/modules/
   ```

2. **Restart Gateway**
   ```bash
   sudo systemctl restart ignition
   # or
   sudo service ignition restart
   ```

3. **Verify Installation**
   Check gateway logs for successful module loading:
   ```bash
   tail -f /var/log/ignition/wrapper.log | grep "Prometheus"
   ```

## Post-Installation Verification

### Check Module Status

1. **Gateway Web Interface**
   - Navigate to **Config** → **System** → **Modules**
   - Locate "Prometheus Exporter Module"
   - Verify status shows **Running**

### Verify Endpoint Availability

Test that the metrics endpoint is accessible:

```bash
curl http://your-gateway:8088/system/metrics
```

Expected response should start with:
```
# HELP jvm_memory_bytes_used Used bytes of a given JVM memory area.
# TYPE jvm_memory_bytes_used gauge
jvm_memory_bytes_used{area="heap"} 1.23456789E8
```

### Check Gateway Logs

Look for successful initialization messages:

```bash
grep "Prometheus Exporter Gateway initialized" /var/log/ignition/wrapper.log
```

Expected log entry:
```
INFO  [PrometheusExporterGatewayHook] Prometheus Exporter Gateway initialized with unified metrics registry
```

## Troubleshooting Installation

### Common Installation Issues

**Module Not Loading**
```
Problem: Module shows in list but status is "Error"
Solution: Check Ignition version compatibility and restart gateway
```

**Endpoint Not Accessible**
```
Problem: HTTP 404 error when accessing /system/metrics
Solution: Verify module is running and check web server configuration
```

**Permission Errors**
```
Problem: Installation fails with permission errors
Solution: Ensure proper file permissions and gateway service account access
```

### Diagnostic Commands

**Check Module Dependencies**
```bash
# Verify Java version
java -version

# Check Ignition process
ps aux | grep ignition
```

**Network Connectivity**
```bash
# Test gateway web interface
curl -I http://your-gateway:8088/main

# Test metrics endpoint
curl -I http://your-gateway:8088/system/metrics
```

**Log Analysis**
```bash
# Filter for module-related logs
grep -i prometheus /var/log/ignition/wrapper.log

# Check for error messages
grep ERROR /var/log/ignition/wrapper.log | tail -10
```

## Next Steps

After successful installation:

1. **[Configure the endpoint](configuration)** for your environment
2. **[View your first metrics](first-metrics)** to verify operation
3. **[Set up Prometheus scraping](../prometheus-integration/scrape-configuration)** to begin collecting data

## Uninstallation

To remove the module:

1. Navigate to **Config** → **System** → **Modules**
2. Find "Prometheus Exporter Module"
3. Click **Uninstall**
4. Confirm the removal
5. Restart gateway to complete uninstallation

The `/system/metrics` endpoint will no longer be available after uninstallation.