package com.livingframe.system.controller;

import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.R;
import com.livingframe.common.constant.Constants;
import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.model.LoginUser;
import com.livingframe.system.dto.AssignPermissionsRequest;
import com.livingframe.system.dto.RoleCreateRequest;
import com.livingframe.system.dto.RoleQuery;
import com.livingframe.system.dto.RoleUpdateRequest;
import com.livingframe.system.service.SysRoleService;
import com.livingframe.system.vo.RoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<PageResult<RoleVO>> page(RoleQuery query) {
        return R.ok(roleService.page(query));
    }

    /**
     * 用户管理里分配普通角色时使用；非超级管理员不能看到 SUPER_ADMIN 选项。
     */
    @GetMapping("/options")
    @PreAuthorize("@ss.hasAnyPermi('system:user:create','system:user:assign-role')")
    public R<List<RoleVO>> options() {
        List<RoleVO> roles = roleService.listAll();
        LoginUser loginUser = LoginUserContext.get();
        if (loginUser == null || !loginUser.isSuperAdmin()) {
            roles = roles.stream()
                    .filter(role -> !Constants.ROLE_SUPER_ADMIN.equals(role.code()))
                    .toList();
        }
        return R.ok(roles);
    }

    @GetMapping("/all")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<List<RoleVO>> all() {
        return R.ok(roleService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<RoleVO> get(@PathVariable Long id) {
        return R.ok(roleService.get(id));
    }

    @PostMapping
    @OperationLog("新增角色")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<Long> create(@Valid @RequestBody RoleCreateRequest request) {
        return R.ok(roleService.create(request));
    }

    @PutMapping("/{id}")
    @OperationLog("编辑角色")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        roleService.update(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @OperationLog("删除角色")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/permissions")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<List<Long>> permissions(@PathVariable Long id) {
        return R.ok(roleService.getPermissionIds(id));
    }

    @PutMapping("/{id}/permissions")
    @OperationLog("分配角色权限")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<Void> assignPermissions(@PathVariable Long id,
                                     @Valid @RequestBody AssignPermissionsRequest request) {
        roleService.assignPermissions(id, request);
        return R.ok();
    }
}
