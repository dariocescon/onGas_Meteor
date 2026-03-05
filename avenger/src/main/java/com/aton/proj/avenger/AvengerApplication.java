package com.aton.proj.avenger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.aton.proj.avenger.config.AvengerProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(AvengerProperties.class)
public class AvengerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AvengerApplication.class, args);
    }
}
