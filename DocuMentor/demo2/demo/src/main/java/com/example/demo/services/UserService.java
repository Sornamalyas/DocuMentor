package com.example.demo.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.demo.model.UserDocument;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.mongodb.client.MongoCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;
    public User getUserByEmail(String email) {
        logger.info("Fetching user by email: {}", email);
        return userRepository.findByEmail(email);
    }
    public User createUser(User user) {
        logger.info("Creating user: {}", user.getEmail());
        return userRepository.save(user);
    }
    @Autowired
    private MongoTemplate mongoTemplate;
    public String uploadDocument(MultipartFile file, String userEmail) throws IOException {
        logger.info("Uploading document: {} for user: {}", file.getOriginalFilename(), userEmail);
        User user = getUserByEmail(userEmail);
        if (user == null) {
            logger.warn("User not found: {}", userEmail);
            throw new RuntimeException("User not found");
        }
        if (user.getDocument() != null && user.getDocument().getDocName().equals(file.getOriginalFilename())) {
            logger.info("Duplicate file upload attempt for user: {}", user.getEmail());
            return "File already uploaded with the same content.";
        }
        if (!mongoTemplate.collectionExists(user.getName())) {
            logger.info("Creating new collection and search index for user: {}", user.getName());
            MongoCollection<org.bson.Document> userCollection= mongoTemplate.createCollection(user.getName());
            org.bson.Document index=createSearchIndex();
            userCollection.createSearchIndex("searchEmbedding",index);
            logger.debug("Search index created for collection: {}", user.getName());
        }
        // Set the reference to the new collection
        user.setDocument(new UserDocument(file.getOriginalFilename()));
        // Save the user document in the "users" collection
        userRepository.save(user);
        logger.info("File {} uploaded successfully for user {}", file.getOriginalFilename(), user.getEmail());
        return "PDF uploaded and stored successfully.";
    }
    public boolean fileExistsForUser(User user, String filename) {
        if (user!=null) {
            logger.info("File {} existed for user : {}",filename,user.getEmail());
            return user.getDocument() != null && user.getDocument().getDocName().equals(filename);
        }
        logger.info("File {} not existed for user : {}",filename,user.getEmail());
        return false;
    }
    public org.bson.Document createSearchIndex()
    {
        logger.info("Creating search index configuration document");
        org.bson.Document indexDefinition = new org.bson.Document("mappings", new org.bson.Document()
                .append("dynamic", false)
                .append("fields", new org.bson.Document()
                        .append("embedding", new org.bson.Document()
                                .append("type", "knnVector")
                                .append("dimensions", 384)
                                .append("similarity", "cosine")
                        )
                )
        );
        return indexDefinition;
    }
}
