package com.wai.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wai.admin.mapper")
public class WaiAdminBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WaiAdminBackendApplication.class, args);
    }

} 