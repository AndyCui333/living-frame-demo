-- 角色
INSERT IGNORE INTO `sys_role` (`id`, `code`, `name`, `description`, `status`, `sort`, `deleted`)
VALUES (1, 'SUPER_ADMIN', '超级管理员', '拥有系统全部权限', 1, 1, 0),
       (2, 'SYSTEM_ADMIN', '系统管理员', '负责后台用户、日志与业务管理', 1, 2, 0),
       (3, 'BUSINESS_ADMIN', '业务管理员', '负责业务用户管理', 1, 3, 0),
       (4, 'VIEWER', '只读用户', '仅可查看列表与菜单', 1, 4, 0),
       (5, 'SMOKE_TEST', '冒烟测试角色', '仅用于冒烟测试的稳定角色，无业务权限', 1, 99, 0),
       (6, 'CS_CHAT', '客服聊天', '在线客服坐席，默认仅 AI 与客服工单相关菜单', 1, 5, 0),
       (7, 'CS_TECH', '客服技术', '技术客服坐席，默认仅 AI 与客服工单相关菜单', 1, 6, 0);

-- 菜单
INSERT IGNORE INTO `sys_permission` (`id`, `code`, `name`, `type`, `parent_id`, `path`, `sort`, `status`, `deleted`)
VALUES (100, 'system', '系统管理', 'MENU', 0, '/system', 1, 1, 0),
       (101, 'system:user:list', '用户管理', 'MENU', 100, '/system/user', 1, 1, 0),
       (102, 'system:role:list', '角色管理', 'MENU', 100, '/system/role', 2, 1, 0),
       (103, 'system:permission:list', '权限管理', 'MENU', 100, '/system/permission', 3, 1, 0),
       (104, 'system:login-log:list', '登录日志', 'MENU', 100, '/system/login-log', 4, 1, 0),
       (105, 'system:operation-log:list', '操作日志', 'MENU', 100, '/system/operation-log', 5, 1, 0),
       (106, 'system:blacklist:list', '系统黑名单', 'MENU', 100, '/system/blacklist', 6, 1, 0),
       (107, 'system:file:list', '文件管理', 'MENU', 100, '/system/file', 7, 1, 0),
       (110, 'business', '业务管理', 'MENU', 0, '/business', 2, 1, 0),
       (111, 'business:user:list', '业务用户', 'MENU', 110, '/business/user', 1, 1, 0),
       (120, 'ai', 'AI 能力', 'MENU', 0, '/ai', 3, 1, 0),
       (121, 'ai:chat', 'AI 对话', 'MENU', 120, '/ai/chat', 1, 1, 0),
       (122, 'ai:agent', 'AI Agent', 'MENU', 120, '/ai/agent', 2, 1, 0),
       (123, 'ai:config', 'AI 设置', 'MENU', 120, '/ai/settings', 3, 1, 0),
       (130, 'customer-service', '客服中心', 'MENU', 0, '/customer-service', 4, 1, 0),
       (131, 'cs:chat:list', '客服聊天', 'MENU', 130, '/customer-service/chat', 1, 1, 0),
       (132, 'cs:ticket:list', '工单流程', 'MENU', 130, '/customer-service/ticket', 2, 1, 0);

-- 兼容旧库：将原黑名单菜单名更新为系统黑名单
UPDATE `sys_permission` SET `name` = '系统黑名单' WHERE `id` = 106 AND `code` = 'system:blacklist:list';

-- 按钮权限
INSERT IGNORE INTO `sys_permission` (`id`, `code`, `name`, `type`, `parent_id`, `path`, `sort`, `status`, `deleted`)
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

-- 系统管理员：用户/日志/黑名单管理；不能访问角色、权限管理菜单
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
VALUES (2, 100), (2, 101), (2, 104), (2, 105), (2, 106), (2, 107),
       (2, 200), (2, 201), (2, 202), (2, 203), (2, 204),
       (2, 240), (2, 241), (2, 242), (2, 250), (2, 251), (2, 252);

-- 兼容旧库：除超级管理员外，收回角色/权限管理菜单及编辑按钮
DELETE rp
FROM `sys_role_permission` rp
         JOIN `sys_role` r ON r.id = rp.role_id
WHERE r.code <> 'SUPER_ADMIN'
  AND rp.permission_id IN (102, 103, 210, 211, 212, 213, 220, 221, 222);

-- 业务管理员：业务菜单 + 业务按钮 + AI 对话
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
VALUES (3, 110), (3, 111), (3, 120), (3, 121),
       (3, 230), (3, 231), (3, 232);

-- 只读用户：只读菜单
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
VALUES (4, 100), (4, 101), (4, 110), (4, 111);

-- 客服坐席：AI 对话 + 客服聊天 + 工单流程，只有这些默认菜单
INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
VALUES (6, 120), (6, 121), (6, 130), (6, 131), (6, 132),
       (6, 260), (6, 261), (6, 262), (6, 263),
       (7, 120), (7, 121), (7, 130), (7, 131), (7, 132),
       (7, 260), (7, 261), (7, 262), (7, 263);

-- 系统黑名单测试账号：账号由 AccountInitializer 创建，这里预置永久黑名单记录
INSERT IGNORE INTO `sys_blacklist` (`target_type`, `target_value`, `reason`, `status`, `created_by`)
VALUES ('USERNAME', 'blacklist_test',
        '系统黑名单功能测试账号：用于验证永久黑名单登录拦截；该账号被永久禁止登录，请勿删除或禁用此记录',
        1, 'SYSTEM');
