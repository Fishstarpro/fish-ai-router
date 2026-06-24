-- =============================================================================
-- 数据库更新脚本 V1.2
-- 描述: request_log 表补齐缺失字段（对比 yu_ai_router.sql 原始建表 vs 当前实体类）
-- 数据库: yu_ai_router
-- 日期: 2026-06-23
-- =============================================================================

USE yu_ai_router;

-- 1. 链路追踪ID
ALTER TABLE `request_log`
    ADD COLUMN `traceId` VARCHAR(64) NULL DEFAULT NULL COMMENT '链路追踪ID' AFTER `id`;

-- 2. 实际调用的模型ID
ALTER TABLE `request_log`
    ADD COLUMN `modelId` BIGINT NULL DEFAULT NULL COMMENT '实际调用的模型ID' AFTER `apiKeyId`;

-- 3. 请求的模型标识
ALTER TABLE `request_log`
    ADD COLUMN `requestModel` VARCHAR(128) NULL DEFAULT NULL COMMENT '请求的模型标识' AFTER `modelId`;

-- 4. 请求类型
ALTER TABLE `request_log`
    ADD COLUMN `requestType` VARCHAR(32) NOT NULL DEFAULT 'chat' COMMENT '请求类型：chat/embedding/image' AFTER `modelName`;

-- 5. 调用来源
ALTER TABLE `request_log`
    ADD COLUMN `source` VARCHAR(32) NOT NULL DEFAULT 'web' COMMENT '调用来源：web/api' AFTER `requestType`;

-- 6. 错误码
ALTER TABLE `request_log`
    ADD COLUMN `errorCode` VARCHAR(64) NULL DEFAULT NULL COMMENT '错误码' AFTER `errorMessage`;

-- 7. 路由策略
ALTER TABLE `request_log`
    ADD COLUMN `routingStrategy` VARCHAR(32) NULL DEFAULT NULL COMMENT '使用的路由策略' AFTER `errorCode`;

-- 8. 是否为Fallback请求
ALTER TABLE `request_log`
    ADD COLUMN `isFallback` TINYINT NOT NULL DEFAULT 0 COMMENT '是否为Fallback请求：0=否，1=是' AFTER `routingStrategy`;

-- 9. 客户端IP
ALTER TABLE `request_log`
    ADD COLUMN `clientIp` VARCHAR(64) NULL DEFAULT NULL COMMENT '客户端IP' AFTER `isFallback`;

-- 10. User-Agent
ALTER TABLE `request_log`
    ADD COLUMN `userAgent` VARCHAR(512) NULL DEFAULT NULL COMMENT 'User-Agent' AFTER `clientIp`;

-- =============================================================================
-- 补齐索引
-- =============================================================================
ALTER TABLE `request_log`
    ADD INDEX `idx_traceId` (`traceId` ASC) USING BTREE;

ALTER TABLE `request_log`
    ADD INDEX `idx_modelId` (`modelId` ASC) USING BTREE;

ALTER TABLE `request_log`
    ADD INDEX `idx_source` (`source` ASC) USING BTREE;
