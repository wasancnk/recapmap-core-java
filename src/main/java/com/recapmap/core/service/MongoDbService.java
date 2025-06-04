package com.recapmap.core.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.bson.Document;

@Service
public class MongoDbService {
    private final MongoTemplate mongoTemplate;

    @Autowired
    public MongoDbService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void saveExtractedDocument(Document doc, String collectionName) {
        mongoTemplate.insert(doc, collectionName);
    }
}
