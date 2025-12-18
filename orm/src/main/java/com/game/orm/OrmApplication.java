package com.game.orm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrmApplication.class, args);
    }

}
