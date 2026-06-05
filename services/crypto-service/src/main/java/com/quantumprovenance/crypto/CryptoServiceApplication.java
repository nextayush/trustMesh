package com.quantumprovenance.crypto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class CryptoServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CryptoServiceApplication.class, args);
    }
}
