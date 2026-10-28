package com.leon.agenthub.auth.repository;

import com.leon.agenthub.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问接口 — 查询用户数据。
 *
 * <p>{@code @Where(clause = "deleted = 0")} 在实体层自动过滤已删除记录。
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询用户（排除已逻辑删除的）。
     *
     * @param username 用户名
     * @return 用户实体，不存在返回 empty
     */
    Optional<User> findByUsername(String username);
}
