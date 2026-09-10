-- BankOps Nexus MySQL 8 本地初始化脚本
-- 使用 MySQL 管理员执行：mysql -uroot -p < database/mysql-init.sql

CREATE DATABASE IF NOT EXISTS bank_ops
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

CREATE USER IF NOT EXISTS 'yinhang'@'localhost' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';
ALTER USER 'yinhang'@'localhost' IDENTIFIED BY 'CHANGE_ME_STRONG_PASSWORD';

GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
    ON bank_ops.* TO 'yinhang'@'localhost';

FLUSH PRIVILEGES;
