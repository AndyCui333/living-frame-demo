package com.livingframe.system.controller;

import com.livingframe.common.annotation.OperationLog;
import com.livingframe.common.api.R;
import com.livingframe.system.dto.PermissionCreateRequest;
import com.livingframe.system.dto.PermissionUpdateRequest;
import com.livingframe.system.service.SysPermissionService;
import com.livingframe.system.vo.PermissionVO;
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
@RequestMapping("/api/system/permissions")
@RequiredArgsConstructor
public class SysPermissionController {

    private final SysPermissionService permissionService;

    @GetMapping("/tree")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<List<PermissionVO>> tree() {
        return R.ok(permissionService.tree());
    }

    @PostMapping
    @OperationLog("新增权限")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<Long> create(@Valid @RequestBody PermissionCreateRequest request) {
        return R.ok(permissionService.create(request));
    }

    @PutMapping("/{id}")
    @OperationLog("编辑权限")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody PermissionUpdateRequest request) {
        permissionService.update(id, request);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @OperationLog("删除权限")
    @PreAuthorize("@ss.hasRole('SUPER_ADMIN')")
    public R<Void> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return R.ok();
    }
}
