package com.example.demo.model;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Field;
import java.util.Date;

@org.springframework.data.mongodb.core.mapping.Document(collection = "users")
public class User {
    @Field("name")
    private String name;
    @Id
    @Indexed(unique = true)
    String id;
    @Field("email")
    private String email;
    @Field("document")
    private UserDocument document;
    @CreatedDate
    private Date createdAt;
    ;


    public User(String name, String email) {
        this.name = name;
        this.email = email;
        this.document=new UserDocument("");
        this.createdAt = new Date();




    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getEmail() {
        return email;
    }


    public void setEmail(String email) {
        this.email = email;
    }
    // Getters and setters
    public UserDocument getDocument() {
        return document;
    }


    public void setDocument(UserDocument pdf) {
        this.document = pdf;
    }




}
