package com.leon.agenthub.knowledge.repository;

import com.leon.agenthub.knowledge.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档数据访问接口 — 查询和管理 documents 表。
 *
 * <p>{@code @Where(clause = "deleted = 0")} 在实体层自动过滤已删除记录；
 * 原生查询不受 {@code @Where} 约束，需显式带上 {@code deleted = 0} 条件。
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 可见性过滤谓词 — 当前用户可见的文档集合。
     *
     * <p>规则与 {@link com.leon.agenthub.permission.service.DocumentAccessChecker} 保持一致：
     * ADMIN 看所有；GUEST 只看 PUBLIC；EDITOR/VIEWER 看 PUBLIC + INTERNAL；
     * 被单独授权（{@code doc_permissions}）的文档对授权用户可见，不受密级限制。
     *
     * <p>该常量供本接口内所有原生查询复用，避免过滤规则在多处漂移。
     */
    String VISIBLE_PREDICATE =
            "(:userRole = 'ADMIN' OR " +
            " d.id IN (SELECT dp.document_id FROM doc_permissions dp WHERE dp.grantee_id = :userId) OR " +
            " d.security_level = 'PUBLIC' OR " +
            " (:userRole IN ('EDITOR', 'VIEWER') AND d.security_level = 'INTERNAL'))";

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
     *
     * <p>使用原生 SQL：线上库中 title / file_type 为 character varying、summary 为 text，
     * 均可直接使用 LOWER()，无需任何类型转换。
     * <p>注意：不要写成 COALESCE(d.summary, ''::bytea) —— summary 是 text，
     * 与 bytea 字面量混合会报 "COALESCE types text and bytea cannot be matched"。
     */
    @Query(value = "SELECT d.* FROM documents d WHERE " +
           "d.deleted = 0 AND " +
           "(:keyword IS NULL OR LOWER(d.title) LIKE '%' || LOWER(:keyword) || '%' " +
           " OR LOWER(COALESCE(d.summary, '')) LIKE '%' || LOWER(:keyword) || '%' " +
           " OR LOWER(d.file_type) LIKE '%' || LOWER(:keyword) || '%') AND " +
           "(:securityLevel IS NULL OR d.security_level = :securityLevel) AND " +
           "(:statusCode IS NULL OR d.status = :statusCode) AND " +
           VISIBLE_PREDICATE +
           " ORDER BY d.created_at DESC",
           countQuery = "SELECT COUNT(d.*) FROM documents d WHERE " +
           "d.deleted = 0 AND " +
           "(:keyword IS NULL OR LOWER(d.title) LIKE '%' || LOWER(:keyword) || '%' " +
           " OR LOWER(COALESCE(d.summary, '')) LIKE '%' || LOWER(:keyword) || '%' " +
           " OR LOWER(d.file_type) LIKE '%' || LOWER(:keyword) || '%') AND " +
           "(:securityLevel IS NULL OR d.security_level = :securityLevel) AND " +
           "(:statusCode IS NULL OR d.status = :statusCode) AND " +
           VISIBLE_PREDICATE,
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

    /**
     * 可见文档按处理状态分组计数 — 工作台文档状态分布。
     *
     * @param userId   当前用户 ID
     * @param userRole 当前用户角色
     * @return 每行为 {@code [statusCode, count]}，statusCode 见 {@link com.leon.agenthub.knowledge.enums.DocumentStatus}
     */
    @Query(value = "SELECT d.status, COUNT(*) FROM documents d " +
           "WHERE d.deleted = 0 AND " + VISIBLE_PREDICATE + " GROUP BY d.status",
           nativeQuery = true)
    List<Object[]> countVisibleByStatus(@Param("userId") Long userId,
                                        @Param("userRole") String userRole);

    /**
     * 可见文档按安全密级分组计数 — 工作台密级分布。
     *
     * @param userId   当前用户 ID
     * @param userRole 当前用户角色
     * @return 每行为 {@code [securityLevel, count]}
     */
    @Query(value = "SELECT d.security_level, COUNT(*) FROM documents d " +
           "WHERE d.deleted = 0 AND " + VISIBLE_PREDICATE + " GROUP BY d.security_level",
           nativeQuery = true)
    List<Object[]> countVisibleBySecurityLevel(@Param("userId") Long userId,
                                               @Param("userRole") String userRole);

    /**
     * 可见文档的分块总数 — 反映已索引的向量规模。
     *
     * @param userId   当前用户 ID
     * @param userRole 当前用户角色
     * @return 分块总数（无数据时为 0）
     */
    @Query(value = "SELECT COALESCE(SUM(d.chunk_count), 0) FROM documents d " +
           "WHERE d.deleted = 0 AND " + VISIBLE_PREDICATE,
           nativeQuery = true)
    Long sumVisibleChunkCount(@Param("userId") Long userId,
                               @Param("userRole") String userRole);

    /**
     * 可见文档的总体积（字节）。
     *
     * @param userId   当前用户 ID
     * @param userRole 当前用户角色
     * @return 总体积（无数据时为 0）
     */
    @Query(value = "SELECT COALESCE(SUM(d.file_size), 0) FROM documents d " +
           "WHERE d.deleted = 0 AND " + VISIBLE_PREDICATE,
           nativeQuery = true)
    Long sumVisibleFileSize(@Param("userId") Long userId,
                            @Param("userRole") String userRole);

    /**
     * 查询最近上传的可见文档 — 工作台「最近上传」列表。
     *
     * @param userId   当前用户 ID
     * @param userRole 当前用户角色
     * @param limit    返回条数
     * @return 按上传时间倒序的文档列表
     */
    @Query(value = "SELECT d.* FROM documents d " +
           "WHERE d.deleted = 0 AND " + VISIBLE_PREDICATE + " ORDER BY d.created_at DESC LIMIT :limit",
           nativeQuery = true)
    List<Document> findRecentVisible(@Param("userId") Long userId,
                                      @Param("userRole") String userRole,
                                      @Param("limit") int limit);
}
