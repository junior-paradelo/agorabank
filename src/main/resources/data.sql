-- ============================================
-- ROLES
-- ============================================
INSERT INTO roles (id, name) VALUES
    ('550e8400-e29b-41d4-a716-446655440001'::uuid, 'ROLE_USER'),
    ('550e8400-e29b-41d4-a716-446655440002'::uuid, 'ROLE_ADMIN');

-- ============================================
-- AUTH USERS
-- "password123"
-- ============================================
INSERT INTO auth_users (id, username, email, password_hash, enabled, created_at, updated_at) VALUES
    ('650e8400-e29b-41d4-a716-446655440001'::uuid, 'john.doe', 'john.doe@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
    ('650e8400-e29b-41d4-a716-446655440002'::uuid, 'jane.admin', 'jane.admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW()),
    ('650e8400-e29b-41d4-a716-446655440003'::uuid, 'bob.smith', 'bob.smith@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', true, NOW(), NOW());

-- ============================================
-- USER ROLES (Many-to-Many)
-- ============================================
INSERT INTO user_roles (user_id, role_id) VALUES
    ('650e8400-e29b-41d4-a716-446655440001'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid), -- john.doe -> ROLE_USER
    ('650e8400-e29b-41d4-a716-446655440002'::uuid, '550e8400-e29b-41d4-a716-446655440002'::uuid), -- jane.admin -> ROLE_ADMIN
    ('650e8400-e29b-41d4-a716-446655440003'::uuid, '550e8400-e29b-41d4-a716-446655440001'::uuid); -- bob.smith -> ROLE_USER

-- ============================================
-- CUSTOMERS
-- ============================================
INSERT INTO customers (email, first_name, last_name, phone_number, document_type, document_number, status, created_at, updated_at) VALUES
    ('john.doe@example.com', 'John', 'Doe', '+34600123456', 'DNI', '12345678A', 'ACTIVE', NOW(), NOW()),
    ('bob.smith@example.com', 'Bob', 'Smith', '+34600789012', 'NIE', 'X9876543B', 'ACTIVE', NOW(), NOW()),
    ('alice.wonder@example.com', 'Alice', 'Wonder', '+34600345678', 'DNI', '87654321C', 'ACTIVE', NOW(), NOW());

-- ============================================
-- ACCOUNTS
-- ============================================
INSERT INTO accounts (id, customer_id, account_number, type, currency, status, available_balance, created_at, updated_at, version) VALUES
    ('750e8400-e29b-41d4-a716-446655440001'::uuid, 1, 'ES7921000813610123456789', 'CHECKING', 'EUR', 'ACTIVE', 1500.00, NOW(), NOW(), 0),
    ('750e8400-e29b-41d4-a716-446655440002'::uuid, 1, 'ES7921000813610123456790', 'SAVINGS', 'EUR', 'ACTIVE', 5000.00, NOW(), NOW(), 0),
    ('750e8400-e29b-41d4-a716-446655440003'::uuid, 2, 'ES7921000813610987654321', 'CHECKING', 'EUR', 'ACTIVE', 2500.00, NOW(), NOW(), 0),
    ('750e8400-e29b-41d4-a716-446655440004'::uuid, 3, 'ES7921000813610111111111', 'CHECKING', 'EUR', 'ACTIVE', 800.00, NOW(), NOW(), 0);

-- ============================================
-- TRANSACTIONS
-- ============================================
INSERT INTO transactions (id, account_id, counterparty_account_id, type, amount, currency, status, reference, correlation_id, balance_after, created_at) VALUES
    ('850e8400-e29b-41d4-a716-446655440001'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, NULL, 'DEPOSIT', 1000.00, 'EUR', 'POSTED', 'Initial deposit', 'CORR-001', 1000.00, NOW() - INTERVAL '7 days'),
    ('850e8400-e29b-41d4-a716-446655440002'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, NULL, 'DEPOSIT', 500.00, 'EUR', 'POSTED', 'Salary payment', 'CORR-002', 1500.00, NOW() - INTERVAL '2 days'),
    ('850e8400-e29b-41d4-a716-446655440003'::uuid, '750e8400-e29b-41d4-a716-446655440002'::uuid, NULL, 'DEPOSIT', 5000.00, 'EUR', 'POSTED', 'Savings deposit', 'CORR-003', 5000.00, NOW() - INTERVAL '10 days'),
    ('850e8400-e29b-41d4-a716-446655440004'::uuid, '750e8400-e29b-41d4-a716-446655440003'::uuid, NULL, 'DEPOSIT', 2500.00, 'EUR', 'POSTED', 'Initial deposit', 'CORR-004', 2500.00, NOW() - INTERVAL '5 days'),
    ('850e8400-e29b-41d4-a716-446655440005'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, '750e8400-e29b-41d4-a716-446655440003'::uuid, 'TRANSFER_OUT', 200.00, 'EUR', 'POSTED', 'Transfer to Bob', 'CORR-005', 1300.00, NOW() - INTERVAL '1 day'),
    ('850e8400-e29b-41d4-a716-446655440006'::uuid, '750e8400-e29b-41d4-a716-446655440003'::uuid, '750e8400-e29b-41d4-a716-446655440001'::uuid, 'TRANSFER_IN', 200.00, 'EUR', 'POSTED', 'Transfer from John', 'CORR-005', 2700.00, NOW() - INTERVAL '1 day');