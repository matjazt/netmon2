# MQTT Message Format and Examples

This document describes the MQTT message format expected by Network Monitor 2 and provides examples of how to publish messages.

## Message Format

Network Monitor 2 expects JSON messages with the following structure:

```json
{
  "hostname": "Scanner",
  "timestamp": "2026-01-20T11:45:40+01:00",
  "devices": [
    {"ip": "192.168.1.1", "mac": "AA:BB:CC:DD:EE:FF"},
    {"ip": "192.168.1.2", "mac": "11:22:33:44:55:66"}
  ]
}
```

### Field Descriptions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `hostname` | string | Yes | Name of the device/scanner publishing the message |
| `timestamp` | ISO 8601 string | Yes | When the scan was performed (with timezone) |
| `devices` | array | Yes | List of devices found on the network |
| `devices[].ip` | string | Yes | Device IP address (IPv4 or IPv6) |
| `devices[].mac` | string | Yes | Device MAC address (format: AA:BB:CC:DD:EE:FF) |

### Timestamp Format

Use ISO 8601 format with timezone:

- `2026-01-20T11:45:40+01:00` (with timezone offset)
- `2026-01-20T11:45:40Z` (UTC, recommended)
- `2026-01-20T11:45:40.123Z` (with milliseconds)

## Topic Structure

Topics follow the pattern configured in `application.yaml`:

```yaml
mqtt:
  topic-template: network/{networkName}/scan
```

Example topics:

- `network/HomeNetwork/scan`
- `network/OfficeNetwork/scan`
- `network/TestLab/scan`

**Important**: The network name in the topic must match a network in the database. Add networks via SQL:

```sql
INSERT INTO network (name, first_seen, last_seen, alerting_delay)
VALUES ('HomeNetwork', NOW(), NOW(), 300);
```

## Publishing Messages

### Using Mosquitto Client

**Basic (no authentication):**

```bash
mosquitto_pub -h localhost -p 1883 \
  -t network/HomeNetwork/scan \
  -m '{"hostname":"Scanner","timestamp":"2026-01-20T12:00:00Z","devices":[{"ip":"192.168.1.1","mac":"AA:BB:CC:DD:EE:FF"}]}'
```

**With username/password:**

```bash
mosquitto_pub -h broker.example.com -p 1883 \
  -t network/HomeNetwork/scan \
  -u mqtt_user -P mqtt_password \
  -m '{"hostname":"Scanner","timestamp":"2026-01-20T12:00:00Z","devices":[{"ip":"192.168.1.1","mac":"AA:BB:CC:DD:EE:FF"}]}'
```

**With TLS/SSL:**

```bash
mosquitto_pub -h broker.example.com -p 8883 \
  -t network/HomeNetwork/scan \
  -u mqtt_user -P mqtt_password \
  --cafile /path/to/ca.crt \
  -m '{"hostname":"Scanner","timestamp":"2026-01-20T12:00:00Z","devices":[{"ip":"192.168.1.1","mac":"AA:BB:CC:DD:EE:FF"}]}'
```

**PowerShell (Windows):**

```powershell
mosquitto_pub -h localhost -p 1883 `
  -t network/HomeNetwork/scan `
  -m '{"hostname":"Scanner","timestamp":"2026-01-20T12:00:00Z","devices":[{"ip":"192.168.1.1","mac":"AA:BB:CC:DD:EE:FF"}]}'
```

### Using Python with Paho MQTT

```python
import json
import paho.mqtt.client as mqtt
from datetime import datetime, timezone

# Create client
client = mqtt.Client()
client.username_pw_set("mqtt_user", "mqtt_password")

# Connect to broker
client.connect("broker.example.com", 1883, 60)

# Create message
message = {
    "hostname": "PythonScanner",
    "timestamp": datetime.now(timezone.utc).isoformat(),
    "devices": [
        {"ip": "192.168.1.1", "mac": "AA:BB:CC:DD:EE:FF"},
        {"ip": "192.168.1.2", "mac": "11:22:33:44:55:66"}
    ]
}

# Publish
client.publish("network/HomeNetwork/scan", json.dumps(message))
client.disconnect()
```

### Using Node.js with MQTT.js

```javascript
const mqtt = require('mqtt');

// Connect
const client = mqtt.connect('mqtt://broker.example.com:1883', {
  username: 'mqtt_user',
  password: 'mqtt_password'
});

client.on('connect', () => {
  // Create message
  const message = {
    hostname: 'NodeScanner',
    timestamp: new Date().toISOString(),
    devices: [
      {ip: '192.168.1.1', mac: 'AA:BB:CC:DD:EE:FF'},
      {ip: '192.168.1.2', mac: '11:22:33:44:55:66'}
    ]
  };

  // Publish
  client.publish('network/HomeNetwork/scan', JSON.stringify(message));
  client.end();
});
```

## Example Messages

### Single Device

```json
{
  "hostname": "RouterOSScanner",
  "timestamp": "2026-01-20T14:30:00Z",
  "devices": [
    {"ip": "192.168.1.100", "mac": "AA:BB:CC:DD:EE:FF"}
  ]
}
```

### Multiple Devices

```json
{
  "hostname": "RouterOSScanner",
  "timestamp": "2026-01-20T14:30:00Z",
  "devices": [
    {"ip": "192.168.1.1", "mac": "AA:BB:CC:DD:EE:11"},
    {"ip": "192.168.1.2", "mac": "AA:BB:CC:DD:EE:22"},
    {"ip": "192.168.1.10", "mac": "AA:BB:CC:DD:EE:33"},
    {"ip": "192.168.1.20", "mac": "AA:BB:CC:DD:EE:44"}
  ]
}
```

### Empty Network (All Devices Offline)

```json
{
  "hostname": "RouterOSScanner",
  "timestamp": "2026-01-20T14:30:00Z",
  "devices": []
}
```

**Note**: An empty devices array means no devices were found online. Network Monitor will mark all previously online devices as offline.

### IPv6 Support

```json
{
  "hostname": "Scanner",
  "timestamp": "2026-01-20T14:30:00Z",
  "devices": [
    {"ip": "2001:db8::1", "mac": "AA:BB:CC:DD:EE:FF"},
    {"ip": "fe80::1234:5678:90ab:cdef", "mac": "11:22:33:44:55:66"}
  ]
}
```

## Message Processing Logic

### What Happens When a Message is Received

1. **Network Update**: `lastSeen` timestamp updated for the network
2. **Device Online**: For each device in message:
   - If new: Create device record, set to online, record state change
   - If was offline: Update to online, record state change
   - If already online: Update `lastSeen`, no state change recorded
3. **Device Offline**: For devices NOT in message:
   - If was online: Update to offline, record state change
   - If already offline: No action
4. **Alert Triggers**:
   - Unauthorized device appears online → DEVICE_UNAUTHORIZED alert
   - Message processing continues normally

### State Changes Only

Network Monitor only records **state changes** (online↔offline transitions), not every scan result. This minimizes database writes while preserving complete history.

## Testing Your Integration

### 1. Subscribe to Test Topic

Monitor your MQTT broker:

```bash
mosquitto_sub -h localhost -p 1883 -t network/#
```

### 2. Publish Test Message

```bash
mosquitto_pub -h localhost -p 1883 \
  -t network/TestNetwork/scan \
  -m '{"hostname":"Test","timestamp":"2026-01-20T12:00:00Z","devices":[{"ip":"192.168.1.99","mac":"FF:FF:FF:FF:FF:FF"}]}'
```

### 3. Check Application Logs

Look for:

```
INFO  MqttService : Received MQTT message: payload='...'
```

### 4. Verify Database

```sql
-- Check device was created
SELECT * FROM device WHERE mac_address = 'FF:FF:FF:FF:FF:FF';

-- Check state history
SELECT * FROM device_status_history 
WHERE device_id = (SELECT id FROM device WHERE mac_address = 'FF:FF:FF:FF:FF:FF')
ORDER BY timestamp DESC;
```

### 5. Query API

```bash
curl -u admin:admin http://localhost:8080/api/devices/paginated?page=0&size=20
```

## Network Scanner Examples

See the [original NetworkMonitor project](https://github.com/matjazt/NetworkMonitor/tree/main/network-scanners) for complete scanner implementations:

- **RouterOS 7**: MikroTik router script using ARP table scanning
- Additional scanner types in development

## Troubleshooting

**Message not received:**

- Verify topic matches database network name
- Check MQTT broker logs
- Ensure application subscribed successfully (check startup logs)
- Test with `mosquitto_sub` to confirm broker connectivity

**Devices not created:**

- Validate JSON format (use JSON validator)
- Check MAC address format (must be AA:BB:CC:DD:EE:FF)
- Verify timestamp is valid ISO 8601
- Review application logs for parsing errors

**State changes not recorded:**

- Check if device already in desired state (no-op if no change)
- Verify `lastSeen` timestamp updates even without state change
- Query `device_status_history` table directly

## Best Practices

1. **Consistent Scanning**: Publish scan results at regular intervals (e.g., every 60 seconds)
2. **Accurate Timestamps**: Use current time when scan completed, not when message published
3. **Complete Device Lists**: Include ALL online devices in each message, not just changes
4. **Reliable Publishers**: Ensure scanner scripts restart on failure
5. **Network Matching**: Topic network name must exactly match database (case-sensitive)
6. **QoS Level**: Use QoS 1 for reliable delivery without duplication overhead
