-- MySQL migration for canonical words + folder memberships.
-- Goal:
-- 1) Keep one word record per user/word/translated/languageCode combination.
-- 2) Preserve "All" + folder memberships via join table.

START TRANSACTION;

-- 1) Add owner user column to words (keeps ownership independent from folders).
ALTER TABLE words
    ADD COLUMN IF NOT EXISTS user_id BIGINT NULL;

UPDATE words w
    JOIN wordlists wl ON wl.id = w.wordlist_id
SET w.user_id = wl.user_id
WHERE w.user_id IS NULL;

ALTER TABLE words
    MODIFY COLUMN user_id BIGINT NOT NULL;

-- 2) Create join table for many-to-many memberships.
CREATE TABLE IF NOT EXISTS wordlist_words (
    word_id BIGINT NOT NULL,
    wordlist_id BIGINT NOT NULL,
    PRIMARY KEY (word_id, wordlist_id),
    CONSTRAINT fk_wordlist_words_word FOREIGN KEY (word_id) REFERENCES words(id) ON DELETE CASCADE,
    CONSTRAINT fk_wordlist_words_wordlist FOREIGN KEY (wordlist_id) REFERENCES wordlists(id) ON DELETE CASCADE
);

-- 3) Backfill membership relation from legacy words.wordlist_id.
INSERT IGNORE INTO wordlist_words (word_id, wordlist_id)
SELECT w.id, w.wordlist_id
FROM words w
WHERE w.wordlist_id IS NOT NULL;

-- 4) Ensure every user word is in that user's "All" folder.
INSERT IGNORE INTO wordlist_words (word_id, wordlist_id)
SELECT w.id, wl.id
FROM words w
JOIN wordlists wl ON wl.user_id = w.user_id AND LOWER(wl.name) = 'all';

COMMIT;
