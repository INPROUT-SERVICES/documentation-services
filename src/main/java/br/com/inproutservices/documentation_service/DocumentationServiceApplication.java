package br.com.inproutservices.documentation_service;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients; // <-- Import adicionado

import java.util.TimeZone;

@SpringBootApplication
@EnableFeignClients
public class DocumentationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentationServiceApplication.class, args);
    }

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
    }
}