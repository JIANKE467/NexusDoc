package com.nexusdoc.service;

import com.nexusdoc.dto.DocumentGenerateRequest;
import com.nexusdoc.vo.DocumentDetailVO;
import com.nexusdoc.vo.DocumentListVO;
import com.nexusdoc.vo.WebSearchResultVO;

import java.util.List;
import java.util.function.Consumer;

public interface DocumentService {

    DocumentDetailVO generateDocument(DocumentGenerateRequest request);

    DocumentDetailVO generateDocumentStream(DocumentGenerateRequest request,
                                            Consumer<String> onDelta,
                                            Consumer<WebSearchResultVO> onSource,
                                            Consumer<String> onWarning);

    List<DocumentListVO> listDocuments(Long userId);

    DocumentDetailVO getDocumentDetail(Long documentId);

    void deleteDocument(Long documentId);
}
