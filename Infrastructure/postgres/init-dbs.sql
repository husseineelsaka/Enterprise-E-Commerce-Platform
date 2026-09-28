SELECT 'CREATE DATABASE productdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'productdb')\gexec
SELECT 'CREATE DATABASE inventorydb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventorydb')\gexec
SELECT 'CREATE DATABASE orderdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orderdb')\gexec
SELECT 'CREATE DATABASE paymentdb' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'paymentdb')\gexec
