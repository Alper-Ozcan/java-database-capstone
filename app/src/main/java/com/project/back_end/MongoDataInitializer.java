package com.project.back_end;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.util.List;

@Component
public class MongoDataInitializer {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public MongoDataInitializer(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void initMongoData() {
        // Kilitlenmeyi önlemek için işlemi arka plan thread'ine alıyoruz
        new Thread(() -> {
            String collectionName = "prescriptions";
            try {
                // MongoDB bağlantısının tam oturması için 1 saniye beklesin
                Thread.sleep(1000); 

                long count = mongoTemplate.getCollection(collectionName).countDocuments();
                System.out.println(">> [MongoDB Kontrol] Mevcut kayıt sayısı: " + count);

                if (count == 0) {
                    InputStream inputStream = TypeReference.class.getResourceAsStream("/prescriptions.json");
                    if (inputStream == null) {
                        System.err.println(">> [MongoDB HATA] prescriptions.json dosyası resources altında bulunamadı!");
                        return;
                    }

                    List<Document> documents = objectMapper.readValue(inputStream, new TypeReference<List<Document>>(){});
                    
                    for (Document doc : documents) {
                        if (doc.containsKey("_id")) {
                            String idStr = doc.get("_id").toString();
                            doc.put("_id", new org.bson.types.ObjectId(idStr));
                        }
                    }

                    mongoTemplate.insert(documents, collectionName);
                    System.out.println(">> [MongoDB Başarılı] " + documents.size() + " adet reçete dokümanı oluşturuldu ve yüklendi!");
                } else {
                    System.out.println(">> [MongoDB] Kayıtlar mevcut, insert atlatıldı.");
                }
            } catch (Exception e) {
                System.err.println(">> [MongoDB Arka Plan Hatası]: " + e.getMessage());
            }
        }).start();
    }
}