# Custom Metrics via Scripting

Extend the Prometheus Exporter Module with custom application metrics using Ignition's gateway scripting capabilities.

## Custom Metrics Overview

### Scripting Integration

The module provides gateway-scoped scripting functions to register custom metrics that are automatically included in the `/system/metrics` endpoint alongside standard Dropwizard metrics.

### Available Metric Types

Custom metrics support all Prometheus metric types:
- **Counter**: Monotonically increasing values
- **Gauge**: Current state values that can increase/decrease  
- **Histogram**: Distribution tracking with configurable buckets
- **Summary**: Distribution tracking with client-side quantiles

## Basic Custom Metrics

### Counter Metrics

Count occurrences of events or operations:

```python
# Gateway startup script or scheduled script
def track_operations():
    # Increment counter for successful operations
    system.prometheus.counter("ignition_operations_total") \
        .labels({"operation": "tag_read", "status": "success"}) \
        .increment()
    
    # Increment by specific amount
    system.prometheus.counter("ignition_data_processed_bytes_total") \
        .labels({"source": "plc_1", "type": "analog"}) \
        .increment(1024)  # 1KB processed
    
    # Multiple label dimensions
    system.prometheus.counter("ignition_alarms_total") \
        .labels({
            "priority": "high", 
            "source": "production_line_1",
            "acknowledged": "false"
        }) \
        .increment()
```

### Gauge Metrics

Track current state and values:

```python
# Gateway timer script (runs every 30 seconds)
def update_system_status():
    # System uptime in hours
    startup_time = system.util.getSystemProperty("startup.time")
    uptime_hours = system.date.hoursBetween(startup_time, system.date.now())
    
    system.prometheus.gauge("ignition_uptime_hours") \
        .set(uptime_hours)
    
    # Active perspective sessions
    sessions = system.perspective.getSessionInfo()
    system.prometheus.gauge("ignition_perspective_sessions_active") \
        .set(len(sessions))
    
    # Available memory in MB
    available_memory = system.util.getAvailableMemory() / 1024 / 1024
    system.prometheus.gauge("ignition_memory_available_mb") \
        .set(available_memory)
    
    # Database connection pool status
    for pool_name in ["default", "backup"]:
        active_connections = get_active_connections(pool_name)
        system.prometheus.gauge("ignition_database_connections") \
            .labels({"pool": pool_name, "state": "active"}) \
            .set(active_connections)

def get_active_connections(pool_name):
    # Implementation depends on your database setup
    # This is a placeholder for actual connection pool monitoring
    return 5  # Example value
```

### Histogram Metrics

Track distributions of values:

```python
# Gateway script for tracking response times
def track_response_times():
    # Create histogram with custom buckets for response time tracking
    histogram = system.prometheus.histogram("ignition_response_time_seconds") \
        .labels({"service": "tag_provider", "operation": "read"}) \
        .buckets([0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0])
    
    # Simulate measuring operation time
    start_time = system.date.now()
    
    # Perform operation (example: tag read)
    tag_value = system.tag.readBlocking(["[default]Production/Line1/Status"])
    
    end_time = system.date.now()
    duration = system.date.secondsBetween(start_time, end_time)
    
    # Record the observation
    histogram.observe(duration)

# Database query performance tracking
def track_database_performance():
    query_histogram = system.prometheus.histogram("ignition_database_query_duration_seconds") \
        .labels({"query_type": "select", "table": "production_data"}) \
        .buckets([0.001, 0.01, 0.1, 1.0, 10.0])
    
    # Time the database query
    start_time = system.date.now()
    
    # Execute query
    results = system.db.runQuery(
        "SELECT COUNT(*) FROM production_data WHERE timestamp > ?", 
        [system.date.addHours(system.date.now(), -1)]
    )
    
    end_time = system.date.now()
    query_duration = system.date.secondsBetween(start_time, end_time)
    
    query_histogram.observe(query_duration)
```

## Advanced Custom Metrics

### Business Logic Metrics

Track application-specific KPIs:

```python
# Production monitoring metrics
def track_production_metrics():
    # Overall Equipment Effectiveness (OEE)
    oee_value = calculate_oee()  # Your business logic function
    system.prometheus.gauge("ignition_production_oee_percent") \
        .labels({"line": "production_line_1", "shift": "day"}) \
        .set(oee_value)
    
    # Energy consumption
    power_usage = get_current_power_usage()  # Your data source
    system.prometheus.gauge("ignition_power_consumption_kw") \
        .labels({"facility": "plant_a", "department": "manufacturing"}) \
        .set(power_usage)
    
    # Quality metrics
    defect_rate = calculate_defect_rate()
    system.prometheus.gauge("ignition_quality_defect_rate_percent") \
        .labels({"product": "widget_a", "inspector": "qc_station_1"}) \
        .set(defect_rate)
    
    # Maintenance metrics
    mtbf_hours = calculate_mean_time_between_failures()
    system.prometheus.gauge("ignition_equipment_mtbf_hours") \
        .labels({"equipment": "conveyor_belt_1", "location": "zone_3"}) \
        .set(mtbf_hours)

def calculate_oee():
    # Placeholder for actual OEE calculation
    availability = 0.95
    performance = 0.87
    quality = 0.92
    return availability * performance * quality * 100

def get_current_power_usage():
    # Read power meter tag
    power_tag = system.tag.readBlocking(["[default]Utilities/PowerMeter/CurrentKW"])[0]
    return power_tag.value if power_tag.quality.isGood() else 0

def calculate_defect_rate():
    # Query defect data from database
    total_produced = system.tag.readBlocking(["[default]Production/TotalCount"])[0].value
    total_defects = system.tag.readBlocking(["[default]Quality/DefectCount"])[0].value
    return (total_defects / total_produced * 100) if total_produced > 0 else 0

def calculate_mean_time_between_failures():
    # Historical analysis of equipment failures
    # This would query maintenance records and calculate MTBF
    return 168.5  # Example: 168.5 hours
```

### Tag System Integration

Monitor tag system performance and quality:

```python
# Tag system monitoring
def monitor_tag_system():
    # Tag quality distribution
    tag_paths = system.tag.browseTags("[default]Production").results
    
    good_tags = 0
    bad_tags = 0
    uncertain_tags = 0
    
    for tag_path in tag_paths:
        tag_result = system.tag.readBlocking([tag_path.fullPath])[0]
        
        if tag_result.quality.isGood():
            good_tags += 1
        elif tag_result.quality.isBad():
            bad_tags += 1
        else:
            uncertain_tags += 1
    
    # Update quality metrics
    system.prometheus.gauge("ignition_tag_quality_total") \
        .labels({"quality": "good", "provider": "default"}) \
        .set(good_tags)
    
    system.prometheus.gauge("ignition_tag_quality_total") \
        .labels({"quality": "bad", "provider": "default"}) \
        .set(bad_tags)
    
    system.prometheus.gauge("ignition_tag_quality_total") \
        .labels({"quality": "uncertain", "provider": "default"}) \
        .set(uncertain_tags)
    
    # Tag subscription metrics
    subscription_count = get_active_subscription_count()
    system.prometheus.gauge("ignition_tag_subscriptions") \
        .labels({"provider": "default", "type": "realtime"}) \
        .set(subscription_count)

def get_active_subscription_count():
    # This would need access to internal subscription manager
    # Placeholder implementation
    return 150
```

## Performance Considerations

### Efficient Metric Registration

Register metrics once and reuse instances:

```python
# Global metric instances (declare once at module level)
operation_counter = None
response_time_histogram = None

def initialize_metrics():
    """Call this once during gateway startup"""
    global operation_counter, response_time_histogram
    
    operation_counter = system.prometheus.counter("ignition_operations_total") \
        .labels({"service": "tag_provider"})
    
    response_time_histogram = system.prometheus.histogram("ignition_response_time_seconds") \
        .labels({"service": "database"}) \
        .buckets([0.001, 0.01, 0.1, 1.0, 10.0])

def record_operation():
    """Efficient metric updates using pre-registered instances"""
    global operation_counter, response_time_histogram
    
    if operation_counter is None:
        initialize_metrics()
    
    # Fast increment operation
    operation_counter.increment()
    
    # Measure and record timing
    start_time = system.date.now()
    # ... perform operation ...
    duration = system.date.secondsBetween(start_time, system.date.now())
    response_time_histogram.observe(duration)
```

### Cardinality Management

Control label cardinality to avoid performance issues:

```python
# Bad: Unbounded cardinality
def bad_metric_example():
    user_id = get_current_user_id()  # Could be thousands of unique values
    system.prometheus.counter("user_actions_total") \
        .labels({"user_id": str(user_id)}) \
        .increment()

# Good: Bounded cardinality
def good_metric_example():
    user_type = get_user_type()  # Limited set: admin, operator, viewer
    department = get_user_department()  # Limited set: production, maintenance, quality
    
    system.prometheus.counter("user_actions_total") \
        .labels({"user_type": user_type, "department": department}) \
        .increment()

def get_user_type():
    # Classify users into broad categories
    current_user = system.security.getUsername()
    
    if system.security.hasRole(current_user, "Administrator"):
        return "admin"
    elif system.security.hasRole(current_user, "Operator"):
        return "operator"
    else:
        return "viewer"
```

## Script Deployment Patterns

### Gateway Startup Scripts

Register persistent metrics during gateway startup:

```python
# Gateway startup script
def on_gateway_startup():
    """Initialize custom metrics when gateway starts"""
    
    # Register system health metrics
    system.prometheus.gauge("ignition_system_health_score") \
        .labels({"component": "database"}) \
        .set(100)  # Initial healthy state
    
    system.prometheus.gauge("ignition_system_health_score") \
        .labels({"component": "tag_provider"}) \
        .set(100)
    
    # Initialize counters at zero
    system.prometheus.counter("ignition_startup_events_total") \
        .labels({"type": "clean_startup"}) \
        .increment()
    
    logger = system.util.getLogger("CustomMetrics")
    logger.info("Custom Prometheus metrics initialized")
```

### Scheduled Scripts

Update metrics on regular intervals:

```python
# Timer script (every 60 seconds)
def scheduled_metric_update():
    """Update metrics that require periodic calculation"""
    
    try:
        # Update system status metrics
        update_system_health()
        
        # Update business metrics
        update_production_kpis()
        
        # Update performance metrics
        update_performance_indicators()
        
    except Exception as e:
        # Log errors but don't break the metric collection
        logger = system.util.getLogger("CustomMetrics")
        logger.error("Error updating custom metrics: %s" % str(e))
        
        # Increment error counter
        system.prometheus.counter("ignition_metric_errors_total") \
            .labels({"script": "scheduled_update", "type": "exception"}) \
            .increment()

def update_system_health():
    # Database connectivity check
    try:
        system.db.runQuery("SELECT 1")
        db_health = 100
    except:
        db_health = 0
    
    system.prometheus.gauge("ignition_system_health_score") \
        .labels({"component": "database"}) \
        .set(db_health)
```

### Event-Driven Metrics

Update metrics based on system events:

```python
# Tag change script or alarm pipeline script
def on_alarm_state_change(event):
    """Update alarm metrics when alarm state changes"""
    
    alarm_name = event.getAlarmName()
    new_state = event.getNewState()
    priority = event.getPriority()
    
    # Update alarm state counters
    if new_state == "Active":
        system.prometheus.counter("ignition_alarms_activated_total") \
            .labels({
                "priority": priority,
                "source": alarm_name.split("/")[0]  # First part of alarm path
            }) \
            .increment()
    
    elif new_state == "Normal":
        system.prometheus.counter("ignition_alarms_cleared_total") \
            .labels({
                "priority": priority,
                "source": alarm_name.split("/")[0]
            }) \
            .increment()
```

## Testing Custom Metrics

### Verification Steps

Confirm custom metrics appear in the endpoint:

```bash
# Check for custom metrics in output
curl -s http://gateway:8088/system/metrics | grep ignition_operations_total

# Verify metric format
curl -s http://gateway:8088/system/metrics | grep -A3 "TYPE ignition_operations_total"

# Count custom metrics
curl -s http://gateway:8088/system/metrics | grep -c "ignition_.*\{"
```

### Script Testing

Test metric registration in Ignition's script console:

```python
# Test script in Gateway > Scripting > Script Console
def test_custom_metrics():
    # Test counter increment
    system.prometheus.counter("test_counter") \
        .labels({"test": "true"}) \
        .increment()
    
    # Test gauge setting
    system.prometheus.gauge("test_gauge") \
        .set(42.5)
    
    print("Test metrics registered. Check /system/metrics endpoint.")

# Run the test
test_custom_metrics()
```

## Troubleshooting Custom Metrics

### Common Issues

**Metrics Not Appearing**
```python
# Debug metric registration
try:
    system.prometheus.counter("debug_counter").increment()
    print("Metric registration successful")
except Exception as e:
    print("Metric registration failed: %s" % str(e))
```

**Label Format Issues**
```python
# Ensure labels are strings
labels = {
    "numeric_id": str(12345),  # Convert numbers to strings
    "boolean_flag": str(True).lower(),  # Convert booleans
    "clean_name": "value_with_underscores"  # Use underscores, not spaces
}

system.prometheus.counter("test_metric") \
    .labels(labels) \
    .increment()
```

**Performance Problems**
```bash
# Monitor metric collection time before/after adding custom metrics
time curl -s http://gateway:8088/system/metrics > /dev/null
```

## Best Practices

### Naming Conventions

Follow Prometheus naming conventions:

- Use underscores, not hyphens or spaces
- Include unit suffix for time/bytes (_seconds, _bytes)
- Use descriptive base names (ignition_database_connections)
- Include _total suffix for counters when appropriate

### Label Design

Design labels for efficient querying and alerting:

- Keep cardinality reasonable (&lt;1000 unique combinations per metric)
- Use consistent label names across related metrics
- Avoid labels that change frequently
- Group related dimensions into single metrics with labels

## Next Steps

After implementing custom metrics:

1. **[Create dashboards](../examples/dashboard-creation)** to visualize custom metrics
2. **[Set up alerts](../examples/alerting-setup)** based on business logic
3. **[Monitor performance impact](../troubleshooting/performance)** of custom metrics