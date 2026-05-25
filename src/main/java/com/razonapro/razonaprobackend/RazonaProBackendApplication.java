package com.razonapro.razonaprobackend;

import com.razonapro.razonaprobackend.infrastructure.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync          // para EmailService.send() asíncrono
@EnableConfigurationProperties(AppProperties.class)
public class RazonaProBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(RazonaProBackendApplication.class, args);
    }
}
