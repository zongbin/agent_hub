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
    /**
     * 使用原生 SQL：数据库中 summary 列为 bytea 类型（schema.sql 声明为 TEXT），
     * 需用 convert_from(d.summary, 'UTF8') 显式将 bytea 转为 text，
     * 否则 LOWER() 会报 "function lower(bytea) does not exist"。
     * title 与 file_type 为 character varying，直接使用 LOWER() 即可。
     */
    @Query(value = "SELECT d.* FROM documents d WHERE " +
           "d.deleted = 0 AND " +
           "(:keyword IS NULL OR LOWER(d.title) LIKE '%' || LOWER(:keyword) || '%' " +
           " OR LOWER(convert_from(COALESCE(d.summary, ''::bytea), 'UTF8')) LIKE '%' || LOWER(:keyword) || '%' " +
           " OR LOWER(d.file_type) LIKE '%' || LOWER(:keyword) || '%') AND " +
           "(:securityLevel IS NULL OR d.security_level = :securityLevel) AND " +
           "(:statusCode IS NULL OR d.status = :statusCode) AND " +
           "(:userRole = 'ADMIN' OR " +
           " d.id IN (SELECT dp.document_id FROM doc_permissions dp WHERE dp.grantee_id = :userId) OR " +
           " d.security_level = 'PUBLIC' OR " +
           " (:userRole IN ('EDITOR', 'VIEWER') AND d.security_level = 'INTERNAL')) " +
           "ORDER BY d.created_at DESC",
           countQuery = "SELECT COUNT(d.*) FROM documents d WHERE " +
           "d.deleted = 0 AND " +
           "(:keyword IS NULL OR LOWER(d.title) LIKE '%' || LOWER(:keyword) || '%' " +
           " OR LOWER(convert_from(COALESCE(d.summary, ''::bytea), 'UTF8')) LIKE '%' || LOWER(:keyword) || '%' " +
           " OR LOWER(d.file_type) LIKE '%' || LOWER(:keyword) || '%') AND " +
           "(:securityLevel IS NULL OR d.security_level = :securityLevel) AND " +
           "(:statusCode IS NULL OR d.status = :statusCode) AND " +
           "(:userRole = 'ADMIN' OR " +
           " d.id IN (SELECT dp.document_id FROM doc_permissions dp WHERE dp.grantee_id = :userId) OR " +
           " d.security_level = 'PUBLIC' OR " +
           " (:userRole IN ('EDITOR', 'VIEWER') AND d.security_level = 'INTERNAL'))",
           nativeQuery = true)
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
