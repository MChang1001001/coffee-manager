-- Manual schema update for v4 AI coffee summary fields.
-- Run this against the coffee_manager database. It does not introduce migration tooling.

USE coffee_manager;

ALTER TABLE coffee_beans
  ADD COLUMN summary_title VARCHAR(128) DEFAULT NULL COMMENT '一句话评测总结' AFTER brew_count,
  ADD COLUMN flavor_summary TEXT DEFAULT NULL COMMENT '风味总结' AFTER summary_title,
  ADD COLUMN brew_suggestion TEXT DEFAULT NULL COMMENT '冲煮建议' AFTER flavor_summary,
  ADD COLUMN repurchase_intention VARCHAR(32) DEFAULT NULL COMMENT '回购意向' AFTER brew_suggestion,
  ADD COLUMN summary_text TEXT DEFAULT NULL COMMENT '评测总结正文' AFTER repurchase_intention,
  ADD COLUMN summary_source VARCHAR(16) DEFAULT NULL COMMENT '总结来源：MANUAL/AI' AFTER summary_text,
  ADD COLUMN summary_generated_at DATETIME DEFAULT NULL COMMENT 'AI总结生成时间' AFTER summary_source;
