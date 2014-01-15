jmonit
======

A system monitor similar to monit written in java. As at the time of this writing, you can't send sms alerts from monit. Hence, jmonit

jmonit uses a json file to describe the system and what you want jmonit instance to monit.

jmonit currently runs as a single instance. jmonit is not currently distributed.

Sample json config file.

```javascript
{
	"system": {
		"system_name": "GLOWebServer",
		"ip_address": "192.168.1.1",        
		"alerts": {
			"mail":[],
			"sms": [
				"2348089370313",
			"2347062022486",
			"2348032517996"
				]
		},
		"mail_server": {
			"host": "smtp.gmail.com",
			"user": "akintayo.segun@gmail.com",
			"password": "********",
			"port": 587,
			"starttls": true,
			"use_auth": true
		},
		"smsc": {
			"sender_id": "JMonit-Alert",
			"ip_address": "10.100.126.111",
			"system_type": "jmonit",
			"system_id": "jmonit",
			"password": "******",
			"port": 5217
		},
		"monitor": {
			"memory": {  
				"ping_interval": 60000,
				"free": {                    
					"condition": "lt 10%",
					"action": [
						"alert_email",
					"alert_sms"
						]
				}
			},
			"cpu": {                
				"load_avg": {
					"ping_interval": 60000,
					"time_interval": 5,
					"condition": "gt 3",
					"action": [
						"alert_all"
						]
				}
			},
			"process": {
				"ping_interval": 60000,
				"name": "mysqld",
				"pid_file": "/var/run/mysqld/mysqld.pid",
				"start_script": "/data/etc/rc.d/my.64001 start",
				"stop_script": "/data/etc/rc.d/my.64001 stop",
				"action": [
					"alert_all",
				"restart"
					]
			},
			"output": {
				"ping_interval": 1000,
				"command": "ls -las | wc -l",
				"expected": "eq 20",
				"action": [
					"alert_sms"
					]
			},
			"port": {
				"ping_interval": 1000,
				"number": 80,
				"action": [
					"alert_all"
					]
			},
			"web": {
				"ping_interval": 1000,
				"url": "http: //localhost/pau",
				"action": [
					"alert_all"
					]
			},
			"send_output": {
				"ping_interval": 60000,
				"command": "ls -las",
				"action": [
					"alert_sms"
					]
			}
		}
	}    
}
```
