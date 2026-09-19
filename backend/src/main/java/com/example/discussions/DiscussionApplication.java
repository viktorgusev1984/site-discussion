package com.example.discussions;
import com.example.discussions.service.notification.NotificationProperties;
import org.springframework.boot.SpringApplication;import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication @EnableScheduling @EnableConfigurationProperties(NotificationProperties.class)
public class DiscussionApplication {public static void main(String[] args){SpringApplication.run(DiscussionApplication.class,args);}}
