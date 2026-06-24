package com.nexusdoc.service;

import com.nexusdoc.dto.DocumentGenerateRequest;
import com.nexusdoc.vo.DocumentDetailVO;
import com.nexusdoc.vo.DocumentListVO;

import java.util.List;

public interface DocumentService {

    DocumentDetailVO generateDocument(DocumentGenerateRequest request);

    List<DocumentListVO> listDocuments(Long userId);

    DocumentDetailVO getDocumentDetail(Long documentId);

    void deleteDocument(Long documentId);
}
