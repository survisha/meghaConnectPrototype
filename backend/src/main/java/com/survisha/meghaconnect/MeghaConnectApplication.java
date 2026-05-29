package com.survisha.meghaconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.survisha.meghaconnect", "com.survisha.common"})
@EnableScheduling
public class MeghaConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeghaConnectApplication.class, args);
    }
}
