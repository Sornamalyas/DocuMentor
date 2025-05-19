package com.example.demo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demo.model.User;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;
import dev.langchain4j.store.embedding.mongodb.IndexMapping;
import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.util.HashSet;


@Service
public class EmbeddingStoreService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingStoreService.class);
    private MongoDbEmbeddingStore embeddingStore;
    private IndexMapping indexMapping;


    @Autowired
    public EmbeddingStoreService(@Value("${embedding.dimensions}") int dimensions) {
        Boolean createIndex = true;
        logger.info("Initializing EmbeddingStoreService with dimensions: {}", dimensions);
        this.indexMapping = IndexMapping.builder()
                .dimension(dimensions)
                .metadataFieldNames(new HashSet<>())
                .build();
    }


    public MongoDbEmbeddingStore getEmbeddingStore(MongoClient mongoClient, User user) {
        logger.info("Attempting to get embedding store for user: {}", user.getName());
        try {
            this.embeddingStore = MongoDbEmbeddingStore.builder().fromClient(mongoClient).databaseName("chatbot").collectionName(user.getName()).indexName("searchEmbedding").indexMapping(indexMapping).build();
            logger.debug("Successfully created MongoDbEmbeddingStore for user: {}", user.getName());
        } catch (Exception e) {
            logger.error("Error creating embedding store for user {}: {}", user.getName(), e.getMessage(), e);
            return null;
        }
        return this.embeddingStore;
    }
}
