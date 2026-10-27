package com.leon.agenthub.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leon.agenthub.auth.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper — 查询用户数据。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户（排除已逻辑删除的）。
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 null
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    User selectByUsername(@Param("username") String username);
}
