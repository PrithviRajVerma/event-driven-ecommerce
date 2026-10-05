CREATE TABLE inventory (
                           id UUID PRIMARY KEY,
                           product_id UUID NOT NULL UNIQUE,

                           available_quantity INTEGER NOT NULL DEFAULT 0,
                           reserved_quantity INTEGER NOT NULL DEFAULT 0,

                           version BIGINT NOT NULL DEFAULT 0,

                           created_at TIMESTAMPTZ NOT NULL,
                           updated_at TIMESTAMPTZ NOT NULL,

                           CONSTRAINT chk_inventory_available_quantity
                               CHECK (available_quantity >= 0),

                           CONSTRAINT chk_inventory_reserved_quantity
                               CHECK (reserved_quantity >= 0)
);