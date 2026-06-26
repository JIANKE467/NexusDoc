package com.nexusdoc.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceDataImportVO {

    private Integer importedDocuments;

    private Integer importedChatRecords;
}
