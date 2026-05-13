package com.tacticaldistrict.command;

import com.tacticaldistrict.command.security.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = JwtProperties.class)
public class TacticalDistrictCommandApplication {

    public static void main(String[] args) {
        SpringApplication.run(TacticalDistrictCommandApplication.class, args);
    }
}

