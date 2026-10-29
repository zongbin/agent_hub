package com.leon.agenthub.permission.repository;

import com.leon.agenthub.permission.model.DocPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档级授权数据访问接口 — 查询和管理 doc_permissions 表。
 */
@Repository
public interface DocPermissionRepository extends JpaRepository<DocPermission, Long> {

    /**
     * 查询用户对指定文档的授权记录。
     */
    @Query("SELECT dp FROM DocPermission dp WHERE dp.documentId = :documentId AND dp.granteeId = :granteeId")
    DocPermission findByDocumentAndUser(@Param("documentId") Long documentId, @Param("granteeId") Long granteeId);

    /**
     * 查询用户对任意文档的授权列表。
     */
    List<DocPermission> findByGranteeId(Long granteeId);

    /**
     * 判断用户是否对指定文档拥有授权（任一权限类型均视为可读）。
     */
    boolean existsByDocumentIdAndGranteeId(Long documentId, Long granteeId);

    /**
     * 查询文档的授权列表。
     */
    List<DocPermission> findByDocumentId(Long documentId);

    /**
     * 删除用户对指定文档的授权。
     *
     * @return 删除行数
     */
    @Modifying
    @Query("DELETE FROM DocPermission dp WHERE dp.documentId = :documentId AND dp.granteeId = :granteeId")
    int deleteByDocumentAndUser(@Param("documentId") Long documentId, @Param("granteeId") Long granteeId);
}
