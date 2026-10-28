package com.leon.agenthub.knowledge.repository;

import com.leon.agenthub.knowledge.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 文档数据访问接口 — 查询和管理 documents 表。
 *
 * <p>{@code @Where(clause = "deleted = 0")} 在实体层自动过滤已删除记录。
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 分页查询文档列表（带权限过滤）。
     *
     * <p>权限规则：GUEST 只看 PUBLIC，VIEWER 看 PUBLIC+INTERNAL，EDITOR/ADMIN 看所有。
     *
     * @param securityLevel 密级筛选（可选）
     * @param status       状态筛选（可选）
     * @param userRole     当前用户角色
     * @param pageable     分页参数
     * @return 分页文档列表
     */
    @Query("SELECT d FROM Document d WHERE " +
           "(:securityLevel IS NULL OR d.securityLevel = :securityLevel) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:userRole = 'ADMIN' OR :userRole = 'EDITOR' OR " +
           " (:userRole = 'VIEWER' AND d.securityLevel IN ('PUBLIC', 'INTERNAL')) OR " +
           " (:userRole = 'GUEST' AND d.securityLevel = 'PUBLIC')) " +
           "ORDER BY d.createdAt DESC")
    Page<Document> findPageWithPermission(@Param("securityLevel") String securityLevel,
                                           @Param("status") Integer status,
                                           @Param("userRole") String userRole,
                                           Pageable pageable);

    /**
     * 统计文档总数（带权限过滤）。
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE " +
           "(:securityLevel IS NULL OR d.securityLevel = :securityLevel) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:userRole = 'ADMIN' OR :userRole = 'EDITOR' OR " +
           " (:userRole = 'VIEWER' AND d.securityLevel IN ('PUBLIC', 'INTERNAL')) OR " +
           " (:userRole = 'GUEST' AND d.securityLevel = 'PUBLIC'))")
    long countWithPermission(@Param("securityLevel") String securityLevel,
                             @Param("status") Integer status,
                             @Param("userRole") String userRole);
}
