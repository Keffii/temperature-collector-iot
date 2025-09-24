package com.example.hibernatedemo;

import com.example.hibernatedemo.models.Temperature;
import com.example.hibernatedemo.repos.TemperatureRepository;
import com.fazecast.jSerialComm.SerialPort;

import java.io.BufferedReader;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.fazecast.jSerialComm.SerialPort;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


@SpringBootApplication
public class TemperatureDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemperatureDemoApplication.class, args);
    }

    @Bean
    public CommandLineRunner demoTemperature(TemperatureRepository tempRepo) {
        return (args) -> {
            final String PORT_NAME = "COM3";
            final int BAUD = 115200;

            SerialPort port = SerialPort.getCommPort(PORT_NAME);
            port.setBaudRate(BAUD);
            port.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);


            if (!port.openPort()){
                System.err.println("Fel inträffade kunde inte öppnar " + PORT_NAME);
                return;
            }
            System.out.println("Öppnat och lyssnar på " + PORT_NAME);
            // logiken och för att testa läsa esp32 serial och spara värde till db
            Thread t = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(port.getInputStream(), StandardCharsets.UTF_8))){
                    String line;
                    while ((line = br.readLine()) != null){
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        try {
                            double val = Double.parseDouble(line);
                            tempRepo.save(new Temperature(val));
                            System.out.println("Sparat " + val + " C");
                        }
                        catch (Exception ex) {
                            System.err.println("Felser");
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally{
                    try {
                    port.closePort();
                     } 
                    catch (Exception ignore){}
                        
                }

            }, "Thread-lyssnare");
            t.setDaemon(true);
            t.start();
        };
    }
}
