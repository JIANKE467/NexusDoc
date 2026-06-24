package com.nexusdoc.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_package")
public class DocumentPackage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private String resultText;

    private LocalDateTime createTime;
}
