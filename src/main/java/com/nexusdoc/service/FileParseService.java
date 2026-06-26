package com.nexusdoc.service;

import com.nexusdoc.vo.FileExtractResultVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileParseService {

    FileExtractResultVO extract(MultipartFile file);
}
