package com.example.demo.chatbot;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import static java.time.Duration.ofSeconds;

public class Searcher {

    private static final Logger logger = LoggerFactory.getLogger(Searcher.class); // Logger instance

    private final MongoDbEmbeddingStore vdb;
    private final EmbeddingModel embeddingModel;

    // Constructor to inject dependencies
    public Searcher(MongoDbEmbeddingStore vdb) {
        this.vdb = vdb;
        this.embeddingModel = HuggingFaceEmbeddingModel.builder()
                .accessToken(System.getProperty("HUGGINGFACE_TOKEN"))
                .modelId("sentence-transformers/all-MiniLM-L6-v2")
                .waitForModel(true)
                .timeout(ofSeconds(60))
                .build();
    }

    public String retrieve(String query) {
        try {
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .minScore(0.6)
                    .maxResults(3)
                    .build();

            EmbeddingSearchResult<TextSegment> searchResult = vdb.search(searchRequest);
            List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

            StringBuilder resk = new StringBuilder();
            for (EmbeddingMatch<TextSegment> embeddingMatch : matches) {
                resk.append("\n").append(embeddingMatch.embedded().text());
            }
            System.out.println("OUTPUT: "+"\n"+resk);
            return resk.toString();
        } catch (Exception e) {
            logger.error("Error occurred during search: {}", e.getMessage(), e); // Log error with stack trace
            return "Error occurred. Please try again.";
        }
    }
}
