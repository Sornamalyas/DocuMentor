/* (C)2024 */
package com.example.demo.chatbot;
import com.cohere.api.Cohere;
import com.cohere.api.resources.v2.requests.V2ChatRequest;
import com.cohere.api.types.*;
import com.example.demo.model.User;
import com.example.demo.services.EmbeddingStoreService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.langchain4j.store.embedding.mongodb.MongoDbEmbeddingStore;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service

public class QABot {
    private static final String cohereToken = System.getProperty("COHERE_TOKEN");
    private static final String uri = System.getProperty("MONGODB_CLUSTER");

    private static final Logger logger = LoggerFactory.getLogger(QABot.class);
    public static String getAnswer(User user, String userInput) {
        try {
            // Initialize MongoDB connection and Embedding store
            logger.info("Starting process for user: {}", user.getEmail());
            MongoClient mongoClient = MongoClients.create(uri);
            EmbeddingStoreService embeddingStoreService = new EmbeddingStoreService(384);
            MongoDbEmbeddingStore vdb = embeddingStoreService.getEmbeddingStore(mongoClient, user);
            if(vdb==null){
                logger.warn("Embedding store is null for user: {}", user.getEmail());
                return "Upload file";
            }



            // Initialize Cohere client
            logger.info("Started interaction - Cohere API by user: {}",user.getEmail());
            Cohere cohere = Cohere.builder().token(cohereToken).clientName("snippet").build();


            // Search document content based on the user's input
            Searcher vec = new Searcher(vdb);
            String results = vec.retrieve(userInput);


            // Build the conversation history
            List<ChatMessageV2> conversationHistory = new ArrayList<>();
            String conversationContent = "Your name is DocuMentor, you are a Document based QA Bot.\n" +
                    "                            You are friendly, personable and polite\n\n.Based on this: " + Arrays.toString(results.split("\n\n")) + " answer " + userInput ;


            ChatMessageV2 userMessage = ChatMessageV2.user(
                    UserMessage.builder()
                            .content(UserMessageContent.of(conversationContent))
                            .build()
            );
            conversationHistory.add(userMessage);


            // Build the chat request with the full conversation history
            V2ChatRequest chatRequest = V2ChatRequest.builder()
                    .model("command-r-plus-08-2024")
                    .messages(conversationHistory) // Pass entire conversation history
                    .build();


            // Call the Cohere API
            ChatResponse response = cohere.v2().chat(chatRequest);


            // Get the assistant's response from the API
            String ans = response.getMessage().getContent().get().get(0).getText().get().getText().translateEscapes();


            // Handle empty results gracefully
            if (results == null || results.isEmpty()) {
                ans = "Sorry, I couldn't find relevant information in the document for your query. But I found other information related to your query:\n" + ans;
            }


            return ans;


        }catch (com.cohere.api.core.CohereApiError e) {
            // Log if you *really* need to
            logger.error("Cohere API error: {}", e.getMessage(), e);
            // Return a safe message for the frontend
            return "Sorry, the AI service is temporarily unavailable. Please try again.";
        }
        catch (Exception e) {
            logger.error("Unexpected error occurred: {}", e.getMessage(), e);
            return "An error occurred while processing your request.";
        }
    }
}
