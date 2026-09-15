package com.example.form;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 表单系统启动类.
 */
@SpringBootApplication
@MapperScan("com.example.form.mapper")
@EnableCaching
public class FormSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(FormSystemApplication.class, args);
    }
}
