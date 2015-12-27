package com.uracer.racer.config;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Configuration
public class SpringBeansConfig {

    @Bean
    public RestTemplate restTemplate() {

        RestTemplate restTemplate = new RestTemplate();
        HttpMessageConverter formHttpMessageConverter = new FormHttpMessageConverter();
        HttpMessageConverter stringHttpMessageConverternew = new StringHttpMessageConverter();
        MappingJackson2XmlHttpMessageConverter jackson2XmlHttpMessageConverter = new MappingJackson2XmlHttpMessageConverter();
        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        ByteArrayHttpMessageConverter converter = new ByteArrayHttpMessageConverter();
        converter.setSupportedMediaTypes(Arrays.asList(MediaType.ALL));
        restTemplate.setMessageConverters(Arrays.
                <HttpMessageConverter<?>>asList(formHttpMessageConverter, stringHttpMessageConverternew, jackson2XmlHttpMessageConverter, jackson2HttpMessageConverter, converter));
        return restTemplate;
    }

    @Bean
    public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {

        PropertyPlaceholderConfigurer configurer = new PropertyPlaceholderConfigurer();
        configurer.setLocation(new ClassPathResource("application.properties"));
        return configurer;
    }
}
