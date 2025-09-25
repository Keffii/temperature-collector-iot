package com.example.hibernatedemo;

import com.example.hibernatedemo.models.Temperature;
import com.example.hibernatedemo.repos.TemperatureRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortTimeoutException;
import java.io.InputStream;
import java.io.IOException;


@SpringBootApplication
public class TemperatureDemoApplication {

    public static void main(String[] args) {SpringApplication.run(TemperatureDemoApplication.class, args);}

    private int parseIntProp(Environment env, String key, int defaultVal) {
        String raw = env.getProperty(key, String.valueOf(defaultVal));
        if (raw == null) return defaultVal;
        // Strip inline comments (# or //) and keep first numeric token
        raw = raw.split("#",2)[0].split("//",2)[0].trim();
        if (raw.isEmpty()) return defaultVal;
        // Keep only leading numeric pattern (optional sign + digits)
        raw = raw.split("\\s+")[0];
        try { return Integer.parseInt(raw); } catch (Exception e) {
            System.err.println("Could not parse int property '" + key + "' from value '" + env.getProperty(key) + "' - using default " + defaultVal);
            return defaultVal;
        }
    }

    private long parseLongProp(Environment env, String key, long defaultVal) {
        String raw = env.getProperty(key, String.valueOf(defaultVal));
        if (raw == null) return defaultVal;
        raw = raw.split("#",2)[0].split("//",2)[0].trim();
        if (raw.isEmpty()) return defaultVal;
        raw = raw.split("\\s+")[0];
        try { return Long.parseLong(raw); } catch (Exception e) {
            System.err.println("Could not parse long property '" + key + "' from value '" + env.getProperty(key) + "' - using default " + defaultVal);
            return defaultVal;
        }
    }

    @Bean
    public CommandLineRunner demoTeperature(TemperatureRepository tempRepo, Environment env) {
        return args -> {
            // Read from application.properties with sensible defaults
            final String PORT_NAME = env.getProperty("app.serial.port", "COM3");
            final int BAUD = parseIntProp(env, "app.serial.baud", 9600);
            final long STARTUP_DELAY_MS = parseLongProp(env, "app.serial.startupDelayMs", 2000L); // allow MCU reset

            System.out.println("=== Serial Port Startup ===");
            System.out.println("Configured port: " + PORT_NAME + ", baud: " + BAUD + ", startupDelayMs: " + STARTUP_DELAY_MS);
            System.out.println("Enumerating available ports:");
            SerialPort[] ports = SerialPort.getCommPorts();
            boolean found = false;
            for (SerialPort p : ports) {
                String desc = p.getDescriptivePortName();
                System.out.println(" - " + p.getSystemPortName() + " (" + desc + ")");
                if (p.getSystemPortName().equalsIgnoreCase(PORT_NAME)) {
                    found = true;
                }
            }
            if (!found) {
                System.err.println("WARNING: Configured port '" + PORT_NAME + "' not found in the enumerated list above. Check Device Manager / dmesg.");
            }

            SerialPort port = SerialPort.getCommPort(PORT_NAME);
            port.setBaudRate(BAUD);
            port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);

            if (!port.openPort()) {
                System.err.println("Could not open serial port: " + PORT_NAME);
                System.err.println("Possible causes: already in use (close Arduino Serial Monitor), wrong port name, insufficient permissions.");
                return;
            }
            System.out.println("Listening on " + PORT_NAME + " @ " + BAUD + " (CTRL+C to exit)");
            System.out.println("Waiting for data... from Arduino sketch calls Serial.begin(" + BAUD + ") and uses println().");
            System.out.println("Applying startup delay (" + STARTUP_DELAY_MS + " ms) to allow Arduino reset...");

            Thread t = new Thread(() -> {
                try {
                    try { Thread.sleep(STARTUP_DELAY_MS); } catch (InterruptedException ignored) {}

                    // Clear any reset noise
                    InputStream is = port.getInputStream();
                    try {
                        while (port.bytesAvailable() > 0) {
                            if (is.read() == -1) break; // end of stream
                        }
                    } catch (IOException ioex) {
                        System.err.println("Ignoring exception while clearing reset noise: " + ioex.getMessage());
                    }

                    StringBuilder current = new StringBuilder();
                    long lastLog = System.currentTimeMillis();
                    long lastDataMillis = System.currentTimeMillis();
                    byte[] buf = new byte[128];

                    while (true) {
                        try {
                            int available = port.bytesAvailable();
                            if (available < 0) {
                                System.err.println("Port reported negative available bytes (device unplugged?). Exiting reader thread.");
                                break;
                            }
                            if (available == 0) {
                                long now = System.currentTimeMillis();
                                if (now - lastDataMillis > 5000 && now - lastLog > 5000) {
                                    System.out.println("(Still listening - no data bytes received in last 5s)");
                                    lastLog = now;
                                }
                                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                                continue;
                            }
                            if (available > buf.length) available = buf.length;
                            int read = is.read(buf, 0, available);
                            if (read <= 0) {
                                // Non-blocking read may return 0
                                continue;
                            }
                            lastDataMillis = System.currentTimeMillis();
                            for (int i = 0; i < read; i++) {
                                char c = (char) (buf[i] & 0xFF);
                                if (c == '\r') {
                                    continue; // normalize CRLF
                                }
                                if (c == '\n') {
                                    String line = current.toString().trim();
                                    current.setLength(0);
                                    if (line.isEmpty()) continue;
                                    // Process line
                                    try {
                                        // Extract first numeric token (handles lines like 'Temp: 23.4 C')
                                        String cleaned = line.replaceAll("[^0-9+-.eE]", " ").trim();
                                        if (cleaned.isEmpty()) {
                                            System.err.println("No numeric token found in line: '" + line + "'");
                                            continue;
                                        }
                                        String[] tokens = cleaned.split("\\s+");
                                        if (tokens.length == 0 || tokens[0].isBlank()) {
                                            System.err.println("No numeric token found in line: '" + line + "'");
                                            continue;
                                        }
                                        double val = Double.parseDouble(tokens[0]);
                                        tempRepo.save(new Temperature(val));
                                        System.out.println("Saved temperature: " + val + " C");
                                    } catch (Exception nfe) {
                                        System.err.println("Could not parse numeric value from line: '" + line + "'");
                                    }
                                } else {
                                    current.append(c);
                                    if (current.length() > 128) { // sanity limit bigger now that we parse tokens
                                        System.err.println("Discarding overlong line (>128 chars): " + current);
                                        current.setLength(0);
                                    }
                                }
                            }
                        } catch (SerialPortTimeoutException ste) {
                            // With NONBLOCKING this shouldn't normally fire, but just in case: continue loop
                            continue;
                        } catch (Exception ex) {
                            System.err.println("Serial read loop exception: " + ex.getMessage());
                            ex.printStackTrace();
                            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                        }
                    }
                } finally {
                    try {
                        port.closePort();
                        System.out.println("Serial port closed.");
                    } catch (Exception ignore) {}
                }
            }, "serial-listener");

            t.setDaemon(true);
            t.start();
        };
    }
}
