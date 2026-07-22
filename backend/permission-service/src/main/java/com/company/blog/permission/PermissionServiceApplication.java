package com.company.blog.permission;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PermissionServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PermissionServiceApplication.class, args);
    }

    @Bean
    PermissionPolicy permissionPolicy() {
        return new PermissionPolicy();
    }
}
