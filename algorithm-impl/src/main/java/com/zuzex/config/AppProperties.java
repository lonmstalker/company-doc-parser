package com.zuzex.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Getter
@ConfigurationProperties(prefix = "app.properties")
@AllArgsConstructor(onConstructor__ = @ConstructorBinding)
public class AppProperties {
    private final String archiveFolder;
}
