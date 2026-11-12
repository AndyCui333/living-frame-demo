package com.livingframe.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.livingframe.ai.entity.SysUserAiConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SysUserAiConfigMapper extends BaseMapper<SysUserAiConfig> {

    /**
     * 唯一键 (user_id, provider, model) 下逻辑删除会阻止重新添加，配置删除使用物理删除。
     */
    @Delete("DELETE FROM sys_user_ai_config WHERE id = #{id}")
    int deletePhysical(@Param("id") Long id);

    @Delete("DELETE FROM sys_user_ai_config WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
