package com.example.demo.chatbot;
import com.mongodb.client.MongoClients;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import com.mongodb.client.MongoClient;
import dev.langchain4j.store.embedding.mongodb.IndexMapping;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;
import java.util.*;
import java.util.stream.Stream;
import static java.time.Duration.ofSeconds;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Chunker {
    private static final Logger logger = LoggerFactory.getLogger(Chunker.class);


    public static void storeChunks(String text,String user) {
        Preprocessor obj=new Preprocessor();
        String preProcessedText=obj.Preprocess(Document.document(text));
        System.out.println("Preprocessed String : "+preProcessedText);
        DocumentByParagraphSplitter chunker = new DocumentByParagraphSplitter(600, 70);
        List<TextSegment> chunks = chunker.split(Document.document(preProcessedText));//remove metadata from each string
        chunks = chunks.stream()
                .map(chunk -> {
                    // Retrieve the existing metadata from the
                    String fin = "";
                    //fin = "In the document\n" + chunk.text();
                    fin =  chunk.text();
                    Metadata metadata = chunk.metadata();
                    // Modify the metadata
                    metadata.put("timestamp", String.valueOf(System.currentTimeMillis())); // Current time
                    metadata.put("word_count", String.valueOf(chunk.text().split("\\s+").length)); // Word count
                    metadata.put("text_type", "plain");// Text type
                    metadata.remove("absolute_directory_path"); // Remove unwanted metadata


                    // Return a new TextSegment with updated text and metadata
                    return new TextSegment(fin, metadata); // Assuming chunk.text() and metadata are used to create TextSegment
                })
                .toList();
        logger.info("Initialisation of embedding model by user : {}",user);
        EmbeddingModel embeddingModel = HuggingFaceEmbeddingModel.builder()
                .accessToken(System.getProperty("HUGGINGFACE_TOKEN"))
                .modelId("sentence-transformers/all-MiniLM-L6-v2")
                .waitForModel(true)
                .timeout(ofSeconds(60))
                .build();


        int batchSize = 6;
        List<List<TextSegment>> chunkBatches = batchList(chunks, batchSize);

        logger.info("Starting embedding process with {} batches", chunkBatches.size());
        List<Embedding> allEmbeddings = chunkBatches.parallelStream()
                .flatMap(batch -> {
                    try {
                        Response<List<Embedding>> response = embeddingModel.embedAll(batch);
                        System.out.println("Chunks: " + response);


                        return response.content().stream();


                    } catch (Exception e) {
                        System.err.println("Embedding batch failed: " + e.getMessage());
                        return Stream.empty();  // or throw
                    }
                })
                .toList();


        String uri=System.getProperty("MONGODB_CLUSTER");

        MongoClient mongoClient = MongoClients.create(uri);
        MongoDbEmbeddingStore vdb = MongoDbEmbeddingStore.builder().fromClient(mongoClient).databaseName("chatbot").collectionName(user).indexName("searchEmbedding").indexMapping(IndexMapping.builder()
                .dimension(embeddingModel.dimension())
                .metadataFieldNames(Set.of())
                .build()).build();
        logger.info("Adding embeddings to MongoDB for user: {}", user);


        List<Map.Entry<Embedding, TextSegment>> embeddingPairs = chunkBatches.parallelStream()
                .flatMap(batch -> {
                    try {
                        Response<List<Embedding>> response = embeddingModel.embedAll(batch);
                        List<Embedding> embeddings = response.content();
                        return Stream.iterate(0, i -> i < embeddings.size(), i -> i + 1)
                                .map(i -> Map.entry(embeddings.get(i), batch.get(i)));
                    } catch (Exception e) {
                        System.err.println("Embedding batch failed: " + e.getMessage());
                        return Stream.<Map.Entry<Embedding, TextSegment>>empty();
                    }
                })
                .toList();




        List<Embedding> embeddingBatch = embeddingPairs.stream()
                .map(Map.Entry::getKey)
                .toList();
        List<TextSegment> chunkBatch = embeddingPairs.stream()
                .map(Map.Entry::getValue)
                .toList();


        vdb.addAll(embeddingBatch, chunkBatch);
        logger.info("Embeddings stored successfully for user: {}", user);
    }
    //splitting into list of smaller sizes
    public static <T> List<List<T>> batchList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            batches.add(list.subList(i, end));
        }
        return batches;
    }


}
