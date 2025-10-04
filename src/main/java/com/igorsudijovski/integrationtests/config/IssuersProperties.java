package com.igorsudijovski.integrationtests.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties("app.security")
public class IssuersProperties {
    private List<String> issuers;
    private List<String> jwkSetUris;
}
