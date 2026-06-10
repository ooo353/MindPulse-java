package com.mindpulse.backend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mindpulse.backend.mapper")
public class MindPulseBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MindPulseBackendApplication.class, args);
    }

}
