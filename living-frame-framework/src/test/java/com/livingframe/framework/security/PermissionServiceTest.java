package com.livingframe.framework.security;

import com.livingframe.common.context.LoginUserContext;
import com.livingframe.common.enums.UserType;
import com.livingframe.common.model.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionServiceTest {

    private final PermissionService permissionService = new PermissionService();

    @AfterEach
    void tearDown() {
        LoginUserContext.clear();
    }

    @Test
    void superAdminShouldPassEverything() {
        LoginUserContext.set(LoginUser.builder()
                .id(1L)
                .username("admin")
                .userType(UserType.ADMIN)
                .roles(Set.of("SUPER_ADMIN"))
                .permissions(Set.of())
                .superAdmin(true)
                .build());

        assertThat(permissionService.hasPermi("system:user:delete")).isTrue();
        assertThat(permissionService.hasRole("ANY_ROLE")).isTrue();
    }

    @Test
    void normalUserShouldBeCheckedByPermissionAndRole() {
        LoginUserContext.set(LoginUser.builder()
                .id(2L)
                .username("viewer")
                .userType(UserType.ADMIN)
                .roles(Set.of("VIEWER"))
                .permissions(Set.of("system:user:list"))
                .superAdmin(false)
                .build());

        assertThat(permissionService.hasPermi("system:user:list")).isTrue();
        assertThat(permissionService.hasPermi("system:user:delete")).isFalse();
        assertThat(permissionService.hasRole("VIEWER")).isTrue();
        assertThat(permissionService.hasRole("SUPER_ADMIN")).isFalse();
    }

    @Test
    void anonymousShouldBeDenied() {
        LoginUserContext.clear();
        assertThat(permissionService.hasPermi("system:user:list")).isFalse();
        assertThat(permissionService.hasRole("VIEWER")).isFalse();
    }
}
