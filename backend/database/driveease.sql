CREATE DATABASE IF NOT EXISTS driveease;
USE driveease;

CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    phone VARCHAR(30),
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS cars (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    price_per_day DECIMAL(10,2) NOT NULL,
    seats INT NOT NULL,
    transmission VARCHAR(30) NOT NULL,
    available BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS bookings (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    car_id INT NOT NULL,
    pickup_date DATE NOT NULL,
    return_date DATE NOT NULL,
    total_price DECIMAL(10,2) NOT NULL,
    status VARCHAR(30) DEFAULT 'CONFIRMED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_booking_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_booking_car FOREIGN KEY (car_id) REFERENCES cars(id)
);

INSERT INTO cars(name,brand,type,price_per_day,seats,transmission,available)
SELECT 'Swift','Maruti Suzuki','Hatchback',2499,5,'Manual',TRUE
WHERE NOT EXISTS (SELECT 1 FROM cars WHERE name='Swift');

INSERT INTO cars(name,brand,type,price_per_day,seats,transmission,available)
SELECT 'City','Honda','Sedan',3299,5,'Automatic',TRUE
WHERE NOT EXISTS (SELECT 1 FROM cars WHERE name='City');

INSERT INTO cars(name,brand,type,price_per_day,seats,transmission,available)
SELECT 'Creta','Hyundai','SUV',4499,5,'Automatic',TRUE
WHERE NOT EXISTS (SELECT 1 FROM cars WHERE name='Creta');

INSERT INTO cars(name,brand,type,price_per_day,seats,transmission,available)
SELECT 'XUV700','Mahindra','SUV',4999,7,'Automatic',TRUE
WHERE NOT EXISTS (SELECT 1 FROM cars WHERE name='XUV700');
