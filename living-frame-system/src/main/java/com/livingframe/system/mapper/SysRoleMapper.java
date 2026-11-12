package com.livingframe.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.livingframe.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT r.* FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.deleted = 0
            ORDER BY r.sort ASC, r.id ASC
            """)
    List<SysRole> selectByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT r.code FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.deleted = 0 AND r.status = 1
            """)
    List<String> selectCodesByUserId(@Param("userId") Long userId);
}
