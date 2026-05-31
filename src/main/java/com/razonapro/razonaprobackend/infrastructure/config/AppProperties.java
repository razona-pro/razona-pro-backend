package com.razonapro.razonaprobackend.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendUrl;
    private String mailFrom;

    private final AdminInitializer adminInitializer = new AdminInitializer();

    @Getter
    @Setter
    public static class AdminInitializer {
        private boolean enabled = true;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private String phone;
    }
}