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
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_user_username` (`username`),
    KEY `idx_biz_user_status` (`status`)
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
    KEY `idx_operation_log_time` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='操作日志';

CREATE TABLE IF NOT EXISTS `sys_blacklist`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `target_type`  VARCHAR(16)  NOT NULL COMMENT 'IP/USERNAME',
    `target_value` VARCHAR(128) NOT NULL COMMENT 'IP 或用户名',
    `reason`       VARCHAR(500) DEFAULT NULL COMMENT '加入黑名单的完整原因',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '1生效 0禁用',
    `created_by`   VARCHAR(64)  DEFAULT NULL COMMENT '创建人',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_blacklist_target` (`target_type`, `target_value`),
    KEY `idx_sys_blacklist_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='系统黑名单';
