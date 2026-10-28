package com.leon.agenthub.knowledge.service;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final VectorStore pgVectorStore;


    public void addDocuments(List<Document> documents) {
        pgVectorStore.add(documents);
    }

}
