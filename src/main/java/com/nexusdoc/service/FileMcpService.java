package com.nexusdoc.service;

import com.nexusdoc.vo.FileExtractResultVO;
import com.nexusdoc.vo.FileMcpCapabilitiesVO;
import com.nexusdoc.vo.FileMcpGenerateVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileMcpService {

    FileMcpCapabilitiesVO getCapabilities();

    FileExtractResultVO parseFile(MultipartFile file);

    FileMcpGenerateVO generateFromFile(MultipartFile file,
                                       Long userId,
                                       String mode,
                                       Boolean enableWebSearch,
                                       String cardTypes,
                                       String requirement);
}
