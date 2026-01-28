DO NOT RUN THIS FILE DIRECTLY!!!!
DO NOT COMMENT THE LINE ABOVE!!!!

drop table device_status_history ;
drop table device ;
drop table network ;


SELECT
    *
FROM
    device_status_history
where device_id = 49
order by "timestamp" desc 

SELECT
    *
FROM
    device_status_history
ORDER BY
    mac_address,
    timestamp DESC;

SELECT
    n.name AS network,
    d.mac_address,
    d.ip_address,
    COUNT(*) FILTER (
        WHERE
            d.online = false
    ) as offline_count,
    COUNT(*) FILTER (
        WHERE
            d.online = true
    ) as online_count,
    MAX(d.timestamp) AS last_seen
FROM
    device_status_history d
    JOIN network n ON d.network_id = n.id
GROUP BY
    n.name,
    d.mac_address,
    d.ip_address
ORDER BY
    offline_count DESC,
    n.name,
    d.mac_address;

select de1_0.id,de1_0.active_alert_id,de1_0.device_operation_mode_id,de1_0.device_operation_mode_id,de1_0.first_seen,de1_0.ip_address,de1_0.last_seen,de1_0.mac_address,de1_0.name,de1_0.network_id,de1_0.online from device de1_0 where de1_0.network_id=1

-- dodaj indeks
select
	dshe1_0.id,
	dshe1_0.device_id,
	dshe1_0.ip_address,
	dshe1_0.network_id,
	dshe1_0.online,
	dshe1_0.timestamp
from
	device_status_history dshe1_0
where
	dshe1_0.network_id = 1
	and dshe1_0.online = true
	and dshe1_0.timestamp =(
	select
		max(dshe2_0.timestamp)
	from
		device_status_history dshe2_0
	where
		dshe2_0.device_id = dshe1_0.device_id)
order by
	dshe1_0.timestamp desc

	
select
	dshe1_0.id,
	dshe1_0.device_id,
	dshe1_0.ip_address,
	dshe1_0.network_id,
	dshe1_0.online,
	dshe1_0.timestamp
from
	device_status_history dshe1_0
where
	dshe1_0.network_id = 1
	and dshe1_0.online = true
	and dshe1_0.timestamp =(
	select
		max(dshe2_0.timestamp)
	from
		device_status_history dshe2_0
	where
		dshe2_0.device_id = dshe1_0.device_id)
order by
	dshe1_0.timestamp desc
	
select
		dshe2_0.device_id, max(dshe2_0.timestamp)
	from
		device_status_history dshe2_0
		group by dshe2_0.device_id 

SELECT * FROM device_status_history h 
WHERE h.network_id = 2
AND h.device_id = 42
ORDER BY h.timestamp DESC
LIMIT 1

SELECT * FROM device_status_history h 
WHERE h.network_id = 1
order by h."timestamp" desc

select * from alert a 
where a.network_id = 1
and a.device_id is null
order by a."timestamp" desc

select * from network n 

select * from alert order by id desc

select
    *
from
    device;

select
    *
from
    network;

alter TABLE network
drop column alertingdelay;

insert into account_type (id, name, description) values (1, 'admin', 'administrator'), (2, 'user', 'ordinary user'), (3, 'device', 'montoring device')



-- Create operation_mode reference table with zero-based IDs
CREATE TABLE device_operation_mode (
    id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

-- Insert operation modes with zero-based IDs matching enum ordinals
INSERT INTO device_operation_mode (id, name, description) VALUES
    (0, 'NOT_ALLOWED', 'Device is not allowed on the network'),
    (1, 'ALLOWED', 'Device is allowed but not monitored'),
    (2, 'ALWAYS_ON', 'Device should always be online and is monitored');


update device set device_operation_mode_id = device_operation_mode_id -1

ALTER TABLE device 
ADD CONSTRAINT fk_device_device_operation_mode 
FOREIGN KEY (device_operation_mode_id) REFERENCES device_operation_mode(id);


ALTER TABLE account 
ADD CONSTRAINT fk_account_account_type 
FOREIGN KEY (account_type_id) REFERENCES account_type (id);


-- Step 1: Create alert_type reference table with zero-based IDs matching enum ordinals
CREATE TABLE alert_type (
    id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

-- Step 2: Insert the three alert types with zero-based IDs matching enum
INSERT INTO alert_type (id, name, description) VALUES
    (0, 'NETWORK_DOWN', 'Network connectivity lost or network went offline'),
    (1, 'DEVICE_DOWN', 'Device that should always be online is not responding'),
    (2, 'DEVICE_NOT_ALLOWED', 'Unauthorized device detected on the network');

-- Step 3: Create alert table
CREATE TABLE alert (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    network_id BIGINT NOT NULL,
    device_id BIGINT,
    alert_type_id INTEGER NOT NULL,
    message VARCHAR(500) NOT NULL,
    CONSTRAINT fk_alert_network FOREIGN KEY (network_id) REFERENCES network(id),
    CONSTRAINT fk_alert_device FOREIGN KEY (device_id) REFERENCES device(id),
    CONSTRAINT fk_alert_type FOREIGN KEY (alert_type_id) REFERENCES alert_type(id)
);

-- Step 4: Create indexes for better query performance
CREATE INDEX idx_alert_network ON alert(network_id);
CREATE INDEX idx_alert_device ON alert(device_id);
CREATE INDEX idx_alert_timestamp ON alert(timestamp);


ALTER TABLE alert
  ADD CONSTRAINT fk_alert_alert_type
  FOREIGN KEY (alert_type_id) REFERENCES alert_type(id);

ALTER TABLE alert
  ADD CONSTRAINT fk_alert_device
  FOREIGN KEY (device_id) REFERENCES device(id);

ALTER TABLE device
  ADD CONSTRAINT fk_devicet_network
  FOREIGN KEY (network_id) REFERENCES network(id);

ALTER TABLE alert
  ADD CONSTRAINT fk_alert_network
  FOREIGN KEY (network_id) REFERENCES network(id);

ALTER TABLE account_network
  ADD CONSTRAINT fk_account_network_network
  FOREIGN KEY (network_id) REFERENCES network(id);

ALTER TABLE account_network
  ADD CONSTRAINT fk_account_network_account
  FOREIGN KEY (account_id) REFERENCES account(id);

-- reset history and all alerts
delete from alert where id > 0;
delete from device_status_history where id > 0;
update network set active_alert_id = null, last_seen = first_seen ;
update device set active_alert_id = null, last_seen = first_seen ;

-- manual cleanup of test devices
delete from alert where device_id in (82,84,77, 78, 81);
delete from device_status_history where device_id in (82,84,77, 78, 81);
delete from device where id in (82,84,77, 78, 81);


-- changes 2026-01-28
------------------------------------------------------------------------

ALTER TABLE network ADD COLUMN configuration varchar;
UPDATE network SET configuration = '{}';
ALTER TABLE network ALTER COLUMN configuration SET NOT NULL;

ALTER TABLE device ADD COLUMN vendor varchar;

ALTER TABLE network ADD COLUMN reporting_interval_ema int;
UPDATE network SET reporting_interval_ema = 0;
ALTER TABLE network ALTER COLUMN reporting_interval_ema SET NOT NULL;

ALTER TABLE alert ADD COLUMN last_notification_timestamp timestamp;
UPDATE alert SET last_notification_timestamp = "timestamp";
ALTER TABLE alert ALTER COLUMN last_notification_timestamp SET NOT NULL;

ALTER TABLE network ADD COLUMN back_online_time  timestamp;

------------------------------------------------------------------------
