package com.leon.agenthub.qa.rag;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.leon.agenthub.common.advisor.MyLoggerAdvisor;
import com.leon.agenthub.common.llm.ChatCitation;
import com.leon.agenthub.common.llm.SystemPromptFactory;
import com.leon.agenthub.permission.enums.UserRole;
import com.leon.agenthub.permission.service.DocumentAccessFilter;
import com.leon.agenthub.qa.converter.QaConverter;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RAG 检索器 — 基于 Spring AI 封装的组件完成「查询改写 + 权限感知召回 + 参考资料组装」。
 *
 * <ul>
 * <li>{@link RewriteQueryTransformer}：把口语化提问改写成适合检索的形式</li>
 * <li>{@link VectorStoreDocumentRetriever}：带过滤表达式的向量召回（权限过滤在此注入）</li>
 * </ul>
 *
 * <p>
 * 召回结果同时产出「引用来源」与「参考资料文本」，两者编号一一对应，
 * 便于大模型在答案中标注 [1]、[2]，前端据此跳转对应文档详情页。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagRetriever {


    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;
    private final DocumentAccessFilter documentAccessFilter;
    private final QaConverter qaConverter;

    /** 检索返回的分块数 */
    @Value("${agenthub.qa.top-k:5}")
    private int topK;
    @Value("${agenthub.qa.similarity-threshold:0.8}")
    private double similarityThreshold;

    /** 是否启用查询改写（关闭可省一次模型调用） */
    @Value("${agenthub.qa.query-rewrite-enabled:true}")
    private boolean queryRewriteEnabled;

    /** 查询改写器（Spring AI 提供，构造一次复用） */
    private QueryTransformer queryTransformer;

    @PostConstruct
    void init() {
        chatClientBuilder.defaultAdvisors(new MyLoggerAdvisor());
        this.queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .promptTemplate(SystemPromptFactory.getReWiteSystemPrompt())
                .targetSearchSystem("企业知识库")
                .build();
    }

    /**
     * 执行一次 RAG 召回。
     *
     * @param question 用户问题
     * @param userId   当前用户 ID（用于文档级授权判断）
     * @param userRole 当前用户角色
     * @return 检索上下文（引用来源 + 参考资料文本）
     */
    public RagContext retrieve(String question, Long userId, UserRole userRole) {

        // 1、检索前查询重写
        String query = queryRewriteEnabled ? rewriteQuery(question) : question;
        // 2、文档检索
        VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(similarityThreshold)
                .topK(topK)
                // ADMIN 不过滤；其余角色按「角色×密级 + 文档级授权」过滤
                .filterExpression(() -> documentAccessFilter.buildFilter(userRole, userId))
                .build();

        List<Document> documents = retriever.retrieve(new Query(query));
        List<ChatCitation> citations = qaConverter.toCitations(documents);
        log.debug("RAG retrieved {} chunks for query: {}", documents.size(), query);
        return new RagContext(query, citations, buildContextText(documents));
    }

    /**
     * 查询改写 —— 把口语化提问转成适合向量检索的表述。
     */
    private String rewriteQuery(String question) {
        try {
            return queryTransformer.transform(new Query(question)).text();
        } catch (Exception e) {
            log.warn("查询改写失败，回退为原始问题: {}", e.getMessage());
            return question;
        }
    }

    /**
     * 组装参考资料文本 —— 编号与引用来源顺序一致，供大模型标注来源编号。
     */
    private String buildContextText(List<Document> documents) {
        if (documents.isEmpty()) {
            return "（知识库中未检索到与问题相关的资料）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Object titleMeta = doc.getMetadata() == null ? null : doc.getMetadata().get("title");
            String title = titleMeta == null ? "" : titleMeta.toString();
            Object chunkIndex = doc.getMetadata() == null ? null : doc.getMetadata().get("chunkIndex");
            String segment = chunkIndex instanceof Number number ? "第 " + (number.intValue() + 1) + " 段" : "";
            sb.append("[").append(i + 1).append("] ")
                    .append(title).append(segment.isEmpty() ? "" : "（" + segment + "）")
                    .append("：\n")
                    .append(doc.getText() == null ? "" : doc.getText())
                    .append("\n\n");
        }
        return sb.toString().trim();
    }
}
