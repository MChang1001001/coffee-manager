CREATE DATABASE IF NOT EXISTS coffee_manager
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE coffee_manager;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS coffee_beans (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '咖啡豆ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(128) NOT NULL COMMENT '咖啡豆名称',
    origin VARCHAR(128) DEFAULT NULL COMMENT '产地',
    region VARCHAR(128) DEFAULT NULL COMMENT '具体产区',
    farm VARCHAR(128) DEFAULT NULL COMMENT '庄园/农场',
    variety VARCHAR(128) DEFAULT NULL COMMENT '品种',
    process_method VARCHAR(64) DEFAULT NULL COMMENT '处理法',
    roast_level VARCHAR(64) DEFAULT NULL COMMENT '烘焙度',
    roaster VARCHAR(128) DEFAULT NULL COMMENT '烘焙商',
    roast_date DATE DEFAULT NULL COMMENT '烘焙日期',
    purchase_date DATE DEFAULT NULL COMMENT '购买日期',
    open_date DATE DEFAULT NULL COMMENT '开封日期',
    finish_date DATE DEFAULT NULL COMMENT '喝完日期',
    net_weight_grams DECIMAL(8,2) DEFAULT NULL COMMENT '净含量，单位g',
    price DECIMAL(10,2) DEFAULT NULL COMMENT '购买价格',
    currency VARCHAR(16) DEFAULT 'CNY' COMMENT '币种',
    status VARCHAR(64) NOT NULL DEFAULT 'UNOPENED' COMMENT '状态',
    cover_image_url VARCHAR(500) DEFAULT NULL COMMENT '包装封面图片访问URL',
    cover_image_object_key VARCHAR(500) DEFAULT NULL COMMENT '包装封面文件存储Key',
    overall_rating DECIMAL(3,1) DEFAULT NULL COMMENT '综合评分缓存，1-5分',
    review_count INT NOT NULL DEFAULT 0 COMMENT '评价数量缓存',
    brew_count INT NOT NULL DEFAULT 0 COMMENT '冲煮记录数量缓存',
    notes TEXT DEFAULT NULL COMMENT '备注',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    KEY idx_coffee_beans_user_id (user_id),
    KEY idx_coffee_beans_status (status),
    KEY idx_coffee_beans_roast_date (roast_date),
    KEY idx_coffee_beans_purchase_date (purchase_date),
    KEY idx_coffee_beans_overall_rating (overall_rating),
    KEY idx_coffee_beans_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咖啡豆表';

CREATE TABLE IF NOT EXISTS coffee_reviews (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '评价ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coffee_bean_id BIGINT NOT NULL COMMENT '咖啡豆ID',
    overall_rating DECIMAL(3,1) NOT NULL COMMENT '综合评分，1-5分',
    aroma_rating DECIMAL(3,1) DEFAULT NULL COMMENT '香气评分',
    acidity_rating DECIMAL(3,1) DEFAULT NULL COMMENT '酸度评分',
    sweetness_rating DECIMAL(3,1) DEFAULT NULL COMMENT '甜感评分',
    bitterness_rating DECIMAL(3,1) DEFAULT NULL COMMENT '苦感评分',
    body_rating DECIMAL(3,1) DEFAULT NULL COMMENT '醇厚度评分',
    aftertaste_rating DECIMAL(3,1) DEFAULT NULL COMMENT '余韵评分',
    content TEXT DEFAULT NULL COMMENT '文字评价',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    KEY idx_reviews_user_id (user_id),
    KEY idx_reviews_coffee_bean_id (coffee_bean_id),
    KEY idx_reviews_overall_rating (overall_rating),
    KEY idx_reviews_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咖啡豆评价表';

CREATE TABLE IF NOT EXISTS brew_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '冲煮记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coffee_bean_id BIGINT NOT NULL COMMENT '咖啡豆ID',
    brew_method VARCHAR(64) NOT NULL COMMENT '冲煮方式',
    bean_amount_grams DECIMAL(6,2) DEFAULT NULL COMMENT '粉量，单位g',
    water_amount_ml DECIMAL(7,2) DEFAULT NULL COMMENT '水量，单位ml',
    ratio VARCHAR(32) DEFAULT NULL COMMENT '粉水比，例如 1:15',
    water_temperature DECIMAL(5,2) DEFAULT NULL COMMENT '水温，单位℃',
    grind_size VARCHAR(128) DEFAULT NULL COMMENT '研磨度',
    brew_time_seconds INT DEFAULT NULL COMMENT '冲煮总时长，单位秒',
    result_summary VARCHAR(255) DEFAULT NULL COMMENT '结果摘要',
    result_notes TEXT DEFAULT NULL COMMENT '结果反馈',
    is_recommended TINYINT NOT NULL DEFAULT 0 COMMENT '是否推荐：0否，1是',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    KEY idx_brew_records_user_id (user_id),
    KEY idx_brew_records_coffee_bean_id (coffee_bean_id),
    KEY idx_brew_records_brew_method (brew_method),
    KEY idx_brew_records_is_recommended (is_recommended),
    KEY idx_brew_records_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='冲煮记录表';

CREATE TABLE IF NOT EXISTS flavor_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '风味标签ID',
    name VARCHAR(64) NOT NULL COMMENT '标签名称',
    category VARCHAR(64) DEFAULT NULL COMMENT '标签分类',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    UNIQUE KEY uk_flavor_tags_name (name),
    KEY idx_flavor_tags_category (category),
    KEY idx_flavor_tags_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风味标签表';

CREATE TABLE IF NOT EXISTS coffee_bean_flavor_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    coffee_bean_id BIGINT NOT NULL COMMENT '咖啡豆ID',
    flavor_tag_id BIGINT NOT NULL COMMENT '风味标签ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_bean_tag (coffee_bean_id, flavor_tag_id),
    KEY idx_bean_tags_coffee_bean_id (coffee_bean_id),
    KEY idx_bean_tags_flavor_tag_id (flavor_tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咖啡豆风味标签关联表';

CREATE TABLE IF NOT EXISTS review_flavor_tags (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    review_id BIGINT NOT NULL COMMENT '评价ID',
    flavor_tag_id BIGINT NOT NULL COMMENT '风味标签ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_review_tag (review_id, flavor_tag_id),
    KEY idx_review_tags_review_id (review_id),
    KEY idx_review_tags_flavor_tag_id (flavor_tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价风味标签关联表';

INSERT INTO users (username, password_hash, nickname)
VALUES ('admin', '$2a$10$ff0bB9I33fXZzLk/YhzHn.pOEkGIm5tG7TfmQz3SCYa77q9o3z1J6', 'mian')
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO flavor_tags (name, category) VALUES
('柑橘', '水果'),
('柠檬', '水果'),
('橙子', '水果'),
('莓果', '水果'),
('草莓', '水果'),
('蓝莓', '水果'),
('热带水果', '水果'),
('苹果', '水果'),
('葡萄', '水果'),
('花香', '花香'),
('茉莉', '花香'),
('蜂蜜', '甜感'),
('焦糖', '甜感'),
('巧克力', '甜感'),
('坚果', '坚果'),
('杏仁', '坚果'),
('榛果', '坚果'),
('酒香', '发酵'),
('茶感', '茶感'),
('香料', '香料'),
('奶油', '口感'),
('可可', '甜感'),
('红糖', '甜感')
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    updated_at = CURRENT_TIMESTAMP;

