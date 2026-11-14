ALTER TABLE orders
    ADD COLUMN delivery_full_name VARCHAR(255) NULL,
    ADD COLUMN delivery_phone_number VARCHAR(10) NULL,
    ADD COLUMN delivery_address_line1 VARCHAR(255) NULL,
    ADD COLUMN delivery_address_line2 VARCHAR(255) NULL,
    ADD COLUMN delivery_city VARCHAR(255) NULL,
    ADD COLUMN delivery_state VARCHAR(255) NULL,
    ADD COLUMN delivery_country VARCHAR(255) NULL,
    ADD COLUMN delivery_postal_code VARCHAR(255) NULL;
