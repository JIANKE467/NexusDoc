ALTER TABLE document
    ADD COLUMN device_id VARCHAR(128) NOT NULL DEFAULT 'legacy-device' COMMENT '匿名设备ID';

CREATE INDEX idx_document_device_id ON document(device_id);

ALTER TABLE chat_record
    ADD COLUMN device_id VARCHAR(128) NOT NULL DEFAULT 'legacy-device' COMMENT '匿名设备ID';

CREATE INDEX idx_chat_device_id ON chat_record(device_id);
