CREATE TABLE IF NOT EXISTS downloaded_files (
    id BIGINT NOT NULL PRIMARY KEY,
    file_url VARCHAR(512) NOT NULL UNIQUE,
    local_path VARCHAR(512) NOT NULL,
    downloaded_at TIMESTAMP NOT NULL
);