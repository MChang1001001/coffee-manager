# Database Design

## Direction

第一版以 MySQL 关系模型为主，Redis 暂用于登录态、缓存或后续提醒任务辅助。

## Core Tables

- users
- coffee_beans
- coffee_reviews
- brew_records
- flavor_tags
- coffee_bean_flavor_tags

## Optional Tables

- coffee_bean_batches
- attachments
- recipe_templates
- purchase_records

## Design Notes

- 咖啡豆基础信息、评价、冲煮记录分表存储。
- `coffee_beans` 保存 `variety`、烘焙日期、赏味开始日期、赏味结束日期，用于前端展示饮用状态和后端饮用状态筛选。
- `coffee_beans.review_count`、`coffee_beans.overall_rating`、`coffee_beans.brew_count` 是聚合缓存字段，v0.3 已在 review / brew 新增、编辑、删除后回写；历史数据不一致时通过 `backend/src/main/resources/db/refresh_coffee_aggregates.sql` 手动修复。
- `coffee_beans.summary_title`、`flavor_summary`、`brew_suggestion`、`repurchase_intention`、`summary_text`、`summary_source`、`summary_generated_at` 保存用户确认后的评测总结；AI 生成只产出草稿，必须经用户保存才写入。
- 冲煮记录保存完整参数快照。
- 风味标签使用多对多关系。
- v0.4 继续手动 SQL，不引入 migration 工具；执行旧库补字段或历史修复 SQL 前建议先备份本地库。
- 第一版枚举 / 常用选项写在后端静态代码里，后续再字典表化和后台管理。
