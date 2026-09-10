-- BankOps Nexus MySQL 8 数据库结构参考
-- 正式运行时由 Spring Data JPA / Hibernate 自动维护表结构

CREATE DATABASE IF NOT EXISTS bank_ops
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE bank_ops;

CREATE TABLE user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(128) NOT NULL,
    password_salt VARCHAR(64) NOT NULL,
    display_name VARCHAR(40) NOT NULL,
    account_role VARCHAR(20) NOT NULL,
    specialty_module VARCHAR(30),
    specialist_duty VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts INT NOT NULL DEFAULT 0,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_user_status (status),
    INDEX idx_user_role (account_role)
);

CREATE TABLE login_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(30) NOT NULL,
    success BOOLEAN NOT NULL,
    reason VARCHAR(80) NOT NULL,
    ip_address VARCHAR(64),
    login_time TIMESTAMP NOT NULL,
    INDEX idx_audit_time (login_time),
    INDEX idx_audit_username (username)
);

CREATE TABLE work_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_no VARCHAR(30) NOT NULL UNIQUE,
    title VARCHAR(100) NOT NULL,
    category VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    system_name VARCHAR(60) NOT NULL,
    assignee VARCHAR(40),
    description VARCHAR(1000),
    created_by VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_order_status (status),
    INDEX idx_order_updated (updated_at)
);

CREATE TABLE asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_code VARCHAR(30) NOT NULL UNIQUE,
    asset_name VARCHAR(80) NOT NULL,
    asset_type VARCHAR(30) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    environment VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    owner VARCHAR(40) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_asset_status (status),
    INDEX idx_asset_environment (environment)
);

CREATE TABLE governance_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module VARCHAR(30) NOT NULL,
    record_no VARCHAR(40) NOT NULL UNIQUE,
    title VARCHAR(120) NOT NULL,
    record_type VARCHAR(40) NOT NULL,
    department VARCHAR(40) NOT NULL,
    category VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    owner VARCHAR(40) NOT NULL,
    planned_date DATE,
    priority VARCHAR(20),
    risk_level VARCHAR(20),
    version_no VARCHAR(30),
    score INT,
    compliant BOOLEAN NOT NULL DEFAULT TRUE,
    details VARCHAR(1500),
    created_by VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_governance_module (module),
    INDEX idx_governance_status (status),
    INDEX idx_governance_department (department),
    INDEX idx_governance_updated (updated_at)
);
