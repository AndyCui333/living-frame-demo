package com.livingframe.system.service;

import cn.hutool.core.util.StrUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.livingframe.common.api.ResultCode;
import com.livingframe.common.constant.Constants;
import com.livingframe.common.exception.BusinessException;
import com.livingframe.common.model.LoginUser;
import com.livingframe.common.util.TreeUtils;
import com.livingframe.system.dto.PermissionCreateRequest;
import com.livingframe.system.dto.PermissionUpdateRequest;
import com.livingframe.system.entity.SysPermission;
import com.livingframe.system.entity.SysRolePermission;
import com.livingframe.system.mapper.SysPermissionMapper;
import com.livingframe.system.mapper.SysRolePermissionMapper;
import com.livingframe.system.mapper.SysUserRoleMapper;
import com.livingframe.system.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 权限/菜单服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysPermissionService {

    private final SysPermissionMapper permissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;

    public List<PermissionVO> tree() {
        List<SysPermission> permissions = permissionMapper.selectList(
                Wrappers.<SysPermission>lambdaQuery()
                        .orderByAsc(SysPermission::getSort)
                        .orderByAsc(SysPermission::getId));
        return buildTree(permissions);
    }

    public List<PermissionVO> menuTreeForUser(LoginUser loginUser) {
        List<SysPermission> permissions;
        if (loginUser.isSuperAdmin()) {
            permissions = permissionMapper.selectList(
                    Wrappers.<SysPermission>lambdaQuery()
                            .eq(SysPermission::getType, "MENU")
                            .eq(SysPermission::getStatus, Constants.STATUS_ENABLED)
                            .orderByAsc(SysPermission::getSort)
                            .orderByAsc(SysPermission::getId));
        } else {
            permissions = permissionMapper.selectMenusByUserId(loginUser.getId());
        }
        return buildTree(permissions);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PermissionCreateRequest request) {
        String code = request.code().trim();
        long exists = permissionMapper.selectCount(
                Wrappers.<SysPermission>lambdaQuery().eq(SysPermission::getCode, code));
        if (exists > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "权限编码已存在");
        }
        validateParent(null, request.parentId());
        SysPermission permission = new SysPermission()
                .setCode(code)
                .setName(request.name())
                .setType(request.type())
                .setParentId(request.parentId() == null ? 0L : request.parentId())
                .setPath(request.path())
                .setComponent(request.component())
                .setIcon(request.icon())
                .setSort(request.sort() == null ? 0 : request.sort())
                .setStatus(request.status() == null ? Constants.STATUS_ENABLED : request.status());
        permissionMapper.insert(permission);
        return permission.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, PermissionUpdateRequest request) {
        SysPermission permission = requirePermission(id);
        validateParent(id, request.parentId());
        permission.setName(request.name());
        if (StrUtil.isNotBlank(request.type())) {
            permission.setType(request.type());
        }
        permission.setParentId(request.parentId() == null ? 0L : request.parentId());
        permission.setPath(request.path());
        permission.setComponent(request.component());
        permission.setIcon(request.icon());
        if (request.sort() != null) {
            permission.setSort(request.sort());
        }
        if (request.status() != null) {
            permission.setStatus(request.status());
        }
        permissionMapper.updateById(permission);
        if (request.status() != null && request.status() == Constants.STATUS_DISABLED) {
            kickoutUsersOfPermission(id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requirePermission(id);
        long children = permissionMapper.selectCount(
                Wrappers.<SysPermission>lambdaQuery().eq(SysPermission::getParentId, id));
        if (children > 0) {
            throw new BusinessException("请先删除子权限");
        }
        long bindings = rolePermissionMapper.selectCount(
                Wrappers.<SysRolePermission>lambdaQuery().eq(SysRolePermission::getPermissionId, id));
        if (bindings > 0) {
            throw new BusinessException("该权限仍绑定角色，请先解除绑定");
        }
        permissionMapper.deleteById(id);
    }

    private List<PermissionVO> buildTree(List<SysPermission> permissions) {
        List<PermissionVO> nodes = permissions.stream().map(this::toVO).toList();
        return TreeUtils.build(nodes,
                PermissionVO::getId,
                PermissionVO::getParentId,
                PermissionVO::setChildren,
                0L);
    }

    private void validateParent(Long id, Long parentId) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (id != null && id.equals(parentId)) {
            throw new BusinessException("父节点不能是自己");
        }
        SysPermission parent = permissionMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException("父节点不存在");
        }
        // 防止把节点挂到自己的后代下形成环
        Long cursor = parent.getParentId();
        int depth = 0;
        while (cursor != null && cursor != 0L && depth++ < 100) {
            if (id != null && id.equals(cursor)) {
                throw new BusinessException("父节点不能是自己的后代");
            }
            SysPermission node = permissionMapper.selectById(cursor);
            if (node == null) {
                break;
            }
            cursor = node.getParentId();
        }
    }

    private SysPermission requirePermission(Long id) {
        SysPermission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "权限不存在");
        }
        return permission;
    }

    private void kickoutUsersOfPermission(Long permissionId) {
        for (Long roleId : rolePermissionMapper.selectRoleIdsByPermissionId(permissionId)) {
            for (Long userId : userRoleMapper.selectUserIdsByRoleId(roleId)) {
                try {
                    StpUtil.kickout(Constants.ADMIN_LOGIN_ID_PREFIX + userId);
                } catch (Exception ex) {
                    log.debug("踢出用户 {} 失败: {}", userId, ex.getMessage());
                }
            }
        }
    }

    private PermissionVO toVO(SysPermission permission) {
        PermissionVO vo = new PermissionVO();
        vo.setId(permission.getId());
        vo.setCode(permission.getCode());
        vo.setName(permission.getName());
        vo.setType(permission.getType());
        vo.setParentId(permission.getParentId());
        vo.setPath(permission.getPath());
        vo.setComponent(permission.getComponent());
        vo.setIcon(permission.getIcon());
        vo.setSort(permission.getSort());
        vo.setStatus(permission.getStatus());
        return vo;
    }
}
