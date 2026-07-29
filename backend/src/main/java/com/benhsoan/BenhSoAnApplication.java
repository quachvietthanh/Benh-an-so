package com.benhsoan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.benhsoan.config.AppointmentReminderProperties;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(AppointmentReminderProperties.class)
public class BenhSoAnApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenhSoAnApplication.class, args);
    }
}
