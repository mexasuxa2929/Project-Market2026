#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE products_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'products_db')\gexec
    SELECT 'CREATE DATABASE payments_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payments_db')\gexec
    SELECT 'CREATE DATABASE discounts_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'discounts_db')\gexec
    SELECT 'CREATE DATABASE analytics_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'analytics_db')\gexec
    SELECT 'CREATE DATABASE shop_db'          WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shop_db')\gexec
    SELECT 'CREATE DATABASE orders_db'        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orders_db')\gexec
    SELECT 'CREATE DATABASE notifications_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notifications_db')\gexec
    SELECT 'CREATE DATABASE delivery_db'      WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'delivery_db')\gexec
    SELECT 'CREATE DATABASE auth_db'         WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db')\gexec
    SELECT 'CREATE DATABASE geo_db'          WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'geo_db')\gexec
    SELECT 'CREATE DATABASE warehouse_db'    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'warehouse_db')\gexec
EOSQL
