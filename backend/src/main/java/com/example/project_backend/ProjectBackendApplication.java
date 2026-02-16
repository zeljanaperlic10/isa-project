package com.example.project_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.example.project_backend",
    "security",
    "controller",
    "service",
    "model",
    "config",
    "repository",
    "dto"
})
@EnableJpaRepositories(basePackages = "repository")  
@EntityScan(basePackages = "model")  
public class ProjectBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjectBackendApplication.class, args);
    }
}
