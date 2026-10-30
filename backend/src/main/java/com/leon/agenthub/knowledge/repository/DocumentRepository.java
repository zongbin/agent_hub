package com.leon.agenthub.knowledge.repository;

import com.leon.agenthub.knowledge.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
     * 分页查询文档列表（带权限过滤与关键字搜索）。
     *
     * <p>权限规则：ADMIN 看所有；EDITOR/VIEWER 看 PUBLIC+INTERNAL；GUEST 只看 PUBLIC；
     * 被单独授权（{@code doc_permissions}）的文档对授权用户可见，不受密级限制。
     *
     * <p>关键字搜索范围：标题、摘要、文件类型（大小写不敏感）。
     *
     * @param keyword       关键字（可选，空串按不过滤处理）
     * @param securityLevel 密级筛选（可选）
     * @param statusCode    状态筛选（可选，取值见 {@link com.leon.agenthub.knowledge.enums.DocumentStatus}）
     * @param userId        当前用户 ID（用于文档级授权判断）
     * @param userRole      当前用户角色
     * @param pageable      分页参数
     * @return 分页文档列表
     */
    @Query("SELECT d FROM Document d WHERE " +
           "(:keyword IS NULL OR LOWER(CAST(d.title AS string)) LIKE '%' || LOWER(:keyword) || '%' " +
           " OR LOWER(CAST(COALESCE(d.summary, '') AS string)) LIKE '%' || LOWER(:keyword) || '%' " +
           " OR LOWER(CAST(COALESCE(d.fileType, '') AS string)) LIKE '%' || LOWER(:keyword) || '%') AND " +
           "(:securityLevel IS NULL OR d.securityLevel = :securityLevel) AND " +
           "(:statusCode IS NULL OR d.statusCode = :statusCode) AND " +
           "(:userRole = 'ADMIN' OR " +
           " d.id IN (SELECT dp.documentId FROM DocPermission dp WHERE dp.granteeId = :userId) OR " +
           " d.securityLevel = 'PUBLIC' OR " +
           " (:userRole IN ('EDITOR', 'VIEWER') AND d.securityLevel = 'INTERNAL')) " +
           "ORDER BY d.createdAt DESC")
    Page<Document> findPageWithPermission(@Param("keyword") String keyword,
                                           @Param("securityLevel") String securityLevel,
                                           @Param("statusCode") Integer statusCode,
                                           @Param("userId") Long userId,
                                           @Param("userRole") String userRole,
                                           Pageable pageable);

    /**
     * 直接更新文档状态与错误信息（不加载实体）。
     *
     * <p>用于索引失败等只需改状态、不需要其他字段的场景。
     *
     * @param id           文档 ID
     * @param statusCode   目标状态码（见 {@link com.leon.agenthub.knowledge.enums.DocumentStatus}）
     * @param errorMessage 错误信息
     * @return 更新行数
     */
    @Modifying
    @Query("UPDATE Document d SET d.statusCode = :statusCode, d.errorMessage = :errorMessage WHERE d.id = :id")
    int updateStatusAndError(@Param("id") Long id,
                              @Param("statusCode") Integer statusCode,
                              @Param("errorMessage") String errorMessage);
}
