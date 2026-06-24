package com.nexusdoc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.nexusdoc.mapper")
@SpringBootApplication
public class NexusDocApplication {

    public static void main(String[] args) {
        SpringApplication.run(NexusDocApplication.class, args);
    }
}
