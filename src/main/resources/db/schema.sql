CREATE DATABASE IF NOT EXISTS nexusdoc
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE nexusdoc;

CREATE TABLE IF NOT EXISTS document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档ID',
    user_id BIGINT NOT NULL DEFAULT 0 COMMENT '匿名归属ID',
    device_id VARCHAR(128) NOT NULL DEFAULT 'legacy-device' COMMENT '匿名设备ID',
    title VARCHAR(100) NOT NULL COMMENT '文档标题',
    doc_type VARCHAR(50) NOT NULL COMMENT '文档类型',
    tag VARCHAR(50) DEFAULT NULL COMMENT '文档标签',
    content TEXT NOT NULL COMMENT '文档原文',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_document_user_id (user_id),
    INDEX idx_document_device_id (device_id)
) COMMENT='文档表';

CREATE TABLE IF NOT EXISTS document_package (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '文档工作包ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    result_text LONGTEXT NOT NULL COMMENT 'AI生成结果',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_package_document_id (document_id)
) COMMENT='文档工作包表';

CREATE TABLE IF NOT EXISTS chat_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '问答记录ID',
    user_id BIGINT NOT NULL DEFAULT 0 COMMENT '匿名归属ID',
    device_id VARCHAR(128) NOT NULL DEFAULT 'legacy-device' COMMENT '匿名设备ID',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    user_question TEXT NOT NULL COMMENT '用户问题',
    ai_answer LONGTEXT NOT NULL COMMENT 'AI回答',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_chat_document_id (document_id),
    INDEX idx_chat_user_id (user_id),
    INDEX idx_chat_device_id (device_id)
) COMMENT='文档追问记录表';
