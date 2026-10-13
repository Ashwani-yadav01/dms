-- Initialize databases for DMS microservices if they do not exist
SELECT 'CREATE DATABASE "userDb"' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'userDb')\gexec
SELECT 'CREATE DATABASE incident_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'incident_db')\gexec
SELECT 'CREATE DATABASE rescue_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'rescue_db')\gexec
SELECT 'CREATE DATABASE hospital_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'hospital_db')\gexec
SELECT 'CREATE DATABASE logistics_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'logistics_db')\gexec
SELECT 'CREATE DATABASE notification_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')\gexec
