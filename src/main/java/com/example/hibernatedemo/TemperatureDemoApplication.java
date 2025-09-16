package com.example.hibernatedemo;

import com.example.hibernatedemo.models.Temperature;
import com.example.hibernatedemo.repos.TemperatureRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TemperatureDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemperatureDemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner demoTemperature(TemperatureRepository tempRepo) {
        return (args) -> {
            // Creates temperature values for the temperature table
            tempRepo.save(new Temperature(22.5));
            tempRepo.save(new Temperature(23.0));
            tempRepo.save(new Temperature(21.8));
        };
    }
}
