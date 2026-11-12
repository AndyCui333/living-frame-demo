package com.livingframe.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.livingframe.business.entity.CsAgentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CsAgentProfileMapper extends BaseMapper<CsAgentProfile> {

    @Select("""
            SELECT p.*
            FROM cs_agent_profile p
            JOIN sys_user u ON u.id = p.user_id AND u.deleted = 0 AND u.status = 1
            WHERE p.deleted = 0
            ORDER BY p.service_type, p.user_id
            """)
    List<CsAgentProfile> listActiveAgents();
}
