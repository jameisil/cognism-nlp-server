package com.cognism.sentiment;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages = {"com"}, excludeFilters = {
    @ComponentScan.Filter(Configuration.class)})
@EnableMBeanExport
@EnableTransactionManagement
public class CognitiveServer {

    @Bean
    public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
        PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
        ppc.setLocations(new Resource[]{
            new ClassPathResource("log4j.properties")
        });
        ppc.setIgnoreUnresolvablePlaceholders(false);
        ppc.setIgnoreResourceNotFound(false);
        return ppc;
    }

//    public static void main(String[] args) {
//        try {
//            SpringApplication.run(SentimentExtractor.class, args);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
}
