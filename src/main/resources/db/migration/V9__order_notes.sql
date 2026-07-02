-- V9: Order notes / employee comments

CREATE TABLE order_note (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES translation_order(id) ON DELETE CASCADE,
    author_username VARCHAR(255) NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_order_note_order_created ON order_note(order_id, created_at DESC);
