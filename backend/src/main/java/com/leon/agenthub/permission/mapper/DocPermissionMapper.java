package com.leon.agenthub.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leon.agenthub.permission.model.DocPermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档级授权 Mapper — 查询和管理 doc_permissions 表。
 */
@Mapper
public interface DocPermissionMapper extends BaseMapper<DocPermission> {

    /**
     * 查询用户对指定文档的授权记录。
     *
     * @param documentId 文档 ID
     * @param granteeId  用户 ID
     * @return 授权记录，不存在返回 null
     */
    @Select("SELECT * FROM doc_permissions WHERE document_id = #{documentId} AND grantee_id = #{granteeId}")
    DocPermission selectByDocumentAndUser(@Param("documentId") Long documentId, @Param("granteeId") Long granteeId);

    /**
     * 查询用户对任意文档的授权列表。
     *
     * @param granteeId 用户 ID
     * @return 授权记录列表
     */
    @Select("SELECT * FROM doc_permissions WHERE grantee_id = #{granteeId}")
    List<DocPermission> selectByGrantee(@Param("granteeId") Long granteeId);

    /**
     * 删除用户对指定文档的授权。
     *
     * @param documentId 文档 ID
     * @param granteeId  用户 ID
     * @return 删除行数
     */
    @Delete("DELETE FROM doc_permissions WHERE document_id = #{documentId} AND grantee_id = #{granteeId}")
    int deleteByDocumentAndUser(@Param("documentId") Long documentId, @Param("granteeId") Long granteeId);
}
