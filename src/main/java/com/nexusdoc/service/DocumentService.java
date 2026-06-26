package com.nexusdoc.service;

import com.nexusdoc.dto.DocumentGenerateRequest;
import com.nexusdoc.dto.DeviceDataImportRequest;
import com.nexusdoc.vo.DeviceDataExportVO;
import com.nexusdoc.vo.DeviceDataImportVO;
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

    List<DocumentListVO> listDocuments(String deviceId);

    DocumentDetailVO getDocumentDetail(Long documentId, String deviceId);

    void deleteDocument(Long documentId, String deviceId);

    DeviceDataExportVO exportDeviceData(String deviceId);

    DeviceDataImportVO importDeviceData(String deviceId, DeviceDataImportRequest request);
}
