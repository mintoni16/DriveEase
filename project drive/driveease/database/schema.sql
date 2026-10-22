-- DriveEase MySQL schema. Db.init() runs these statements automatically.
-- For manual setup, create/select the database before running the table definitions.
CREATE DATABASE IF NOT EXISTS driveease CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE driveease;

CREATE TABLE IF NOT EXISTS users (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(20) NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cars (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    seats TINYINT UNSIGNED NOT NULL DEFAULT 5,
    transmission VARCHAR(30) NOT NULL,
    fuel VARCHAR(30) NOT NULL,
    price_per_day INT UNSIGNED NOT NULL,
    popular TINYINT(1) NOT NULL DEFAULT 0,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_cars_active_category (active, category),
    INDEX idx_cars_active_popular (active, popular, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bookings (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id INT UNSIGNED NOT NULL,
    car_id INT UNSIGNED NOT NULL,
    pickup_date DATE NOT NULL,
    return_date DATE NOT NULL,
    pickup_location VARCHAR(80) NOT NULL,
    drop_location VARCHAR(80) NOT NULL,
    total_price INT UNSIGNED NOT NULL,
    status ENUM('confirmed', 'cancelled') NOT NULL DEFAULT 'confirmed',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_bookings_user_dates (user_id, pickup_date, id),
    INDEX idx_bookings_car_availability (car_id, status, pickup_date, return_date),
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_car FOREIGN KEY (car_id) REFERENCES cars(id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT chk_booking_dates CHECK (return_date > pickup_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sessions (
    token_hash CHAR(64) NOT NULL,
    user_id INT UNSIGNED NOT NULL,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (token_hash),
    INDEX idx_sessions_user (user_id),
    INDEX idx_sessions_expiry (expires_at),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id)
        ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
