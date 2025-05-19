package com.example.demo.storage;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;


@Configuration
public class MongoUtil {


    @Autowired
    private MongoProperties mongoProperties;


    public MongoDatabase getDatabase() {
        MongoClient mongoClient = MongoClients.create(mongoProperties.getUri());
        return mongoClient.getDatabase(mongoProperties.getDatabase());
    }


    public MongoCollection<Document> getUserChunkCollection(String user) {
        String collectionName = user;
        return getDatabase().getCollection(collectionName);
    }
}
