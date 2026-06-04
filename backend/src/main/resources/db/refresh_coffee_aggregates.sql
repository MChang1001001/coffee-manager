-- Manual one-time repair for coffee_beans aggregate cache fields.
-- Run this against the coffee_manager database after deploying aggregate write-back.
-- It does not create or migrate tables.

USE coffee_manager;

START TRANSACTION;

UPDATE coffee_beans cb
LEFT JOIN (
    SELECT
        user_id,
        coffee_bean_id,
        COUNT(*) AS review_count,
        ROUND(AVG(overall_rating), 1) AS overall_rating
    FROM coffee_reviews
    WHERE deleted = 0
    GROUP BY user_id, coffee_bean_id
) r
    ON r.user_id = cb.user_id
    AND r.coffee_bean_id = cb.id
LEFT JOIN (
    SELECT
        user_id,
        coffee_bean_id,
        COUNT(*) AS brew_count
    FROM brew_records
    WHERE deleted = 0
    GROUP BY user_id, coffee_bean_id
) b
    ON b.user_id = cb.user_id
    AND b.coffee_bean_id = cb.id
SET
    cb.review_count = COALESCE(r.review_count, 0),
    cb.overall_rating = r.overall_rating,
    cb.brew_count = COALESCE(b.brew_count, 0)
WHERE cb.deleted = 0;

COMMIT;
