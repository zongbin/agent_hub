package com.leon.agenthub.knowledge.spliter;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentSpliter {


    public List<Document> split(List<Document> documents) {
        TokenTextSplitter tokenTextSplitter = new TokenTextSplitter(1000,400,5,6000,true);
        return tokenTextSplitter.apply(documents);
    }
}
