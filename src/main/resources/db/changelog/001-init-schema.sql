--liquibase formatted sql
--changeset hieu:001

-- =========================
-- 1. AUTH & SECURITY
-- =========================

CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE roles (
    role_id INT AUTO_INCREMENT PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO roles(role_name) VALUES ('ADMIN'), ('USER');

CREATE TABLE user_roles (
    user_id BIGINT,
    role_id INT,
    PRIMARY KEY(user_id, role_id),
    FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY(role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

CREATE TABLE refresh_contexts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    context_id VARCHAR(64) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE blacklisted_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(512) NOT NULL UNIQUE,
    user_id BIGINT,
    reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL DEFAULT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE verification_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL UNIQUE,
    type ENUM('EMAIL_VERIFICATION', 'RESET_PASSWORD', 'CHANGE_EMAIL') NOT NULL,
    email_target VARCHAR(100),
    expiry_date TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- =========================
-- 2. DOMAIN: LIBRARY
-- =========================

CREATE TABLE categories (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE books (
    book_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(100),
    isbn VARCHAR(50) UNIQUE,
    quantity INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    category_id BIGINT,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL
);

CREATE TABLE members (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    full_name VARCHAR(100) NOT NULL,
    dob DATE,
    address VARCHAR(255),
    phone VARCHAR(20) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE borrow_records (
    record_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT,
    book_id BIGINT,
    borrow_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    return_date DATE,
    status ENUM('BORROWED','RETURNED') DEFAULT 'BORROWED',
    due_date DATE,
    UNIQUE(member_id, book_id, status),
    FOREIGN KEY(member_id) REFERENCES members(member_id) ON DELETE CASCADE,
    FOREIGN KEY(book_id) REFERENCES books(book_id) ON DELETE CASCADE
);

CREATE TABLE system_config (
    config_key VARCHAR(100) PRIMARY KEY,
    config_value VARCHAR(100) NOT NULL
);

INSERT INTO system_config(config_key, config_value)
VALUES ('maintenance_mode', 'OFF');

INSERT INTO categories(category_name, description)
VALUES ('General', 'Default category for uncategorized books');

--rollback DROP TABLE borrow_records, members, books, categories, verification_tokens, blacklisted_tokens, refresh_contexts, user_roles, roles, users, system_config;
