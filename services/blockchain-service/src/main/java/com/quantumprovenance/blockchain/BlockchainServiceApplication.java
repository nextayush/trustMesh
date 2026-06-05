package com.quantumprovenance.blockchain;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class BlockchainServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlockchainServiceApplication.class, args);
    }
}
