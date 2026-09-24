-- DriveEase schema (MySQL 8+). The app runs this automatically on startup,
-- but you can also run it by hand in MySQL Workbench / the mysql client.

CREATE DATABASE IF NOT EXISTS driveease CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE driveease;

CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(80)  NOT NULL,
    email         VARCHAR(254) NOT NULL,
    phone         VARCHAR(20)  NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    DATETIME     NOT NULL,          -- stored in UTC
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cars (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    category      VARCHAR(30)  NOT NULL,
    description   VARCHAR(255) NOT NULL,
    seats         INT          NOT NULL,
    transmission  VARCHAR(20)  NOT NULL,
    fuel          VARCHAR(20)  NOT NULL,
    price_per_day INT          NOT NULL,
    popular       TINYINT(1)   NOT NULL DEFAULT 0,
    active        TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bookings (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT         NOT NULL,
    car_id          INT         NOT NULL,
    pickup_date     DATE        NOT NULL,
    return_date     DATE        NOT NULL,
    pickup_location VARCHAR(50) NOT NULL,
    drop_location   VARCHAR(50) NOT NULL,
    total_price     INT         NOT NULL,
    status          ENUM('confirmed','cancelled') NOT NULL DEFAULT 'confirmed',
    created_at      DATETIME    NOT NULL,         -- stored in UTC
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookings_car  FOREIGN KEY (car_id)  REFERENCES cars(id),
    INDEX idx_bookings_user (user_id),
    INDEX idx_bookings_car_dates (car_id, pickup_date, return_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Login sessions. Only a SHA-256 hash of the cookie token is stored.
CREATE TABLE IF NOT EXISTS sessions (
    token_hash CHAR(64) PRIMARY KEY,
    user_id    INT      NOT NULL,
    expires_at DATETIME NOT NULL,                 -- UTC
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_sessions_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
