package com.nexusdoc.service.support;

import com.nexusdoc.entity.ChatRecord;
import com.nexusdoc.entity.Document;
import com.nexusdoc.entity.DocumentPackage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class InMemoryDocumentStore {

    private final AtomicLong documentId = new AtomicLong(1);
    private final AtomicLong packageId = new AtomicLong(1);
    private final AtomicLong chatRecordId = new AtomicLong(1);
    private final Map<Long, Document> documents = new ConcurrentHashMap<>();
    private final Map<Long, DocumentPackage> packages = new ConcurrentHashMap<>();
    private final Map<Long, List<ChatRecord>> chatRecords = new ConcurrentHashMap<>();

    public void saveDocument(Document document) {
        if (document.getId() == null) {
            document.setId(documentId.getAndIncrement());
        }
        if (document.getCreateTime() == null) {
            document.setCreateTime(LocalDateTime.now());
        }
        documents.put(document.getId(), document);
    }

    public List<Document> listDocuments(String deviceId) {
        return documents.values().stream()
                .filter(document -> deviceId.equals(document.getDeviceId()))
                .sorted(Comparator.comparing(Document::getCreateTime).reversed())
                .toList();
    }

    public Document findDocument(Long id) {
        return documents.get(id);
    }

    public Document findDocument(Long id, String deviceId) {
        Document document = documents.get(id);
        return document != null && deviceId.equals(document.getDeviceId()) ? document : null;
    }

    public void deleteDocument(Long id) {
        documents.remove(id);
        packages.remove(id);
        chatRecords.remove(id);
    }

    public void deleteDocument(Long id, String deviceId) {
        Document document = documents.get(id);
        if (document == null || !deviceId.equals(document.getDeviceId())) {
            return;
        }
        deleteDocument(id);
    }

    public void savePackage(DocumentPackage documentPackage) {
        if (documentPackage.getId() == null) {
            documentPackage.setId(packageId.getAndIncrement());
        }
        if (documentPackage.getCreateTime() == null) {
            documentPackage.setCreateTime(LocalDateTime.now());
        }
        packages.put(documentPackage.getDocumentId(), documentPackage);
    }

    public DocumentPackage findPackage(Long documentId) {
        return packages.get(documentId);
    }

    public void saveChatRecord(ChatRecord chatRecord) {
        if (chatRecord.getId() == null) {
            chatRecord.setId(chatRecordId.getAndIncrement());
        }
        if (chatRecord.getCreateTime() == null) {
            chatRecord.setCreateTime(LocalDateTime.now());
        }
        chatRecords.computeIfAbsent(chatRecord.getDocumentId(), ignored -> new ArrayList<>()).add(chatRecord);
    }

    public List<ChatRecord> listChatRecords(Long documentId) {
        return chatRecords.getOrDefault(documentId, List.of()).stream()
                .sorted(Comparator.comparing(ChatRecord::getCreateTime))
                .toList();
    }

    public List<ChatRecord> listChatRecords(Long documentId, String deviceId) {
        return chatRecords.getOrDefault(documentId, List.of()).stream()
                .filter(record -> deviceId.equals(record.getDeviceId()))
                .sorted(Comparator.comparing(ChatRecord::getCreateTime))
                .toList();
    }
}
