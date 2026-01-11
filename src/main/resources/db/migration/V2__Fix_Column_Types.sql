-- Connect to your database
psql -U postgres -d your_database_name

-- OR if using pgAdmin, run these commands:

ALTER TABLE shortened_url
    ALTER COLUMN code TYPE VARCHAR(255) USING code::VARCHAR,
    ALTER COLUMN long_url TYPE VARCHAR(2048) USING long_url::VARCHAR,
    ALTER COLUMN short_url TYPE VARCHAR(255) USING short_url::VARCHAR;

ALTER TABLE user_oauth_providers
    ALTER COLUMN oauth_provider TYPE VARCHAR(50) USING oauth_provider::VARCHAR,
    ALTER COLUMN oauth_id TYPE VARCHAR(255) USING oauth_id::VARCHAR,
    ALTER COLUMN profile_picture TYPE VARCHAR(500) USING profile_picture::VARCHAR;

ALTER TABLE users
    ALTER COLUMN email TYPE VARCHAR(255) USING email::VARCHAR,
    ALTER COLUMN name TYPE VARCHAR(255) USING name::VARCHAR,
    ALTER COLUMN profile_picture TYPE VARCHAR(500) USING profile_picture::VARCHAR;

ALTER TABLE click_analytics
    ALTER COLUMN ip_address TYPE VARCHAR(45) USING ip_address::VARCHAR,
    ALTER COLUMN user_agent TYPE VARCHAR(500) USING user_agent::VARCHAR,
    ALTER COLUMN referer TYPE VARCHAR(500) USING referer::VARCHAR,
    ALTER COLUMN country TYPE VARCHAR(100) USING country::VARCHAR,
    ALTER COLUMN city TYPE VARCHAR(100) USING city::VARCHAR,
    ALTER COLUMN device_type TYPE VARCHAR(50) USING device_type::VARCHAR,
    ALTER COLUMN browser TYPE VARCHAR(100) USING browser::VARCHAR,
    ALTER COLUMN os TYPE VARCHAR(100) USING os::VARCHAR;