CREATE TABLE products (
                          id UUID PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          price NUMERIC(19, 2) NOT NULL,
                          currency VARCHAR(3) NOT NULL,
                          stock_quantity INTEGER NOT NULL,
                          active BOOLEAN NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                          CONSTRAINT chk_products_price_non_negative
                              CHECK (price >= 0),

                          CONSTRAINT chk_products_stock_non_negative
                              CHECK (stock_quantity >= 0),

                          CONSTRAINT chk_products_currency_length
                              CHECK (char_length(currency) = 3)
);