package com.livingframe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.livingframe.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select("""
            SELECT DISTINCT p.* FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
            INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId} AND p.deleted = 0 AND p.status = 1
            ORDER BY p.sort ASC, p.id ASC
            """)
    List<SysPermission> selectByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT p.code FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
            INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId} AND p.deleted = 0 AND p.status = 1
            """)
    List<String> selectCodesByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT p.* FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
            INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = #{userId} AND p.deleted = 0 AND p.status = 1 AND p.type = 'MENU'
            ORDER BY p.sort ASC, p.id ASC
            """)
    List<SysPermission> selectMenusByUserId(@Param("userId") Long userId);

    @Select("SELECT code FROM sys_permission WHERE deleted = 0 AND status = 1")
    List<String> selectAllCodes();
}
