package com.leon.agenthub.permission.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.leon.agenthub.auth.model.User;
import com.leon.agenthub.auth.repository.UserRepository;
import com.leon.agenthub.common.dto.PageQuery;
import com.leon.agenthub.common.dto.PageResult;
import com.leon.agenthub.common.exception.BusinessException;
import com.leon.agenthub.knowledge.model.Document;
import com.leon.agenthub.knowledge.repository.DocumentRepository;
import com.leon.agenthub.permission.converter.DocPermissionConverter;
import com.leon.agenthub.permission.dto.DocPermissionVO;
import com.leon.agenthub.permission.model.DocPermission;
import com.leon.agenthub.permission.repository.DocPermissionRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link DocumentPermissionService} 单元测试 — 覆盖授权的创建、覆盖、校验与撤销。
 */
class DocumentPermissionServiceTest {

    private DocPermissionRepository docPermissionRepository;
    private DocumentRepository documentRepository;
    private UserRepository userRepository;
    private DocumentPermissionService service;

    @BeforeEach
    void setUp() {
        docPermissionRepository = mock(DocPermissionRepository.class);
        documentRepository = mock(DocumentRepository.class);
        userRepository = mock(UserRepository.class);
        service = new DocumentPermissionService(
                docPermissionRepository, documentRepository, userRepository, new DocPermissionConverter());

        Document document = new Document();
        document.setId(10L);
        document.setTitle("员工手册");
        document.setSecurityLevel("CONFIDENTIAL");
        when(documentRepository.findById(10L)).thenReturn(Optional.of(document));
        when(documentRepository.findAllById(any())).thenReturn(List.of(document));

        User user = new User();
        user.setId(99L);
        user.setUsername("zhangsan");
        user.setRealName("张三");
        user.setRole("VIEWER");
        when(userRepository.findById(99L)).thenReturn(Optional.of(user));
        when(userRepository.findAllById(any())).thenReturn(List.of(user));
    }

    @Test
    void 授予权限时创建授权记录() {
        when(docPermissionRepository.findByDocumentAndUser(10L, 99L)).thenReturn(null);
        when(docPermissionRepository.save(any(DocPermission.class))).thenAnswer(inv -> {
            DocPermission p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        DocPermissionVO vo = service.grant(10L, 99L, "WRITE");

        assertEquals(10L, vo.getDocumentId());
        assertEquals("员工手册", vo.getDocumentTitle());
        assertEquals("张三", vo.getGranteeName());
        assertEquals("WRITE", vo.getPermission());
        assertEquals("读写", vo.getPermissionLabel());
        verify(docPermissionRepository).save(any(DocPermission.class));
    }

    @Test
    void 已存在授权时覆盖权限类型() {
        DocPermission existing = new DocPermission();
        existing.setId(5L);
        existing.setDocumentId(10L);
        existing.setGranteeId(99L);
        existing.setPermission("READ");
        when(docPermissionRepository.findByDocumentAndUser(10L, 99L)).thenReturn(existing);
        when(docPermissionRepository.save(existing)).thenReturn(existing);

        DocPermissionVO vo = service.grant(10L, 99L, "WRITE");

        assertEquals("WRITE", vo.getPermission());
        verify(docPermissionRepository).save(existing);
    }

    @Test
    void 文档不存在时抛出404() {
        when(documentRepository.findById(404L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.grant(404L, 99L, "READ"));

        assertEquals(404, ex.getCode());
    }

    @Test
    void 用户不存在时抛出404() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.grant(10L, 404L, "READ"));

        assertEquals(404, ex.getCode());
    }

    @Test
    void 非法权限类型抛出400() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.grant(10L, 99L, "DELETE"));

        assertEquals(400, ex.getCode());
    }

    @Test
    void 分页查询返回补齐文档与用户信息() {
        DocPermission p = new DocPermission();
        p.setId(1L);
        p.setDocumentId(10L);
        p.setGranteeId(99L);
        p.setPermission("READ");
        when(docPermissionRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p)));

        PageQuery query = new PageQuery();
        query.setPage(1);
        query.setSize(20);
        PageResult<DocPermissionVO> page = service.listAll(query);

        assertEquals(1, page.getTotal());
        assertEquals(1, page.getPage());
        assertEquals("员工手册", page.getList().get(0).getDocumentTitle());
        assertEquals("张三", page.getList().get(0).getGranteeName());
        assertEquals("CONFIDENTIAL", page.getList().get(0).getDocumentSecurityLevel());
    }

    @Test
    void 撤销授权时删除记录() {
        when(docPermissionRepository.deleteByDocumentAndUser(10L, 99L)).thenReturn(1);

        assertTrue(service.revoke(10L, 99L));

        verify(docPermissionRepository).deleteByDocumentAndUser(10L, 99L);
    }

    @Test
    void 撤销不存在的授权返回false() {
        when(docPermissionRepository.deleteByDocumentAndUser(anyLong(), anyLong())).thenReturn(0);

        assertFalse(service.revoke(10L, 99L));
    }

    @Test
    void 授权查询按分页参数下推到仓储() {
        when(docPermissionRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageQuery query = new PageQuery();
        query.setPage(3);
        query.setSize(50);
        service.listAll(query);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(docPermissionRepository).findAllByOrderByCreatedAtDesc(captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(50, captor.getValue().getPageSize());
    }

    @Test
    void 按文档查询返回授权列表() {
        DocPermission p = new DocPermission();
        p.setId(1L);
        p.setDocumentId(10L);
        p.setGranteeId(99L);
        p.setPermission("READ");
        when(docPermissionRepository.findByDocumentId(10L)).thenReturn(List.of(p));

        List<DocPermissionVO> list = service.listByDocument(10L);

        assertEquals(1, list.size());
        assertEquals("只读", list.get(0).getPermissionLabel());
    }

    @Test
    void 未知文档授权类型原样返回标签() {
        DocPermission p = new DocPermission();
        p.setId(1L);
        p.setDocumentId(10L);
        p.setGranteeId(99L);
        p.setPermission("LEGACY");
        when(docPermissionRepository.findByDocumentId(10L)).thenReturn(List.of(p));

        List<DocPermissionVO> list = service.listByDocument(10L);

        assertEquals("LEGACY", list.get(0).getPermissionLabel());
    }

    @Test
    void 空授权列表返回空结果() {
        when(docPermissionRepository.findByDocumentId(anyLong())).thenReturn(List.of());

        assertTrue(service.listByDocument(10L).isEmpty());
    }

    @Test
    void 参数为空时抛出400() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.grant(null, 99L, "READ"));
        assertEquals(400, ex.getCode());

        BusinessException ex2 = assertThrows(BusinessException.class, () -> service.revoke(10L, null));
        assertEquals(400, ex2.getCode());
    }

    @Test
    void 权限类型缺省时按只读处理() {
        when(docPermissionRepository.findByDocumentAndUser(10L, 99L)).thenReturn(null);
        when(docPermissionRepository.save(any(DocPermission.class))).thenAnswer(inv -> inv.getArgument(0));

        DocPermissionVO vo = service.grant(10L, 99L, null);

        assertEquals("READ", vo.getPermission());
        assertEquals("只读", vo.getPermissionLabel());
    }
}
