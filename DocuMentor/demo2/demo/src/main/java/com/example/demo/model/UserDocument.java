package com.example.demo.model;


import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Field;


import java.util.Date;


public class UserDocument {


    @Field("docName")
    private String docName;
    @CreatedDate
    private Date uploadDate;
    // Constructor
    public UserDocument(String docName) {
        this.docName = docName;
        this.uploadDate=new Date();
    }


    // Getter and Setter
    public String getDocName() {
        return docName;
    }


    public void setDocName(String docName) {
        this.docName = docName;
    }
}
