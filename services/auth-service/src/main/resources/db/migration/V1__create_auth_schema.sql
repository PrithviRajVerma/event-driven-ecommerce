
CREATE TABLE users(
    id UUID PRIMARY KEY ,
    email VARCHAR(320) NOT NULL UNIQUE ,
    password_hash VARCHAR(255) ,

    email_verified BOOLEAN NOT NULL DEFAULT false,
    email_verified_at TIMESTAMPTZ,

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    last_login_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL ,
    updated_at TIMESTAMPTZ NOT NULL ,

    CONSTRAINT chk_user_status
                  CHECK ( status IN('ACTIVE','DISABLED') )
);

CREATE TABLE roles(
    id UUID PRIMARY KEY ,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE user_roles(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE

);

CREATE TABLE oauth_accounts(
    id UUID PRIMARY KEY ,
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL ,
    provider_user_id VARCHAR(255) NOT NULL ,
    created_at TIMESTAMPTZ NOT NULL ,

    CONSTRAINT fk_oauth_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE ,

    CONSTRAINT uq_oauth_provider_user
        UNIQUE (provider,provider_user_id),

    CONSTRAINT chk_oauth_provider
        CHECK ( provider IN ('GOOGLE') )
);

CREATE TABLE email_verification_tokens(
    id UUID PRIMARY KEY NOT NULL ,
    user_id UUID NOT NULL ,
    token_hash VARCHAR(255) UNIQUE NOT NULL ,
    expires_at TIMESTAMPTZ NOT NULL ,
    created_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ ,

    CONSTRAINT fk_email_verification_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens(
     id UUID PRIMARY KEY NOT NULL ,
     user_id UUID NOT NULL ,
     token_hash VARCHAR(255) UNIQUE NOT NULL ,
     expires_at TIMESTAMPTZ NOT NULL ,
     created_at TIMESTAMPTZ NOT NULL,
     used_at TIMESTAMPTZ ,

     CONSTRAINT fk_password_reset_user
         FOREIGN KEY (user_id)
             REFERENCES users(id)
             ON DELETE CASCADE
);

CREATE TABLE refresh_sessions(
     id UUID PRIMARY KEY NOT NULL ,
     user_id UUID NOT NULL ,
     token_hash VARCHAR(255) UNIQUE NOT NULL ,

     revoked_at TIMESTAMPTZ,
     revoked_reason VARCHAR(255),
     device_info VARCHAR(500),
     ip_address INET,

     expires_at TIMESTAMPTZ NOT NULL ,
     created_at TIMESTAMPTZ NOT NULL,
     last_used_at TIMESTAMPTZ ,

     CONSTRAINT fk_refresh_session_user
         FOREIGN KEY (user_id)
             REFERENCES users(id)
             ON DELETE CASCADE
);

CREATE INDEX user_roles_roles_id
    ON user_roles(role_id);

CREATE INDEX idx_email_verification_user_id
    ON email_verification_tokens(user_id);

CREATE INDEX idx_password_reset_user_id
    ON password_reset_tokens(user_id);

CREATE INDEX idx_refresh_session_user_id
    ON refresh_sessions(user_id);

CREATE INDEX idx_refresh_session_expires_at
    ON refresh_sessions(expires_at);

CREATE INDEX idx_refresh_session_revoked_at
    ON refresh_sessions(revoked_at);



    INSERT INTO roles (id, name)
    VALUES
        (gen_random_uuid(), 'USER'),
        (gen_random_uuid(), 'ADMIN');