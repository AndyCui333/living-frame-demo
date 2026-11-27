CREATE TABLE IF NOT EXISTS `sys_user`
(
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `username`      VARCHAR(64)  NOT NULL COMMENT '登录名',
    `password`      VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    `nickname`      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `email`         VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `phone`         VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    `avatar`        VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    `last_login_at` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`),
    KEY `idx_sys_user_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='后台用户';

INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `email`, `phone`, `avatar`, `status`, `deleted`) VALUES (2119649179186569218, 'admin', '$2a$10$FwCC65V6ZMfEDelbb0WLXukRxnjEubjP23Sgbjg73EAbYm1buC73u', '超级管理员', '', '', '', 1, 0);

CREATE TABLE IF NOT EXISTS `sys_role`
(
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `code`        VARCHAR(64)  NOT NULL COMMENT '角色编码',
    `name`        VARCHAR(64)  NOT NULL COMMENT '角色名称',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_code` (`code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色';

INSERT IGNORE INTO `sys_role` (`id`, `code`, `name`, `description`, `status`, `sort`, `deleted`) VALUES (1, 'SUPER_ADMIN', '超级管理员', '拥有系统全部权限', 1, 1, 0);

CREATE TABLE IF NOT EXISTS `sys_permission`
(
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `code`        VARCHAR(128) NOT NULL COMMENT '权限编码',
    `name`        VARCHAR(64)  NOT NULL COMMENT '权限名称',
    `type`        VARCHAR(16)  NOT NULL COMMENT 'MENU/BUTTON',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父节点',
    `path`        VARCHAR(255) DEFAULT NULL COMMENT '路由地址',
    `component`   VARCHAR(255) DEFAULT NULL COMMENT '组件',
    `icon`        VARCHAR(64)  DEFAULT NULL COMMENT '图标',
    `sort`        INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_permission_code` (`code`),
    KEY `idx_sys_permission_parent` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='菜单/权限';

CREATE TABLE IF NOT EXISTS `sys_user_role`
(
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT   NOT NULL COMMENT '用户 ID',
    `role_id`    BIGINT   NOT NULL COMMENT '角色 ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_role` (`user_id`, `role_id`),
    KEY `idx_sys_user_role_role` (`role_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户角色关联';

CREATE TABLE IF NOT EXISTS `sys_role_permission`
(
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id`       BIGINT   NOT NULL COMMENT '角色 ID',
    `permission_id` BIGINT   NOT NULL COMMENT '权限 ID',
    `created_at`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_role_permission` (`role_id`, `permission_id`),
    KEY `idx_sys_role_permission_permission` (`permission_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色权限关联';

CREATE TABLE IF NOT EXISTS `biz_user`
(
    `id`            BIGINT       NOT NULL COMMENT '主键',
    `username`      VARCHAR(64)  NOT NULL COMMENT '登录名',
    `password`      VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码',
    `nickname`      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    `phone`         VARCHAR(32)  DEFAULT NULL COMMENT '手机号',
    `email`         VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
    `avatar`        VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    `last_login_at` DATETIME     DEFAULT NULL COMMENT '最后登录时间',
    `last_active_at` DATETIME    DEFAULT NULL COMMENT '最后活跃时间',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_user_username` (`username`),
    KEY `idx_biz_user_status` (`status`),
    KEY `idx_biz_user_active` (`last_active_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='业务用户';

CREATE TABLE IF NOT EXISTS `sys_user_ai_config`
(
    `id`                 BIGINT        NOT NULL COMMENT '主键',
    `user_id`            BIGINT        NOT NULL COMMENT '后台用户 ID',
    `provider`           VARCHAR(32)   NOT NULL COMMENT 'DEEPSEEK/QWEN/OPENAI/CUSTOM',
    `provider_name`      VARCHAR(64)   NOT NULL COMMENT '厂商名称',
    `base_url`           VARCHAR(512)  NOT NULL COMMENT 'OpenAI 兼容 Base URL',
    `model`              VARCHAR(128)  NOT NULL COMMENT '模型标识',
    `api_key_encrypted`  VARCHAR(1024) DEFAULT NULL COMMENT 'AES-GCM 加密后的 API Key',
    `reasoning_level`    VARCHAR(16)   NOT NULL DEFAULT 'HIGH' COMMENT 'NONE/LOW/MEDIUM/HIGH/MAX',
    `temperature`        DECIMAL(3, 2) NOT NULL DEFAULT 0.70 COMMENT '温度',
    `max_tokens`         INT           NOT NULL DEFAULT 4096 COMMENT '最大输出 token',
    `is_default`         TINYINT       NOT NULL DEFAULT 0 COMMENT '1当前使用 0普通配置',
    `created_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_ai_config_model` (`user_id`, `provider`, `model`),
    KEY `idx_sys_user_ai_config_default` (`user_id`, `is_default`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户 AI 模型厂商配置（Key 加密存储）';

CREATE TABLE IF NOT EXISTS `ai_conversation`
(
    `id`               BIGINT       NOT NULL COMMENT '主键',
    `user_id`          BIGINT       NOT NULL COMMENT '后台用户 ID',
    `title`            VARCHAR(120) NOT NULL DEFAULT '新对话' COMMENT '会话标题',
    `summary`          TEXT         DEFAULT NULL COMMENT '会话摘要（预留）',
    `message_count`    INT          NOT NULL DEFAULT 0 COMMENT '消息数量',
    `last_message_at`  DATETIME     DEFAULT NULL COMMENT '最后消息时间',
    `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_ai_conversation_user` (`user_id`, `last_message_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI 对话会话';

CREATE TABLE IF NOT EXISTS `ai_message`
(
    `id`               BIGINT      NOT NULL COMMENT '主键',
    `conversation_id`  BIGINT      NOT NULL COMMENT '会话 ID',
    `user_id`          BIGINT      NOT NULL COMMENT '后台用户 ID',
    `role`             VARCHAR(16) NOT NULL COMMENT 'user/assistant',
    `content`          MEDIUMTEXT  NOT NULL COMMENT '消息内容',
    `model`            VARCHAR(128) DEFAULT NULL COMMENT '生成模型',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_ai_message_conversation` (`conversation_id`, `id`),
    KEY `idx_ai_message_user` (`user_id`, `created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='AI 对话消息（长期记忆源）';

CREATE TABLE IF NOT EXISTS `biz_notification`
(
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '门户用户 ID',
    `type`        VARCHAR(32)  NOT NULL DEFAULT 'CS_MESSAGE' COMMENT '通知类型',
    `title`       VARCHAR(128) NOT NULL COMMENT '通知标题',
    `content`     VARCHAR(500) DEFAULT NULL COMMENT '通知摘要',
    `target_type` VARCHAR(32)  DEFAULT NULL COMMENT '跳转目标类型',
    `target_id`   BIGINT       DEFAULT NULL COMMENT '跳转目标 ID',
    `read_status` TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_biz_notification_user` (`user_id`, `read_status`),
    KEY `idx_biz_notification_time` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='门户通知';

CREATE TABLE IF NOT EXISTS `cs_agent_profile`
(
    `id`                BIGINT      NOT NULL COMMENT '主键',
    `user_id`           BIGINT      NOT NULL COMMENT '客服后台用户 ID',
    `service_type`      VARCHAR(16) NOT NULL COMMENT 'CHAT/TECH',
    `online_status`     VARCHAR(16) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE/BUSY/OFFLINE',
    `current_ticket_id` BIGINT      DEFAULT NULL COMMENT '最近处理工单',
    `last_seen_at`      DATETIME    DEFAULT NULL COMMENT '客服最近在线时间',
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cs_agent_user` (`user_id`),
    KEY `idx_cs_agent_dispatch` (`service_type`, `online_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='客服坐席状态';

CREATE TABLE IF NOT EXISTS `cs_ticket`
(
    `id`                BIGINT       NOT NULL COMMENT '主键',
    `ticket_no`         VARCHAR(32)  NOT NULL COMMENT '工单编号',
    `portal_user_id`    BIGINT       NOT NULL COMMENT '门户/业务用户 ID',
    `title`             VARCHAR(200) NOT NULL COMMENT '标题',
    `content`           TEXT         NOT NULL COMMENT '问题描述',
    `ticket_type`       VARCHAR(16)  NOT NULL COMMENT 'CHAT/TECH',
    `status`            VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PENDING/ASSIGNED/PROCESSING/ARCHIVED',
    `assigned_agent_id` BIGINT       DEFAULT NULL COMMENT '接单客服用户 ID',
    `assigned_at`       DATETIME     DEFAULT NULL,
    `agent_read_at`     DATETIME     DEFAULT NULL COMMENT '客服最近阅读时间',
    `last_message_at`   DATETIME     DEFAULT NULL,
    `closed_at`         DATETIME     DEFAULT NULL,
    `rating`            TINYINT      DEFAULT NULL COMMENT '1-5',
    `feedback`          VARCHAR(1000) DEFAULT NULL,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`           TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cs_ticket_no` (`ticket_no`),
    KEY `idx_cs_ticket_portal` (`portal_user_id`, `status`),
    KEY `idx_cs_ticket_agent` (`assigned_agent_id`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='客服工单';

CREATE TABLE IF NOT EXISTS `cs_ticket_message`
(
    `id`          BIGINT      NOT NULL COMMENT '主键',
    `ticket_id`   BIGINT      NOT NULL COMMENT '工单 ID',
    `sender_type` VARCHAR(16) NOT NULL COMMENT 'PORTAL/AGENT/SYSTEM',
    `sender_id`   BIGINT      DEFAULT NULL,
    `content`     TEXT        NOT NULL,
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`     TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_cs_ticket_message` (`ticket_id`, `id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='客服工单消息';

CREATE TABLE IF NOT EXISTS `sys_login_log`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    BIGINT       DEFAULT NULL COMMENT '用户 ID',
    `username`   VARCHAR(64)  NOT NULL COMMENT '登录名',
    `login_type` VARCHAR(16)  NOT NULL COMMENT 'ADMIN/BUSINESS',
    `ip`         VARCHAR(64)  DEFAULT NULL COMMENT '登录 IP',
    `user_agent` VARCHAR(512) DEFAULT NULL COMMENT '浏览器 UA',
    `status`     VARCHAR(16)  NOT NULL COMMENT 'SUCCESS/FAIL',
    `message`    VARCHAR(255) DEFAULT NULL COMMENT '结果说明',
    `login_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_login_log_username` (`username`),
    KEY `idx_login_log_ip` (`ip`),
    KEY `idx_login_log_time` (`login_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='登录日志';

CREATE TABLE IF NOT EXISTS `sys_operation_log`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`       BIGINT       DEFAULT NULL COMMENT '操作人 ID',
    `username`      VARCHAR(64)  DEFAULT NULL COMMENT '操作人',
    `user_type`     VARCHAR(16)  DEFAULT NULL COMMENT 'ADMIN/BUSINESS',
    `operation`     VARCHAR(128) NOT NULL COMMENT '操作描述',
    `method`        VARCHAR(16)  DEFAULT NULL COMMENT 'HTTP 方法',
    `request_uri`   VARCHAR(255) DEFAULT NULL COMMENT '请求地址',
    `ip`            VARCHAR(64)  DEFAULT NULL COMMENT '操作 IP',
    `status`        VARCHAR(16)  NOT NULL COMMENT 'SUCCESS/FAIL',
    `error_message` VARCHAR(500) DEFAULT NULL COMMENT '失败原因',
    `duration_ms`   BIGINT       DEFAULT 0 COMMENT '耗时毫秒',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_operation_log_user` (`username`),
    KEY `idx_operation_log_time` (`created_at`),
    KEY `idx_operation_log_type` (`user_type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='操作日志';

CREATE TABLE IF NOT EXISTS `sys_blacklist`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `target_type`  VARCHAR(16)  NOT NULL COMMENT 'IP/USERNAME',
    `target_value` VARCHAR(128) NOT NULL COMMENT 'IP 或用户名',
    `user_scope`   VARCHAR(16)  NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM/PORTAL',
    `reason`       VARCHAR(500) DEFAULT NULL COMMENT '加入黑名单的完整原因',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1生效 0禁用',
    `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_blacklist_target` (`user_scope`, `target_type`, `target_value`),
    KEY `idx_sys_blacklist_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='系统黑名单';

-- ============================================================
-- 超级管理员初始化数据
-- ============================================================

-- 超级管理员账号绑定 SUPER_ADMIN 角色
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
VALUES (2119649179186569218, 1);

-- 菜单权限
INSERT IGNORE INTO `sys_permission`
(`id`, `code`, `name`, `type`, `parent_id`, `path`, `sort`, `status`, `deleted`)
VALUES (99, 'dashboard', '首页', 'MENU', 0, '/dashboard', 0, 1, 0),
       (100, 'system', '系统管理', 'MENU', 0, '/system', 1, 1, 0),
       (101, 'system:user:list', '用户管理', 'MENU', 100, '/system/user', 1, 1, 0),
       (102, 'system:role:list', '角色管理', 'MENU', 100, '/system/role', 2, 1, 0),
       (103, 'system:permission:list', '权限管理', 'MENU', 100, '/system/permission', 3, 1, 0),
       (104, 'system:login-log:list', '系统登录日志', 'MENU', 100, '/system/login-log', 4, 1, 0),
       (105, 'system:operation-log:list', '系统操作日志', 'MENU', 100, '/system/operation-log', 5, 1, 0),
       (106, 'system:blacklist:list', '系统黑名单', 'MENU', 0, '/system/blacklist', 5, 1, 0),
       (107, 'system:file:list', '文件管理', 'MENU', 100, '/system/file', 7, 1, 0),
       (110, 'business', '业务管理', 'MENU', 0, '/business', 2, 1, 0),
       (111, 'business:user:list', '业务用户', 'MENU', 110, '/business/user', 1, 1, 0),
       (112, 'business:login-log:portal', '门户登录日志', 'MENU', 110, '/business/login-log', 2, 1, 0),
       (113, 'business:operation-log:portal', '门户操作日志', 'MENU', 110, '/business/operation-log', 3, 1, 0),
       (114, 'business:statistics:list', '用户统计', 'MENU', 110, '/business/statistics', 4, 1, 0),
       (120, 'ai', 'AI 能力', 'MENU', 0, '/ai', 3, 1, 0),
       (121, 'ai:chat', 'AI 对话', 'MENU', 120, '/ai/chat', 1, 1, 0),
       (122, 'ai:agent', 'AI Agent', 'MENU', 120, '/ai/agent', 2, 1, 0),
       (123, 'ai:config', 'AI 设置', 'MENU', 120, '/ai/settings', 3, 1, 0),
       (130, 'customer-service', '客服中心', 'MENU', 0, '/customer-service', 4, 1, 0),
       (131, 'cs:chat:list', '客服聊天', 'MENU', 130, '/customer-service/chat', 1, 1, 0),
       (132, 'cs:ticket:list', '工单流程', 'MENU', 130, '/customer-service/ticket', 2, 1, 0),
       (140, 'cs:ranking:list', '客服排行', 'MENU', 0, '/customer-service/ranking', 6, 1, 0);

-- 按钮权限
INSERT IGNORE INTO `sys_permission`
(`id`, `code`, `name`, `type`, `parent_id`, `path`, `sort`, `status`, `deleted`)
VALUES (200, 'system:user:create', '新增用户', 'BUTTON', 101, NULL, 1, 1, 0),
       (201, 'system:user:update', '编辑用户', 'BUTTON', 101, NULL, 2, 1, 0),
       (202, 'system:user:delete', '删除用户', 'BUTTON', 101, NULL, 3, 1, 0),
       (203, 'system:user:reset-password', '重置密码', 'BUTTON', 101, NULL, 4, 1, 0),
       (204, 'system:user:assign-role', '分配角色', 'BUTTON', 101, NULL, 5, 1, 0),
       (210, 'system:role:create', '新增角色', 'BUTTON', 102, NULL, 1, 1, 0),
       (211, 'system:role:update', '编辑角色', 'BUTTON', 102, NULL, 2, 1, 0),
       (212, 'system:role:delete', '删除角色', 'BUTTON', 102, NULL, 3, 1, 0),
       (213, 'system:role:assign-permission', '分配权限', 'BUTTON', 102, NULL, 4, 1, 0),
       (220, 'system:permission:create', '新增权限', 'BUTTON', 103, NULL, 1, 1, 0),
       (221, 'system:permission:update', '编辑权限', 'BUTTON', 103, NULL, 2, 1, 0),
       (222, 'system:permission:delete', '删除权限', 'BUTTON', 103, NULL, 3, 1, 0),
       (230, 'business:user:create', '新增业务用户', 'BUTTON', 111, NULL, 1, 1, 0),
       (231, 'business:user:update', '编辑业务用户', 'BUTTON', 111, NULL, 2, 1, 0),
       (232, 'business:user:delete', '删除业务用户', 'BUTTON', 111, NULL, 3, 1, 0),
       (240, 'system:blacklist:create', '新增黑名单', 'BUTTON', 106, NULL, 1, 1, 0),
       (241, 'system:blacklist:update', '编辑黑名单', 'BUTTON', 106, NULL, 2, 1, 0),
       (242, 'system:blacklist:delete', '删除黑名单', 'BUTTON', 106, NULL, 3, 1, 0),
       (250, 'system:file:upload', '上传文件', 'BUTTON', 107, NULL, 1, 1, 0),
       (251, 'system:file:delete', '删除文件', 'BUTTON', 107, NULL, 2, 1, 0),
       (252, 'system:file:bucket', '管理 Bucket', 'BUTTON', 107, NULL, 3, 1, 0),
       (260, 'cs:chat:reply', '回复客服消息', 'BUTTON', 131, NULL, 1, 1, 0),
       (261, 'cs:ticket:dispatch', '派遣工单', 'BUTTON', 132, NULL, 1, 1, 0),
       (262, 'cs:ticket:transfer', '转接工单', 'BUTTON', 132, NULL, 2, 1, 0),
       (263, 'cs:ticket:close', '截单归档', 'BUTTON', 132, NULL, 3, 1, 0);

-- SUPER_ADMIN 代码内直接放行全部权限；这里仍写全角色权限关联，
-- 方便以后去掉代码级 superAdmin 放行时仍保持完整权限。
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id
FROM `sys_permission`
WHERE `deleted` = 0;
