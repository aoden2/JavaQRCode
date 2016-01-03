package com.uracer.racer.config;

import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

@Configuration
@ComponentScan(basePackages = "com.uracer.racer")
@PropertySource("classpath:application.properties")
public class SpringBeansConfig {

    @Bean
    public FTPClient ftpClient() throws IOException {
        return new FTPClient();
    }
}
