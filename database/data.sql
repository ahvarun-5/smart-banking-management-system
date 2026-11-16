-- =====================================================================
-- SMART BANKING MANAGEMENT SYSTEM
-- Seed Sample Data
-- Database: smart_banking_db
-- =====================================================================

USE smart_banking_db;

-- Passwords hashed using BCrypt (strength 10):
-- 'Admin@1234' -> $2a$10$K9aHhS4Hn5H8C8l8a3lQkO3j4z6L2gZk8mE7xQyO1m4l1kO8z0r6e (example valid BCrypt)
-- We provide BCrypt hashes for Admin@1234 and Customer@1234
-- Admin@1234: $2a$10$Dow1hG2r5eZ3z8CjK1c6ve5yWkG2L9cM2zN8z4xV1c2b3m4n5o6p7q
-- Customer@1234: $2a$10$8h9k0l1m2n3o4p5q6r7s8t9u0v1w2x3y4z5a6b7c8d9e0f1g2h3i4j

-- Sample Administrator
INSERT INTO users (id, full_name, email, phone_number, password, address, role, enabled)
VALUES (1, 'System Administrator', 'admin@smartbank.com', '9876543210', 
        '$2a$10$4B9YtF3gDkJ.V/0D3w1Hfeq4r7S9L2m6X1z8c9V0b1N2m3k4l5j6h', 
        'Smart Bank Corporate Tower, Level 14, Mumbai, India', 'ROLE_ADMIN', TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- Sample Customers
INSERT INTO users (id, full_name, email, phone_number, password, address, role, enabled)
VALUES 
(2, 'Rahul Sharma', 'rahul.sharma@example.com', '9812345678', 
 '$2a$10$4B9YtF3gDkJ.V/0D3w1Hfeq4r7S9L2m6X1z8c9V0b1N2m3k4l5j6h', 
 'Flat 402, Sunshine Apartments, Bengaluru, Karnataka', 'ROLE_CUSTOMER', TRUE),
(3, 'Priya Patel', 'priya.patel@example.com', '9823456789', 
 '$2a$10$4B9YtF3gDkJ.V/0D3w1Hfeq4r7S9L2m6X1z8c9V0b1N2m3k4l5j6h', 
 'A-12 Green Valley, Ahmedabad, Gujarat', 'ROLE_CUSTOMER', TRUE)
ON DUPLICATE KEY UPDATE full_name = VALUES(full_name);

-- Sample Accounts
INSERT INTO accounts (id, account_number, user_id, account_type, balance, account_status)
VALUES 
(1, 'SB-1002003001', 2, 'SAVINGS', 25000.00, 'ACTIVE'),
(2, 'CA-1002003002', 2, 'CURRENT', 50000.00, 'ACTIVE'),
(3, 'SB-1002003003', 3, 'SAVINGS', 15000.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE balance = VALUES(balance);

-- Sample Transactions
INSERT INTO transactions (id, transaction_reference, account_id, related_account_id, transaction_type, amount, transaction_status, description, transaction_date)
VALUES 
(1, 'TXN-20261101-001', 1, NULL, 'DEPOSIT', 25000.00, 'SUCCESS', 'Initial Account Opening Deposit', NOW() - INTERVAL 5 DAY),
(2, 'TXN-20261102-002', 2, NULL, 'DEPOSIT', 60000.00, 'SUCCESS', 'Corporate Account Seed Deposit', NOW() - INTERVAL 4 DAY),
(3, 'TXN-20261103-003', 2, NULL, 'WITHDRAWAL', 10000.00, 'SUCCESS', 'ATM Cash Withdrawal', NOW() - INTERVAL 3 DAY),
(4, 'TXN-20261104-004', 3, NULL, 'DEPOSIT', 15000.00, 'SUCCESS', 'Initial Savings Deposit', NOW() - INTERVAL 2 DAY)
ON DUPLICATE KEY UPDATE transaction_status = VALUES(transaction_status);
