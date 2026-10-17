-- Initialize Subscribly Microservice Databases
CREATE DATABASE users_db;
CREATE DATABASE subscriptions_db;
CREATE DATABASE payments_db;
CREATE DATABASE notification_db;

CREATE DATABASE monolith_db;

CREATE USER subscribly_user WITH PASSWORD '1';

GRANT ALL PRIVILEGES ON DATABASE users_db TO subscribly_user;
GRANT ALL PRIVILEGES ON DATABASE subscriptions_db TO subscribly_user;
GRANT ALL PRIVILEGES ON DATABASE payments_db TO subscribly_user;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO subscribly_user;
GRANT ALL PRIVILEGES ON DATABASE monolith_db TO subscribly_user;

\c users_db
GRANT ALL ON SCHEMA public TO subscribly_user;
\c subscriptions_db
GRANT ALL ON SCHEMA public TO subscribly_user;
\c payments_db
GRANT ALL ON SCHEMA public TO subscribly_user;
\c notification_db
GRANT ALL ON SCHEMA public TO subscribly_user;
\c monolith_db
GRANT ALL ON SCHEMA public TO subscribly_user;
