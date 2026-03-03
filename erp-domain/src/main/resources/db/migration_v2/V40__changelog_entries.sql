CREATE TABLE IF NOT EXISTS changelog_entries (
    id BIGSERIAL PRIMARY KEY,
    version_label VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    published_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255) NOT NULL,
    highlighted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_changelog_entries_published
    ON changelog_entries (published_at DESC, id DESC)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_changelog_entries_highlighted
    ON changelog_entries (highlighted, published_at DESC, id DESC)
    WHERE deleted = FALSE;
