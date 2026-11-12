package com.livingframe.system.controller;

import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.PageResult;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.AssignRolesRequest;
import com.livingframe.system.dto.ResetPasswordRequest;
import com.livingframe.system.dto.StatusRequest;
import com.livingframe.system.dto.UserCreateRequest;
import com.livingframe.system.dto.UserQuery;
import com.livingframe.system.dto.UserUpdateRequest;
import com.livingframe.system.service.SysUserService;
import com.livingframe.system.vo.RoleBriefVO;
import com.livingframe.system.vo.UserVO;
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
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public R<PageResult<UserVO>> page(UserQuery query) {
        return R.ok(userService.page(query));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    public R<UserVO> get(@PathVariable Long id) {
        return R.ok(userService.get(id));
    }

    @PostMapping
    @OperationLog("新增后台用户")
    @PreAuthorize("@ss.hasPermi('system:user:create')")
    public R<Long> create(@Valid @RequestBody UserCreateRequest request) {
        return R.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @OperationLog("编辑后台用户")
    @PreAuthorize("@ss.hasPermi('system:user:update')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        userService.update(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @OperationLog("删除后台用户")
    @PreAuthorize("@ss.hasPermi('system:user:delete')")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/status")
    @OperationLog("修改后台用户状态")
    @PreAuthorize("@ss.hasPermi('system:user:update')")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request) {
        userService.updateStatus(id, request.status());
        return R.ok();
    }

    @PutMapping("/{id}/password")
    @OperationLog("重置后台用户密码")
    @PreAuthorize("@ss.hasPermi('system:user:reset-password')")
    public R<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.password());
        return R.ok();
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("@ss.hasPermi('system:user:assign-role')")
    public R<List<RoleBriefVO>> roles(@PathVariable Long id) {
        return R.ok(userService.getRoles(id));
    }

    @PutMapping("/{id}/roles")
    @OperationLog("分配后台用户角色")
    @PreAuthorize("@ss.hasPermi('system:user:assign-role')")
    public R<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        userService.assignRoles(id, request);
        return R.ok();
    }
}
