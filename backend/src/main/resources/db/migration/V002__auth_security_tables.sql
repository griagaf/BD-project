CREATE TABLE users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    personnel_id BIGINT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    CHECK (username <> ''),
    CHECK (display_name <> '')
);

CREATE TABLE roles (
    role_id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    CHECK (code <> ''),
    CHECK (name <> '')
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    assigned_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE permissions (
    permission_id BIGSERIAL PRIMARY KEY,
    code VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    CHECK (code <> ''),
    CHECK (name <> '')
);

CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
    permission_id BIGINT NOT NULL REFERENCES permissions(permission_id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE command_assignments (
    assignment_id BIGSERIAL PRIMARY KEY,
    soldier_id BIGINT NOT NULL,
    object_type VARCHAR(32) NOT NULL,
    object_id BIGINT NOT NULL,
    starts_at DATE NOT NULL DEFAULT CURRENT_DATE,
    ends_at DATE NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK (object_type IN (
        'DISTRICT',
        'FORMATION',
        'ARMY',
        'CORPS',
        'DIVISION',
        'BRIGADE',
        'MILITARY_UNIT',
        'BATTALION',
        'COMPANY',
        'PLATOON',
        'SQUAD',
        'SELF'
    )),
    CHECK (ends_at IS NULL OR ends_at >= starts_at)
);

CREATE INDEX idx_command_assignments_soldier
    ON command_assignments (soldier_id);

CREATE TABLE refresh_tokens (
    refresh_token_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    issued_at TIMESTAMP NOT NULL DEFAULT now(),
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    replaced_by_hash VARCHAR(255) NULL,
    user_agent TEXT NULL,
    ip_address VARCHAR(64) NULL
);

CREATE INDEX idx_refresh_tokens_user
    ON refresh_tokens (user_id);

