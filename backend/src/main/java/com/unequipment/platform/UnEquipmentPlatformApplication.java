package com.unequipment.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UnEquipmentPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnEquipmentPlatformApplication.class, args);
    }
}
