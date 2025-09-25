package com.example.hibernatedemo.controllers;

import com.example.hibernatedemo.models.SimpleGraphData;
import com.example.hibernatedemo.models.Temperature;
import com.example.hibernatedemo.repos.TemperatureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class RootController {
    @Autowired
    private TemperatureRepository temperatureRepository;

    private static final Logger log = LoggerFactory.getLogger(RootController.class);

    @GetMapping("/")
    public String index() {
        // Directly show temperature list at root
        return "redirect:/temperature/list";
    }

    @GetMapping("/graph")
    public String graph(Model model) {
        List<Temperature> temps = temperatureRepository.findAll();
        temps = temps.stream()
                .filter(t -> t != null && t.getTimestamp() != null)
                .collect(Collectors.toList());

        // Today
        LocalDate today = LocalDate.now();
        List<Temperature> todayTemps = temps.stream()
                .filter(t -> t.getTimestamp().toLocalDate().isEqual(today))
                .sorted(Comparator.comparing(Temperature::getTimestamp))
                .collect(Collectors.toList());
        List<String> todayLabels = todayTemps.stream()
                .map(t -> t.getTimestamp().toLocalTime().toString().substring(0,5))
                .collect(Collectors.toList());
        List<Double> todayValues = todayTemps.stream()
                .map(Temperature::getValue)
                .collect(Collectors.toList());
        model.addAttribute("todayGraph", new SimpleGraphData(todayLabels, todayValues));

        // This month (daily averages)
        int month = today.getMonthValue();
        int year = today.getYear();
        Map<Integer, List<Double>> dayToTemps = temps.stream()
                .filter(t -> t.getTimestamp().getYear() == year && t.getTimestamp().getMonthValue() == month)
                .collect(Collectors.groupingBy(t -> t.getTimestamp().getDayOfMonth(),
                        Collectors.mapping(Temperature::getValue, Collectors.toList())));
        List<String> monthLabels = dayToTemps.keySet().stream()
                .sorted()
                .map(d -> String.valueOf(d))
                .collect(Collectors.toList());
        List<Double> monthValues = dayToTemps.keySet().stream()
                .sorted()
                .map(d -> dayToTemps.get(d).stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN))
                .collect(Collectors.toList());
        model.addAttribute("monthGraph", new SimpleGraphData(monthLabels, monthValues));

        // This year (monthly averages)
        Map<Integer, List<Double>> monthToTemps = temps.stream()
                .filter(t -> t.getTimestamp().getYear() == year)
                .collect(Collectors.groupingBy(t -> t.getTimestamp().getMonthValue(),
                        Collectors.mapping(Temperature::getValue, Collectors.toList())));
        List<String> yearLabels = monthToTemps.keySet().stream()
                .sorted()
                .map(m -> java.time.Month.of(m).name().substring(0,3))
                .collect(Collectors.toList());
        List<Double> yearValues = monthToTemps.keySet().stream()
                .sorted()
                .map(m -> monthToTemps.get(m).stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN))
                .collect(Collectors.toList());
        model.addAttribute("yearGraph", new SimpleGraphData(yearLabels, yearValues));

        return "graph";
    }

    @GetMapping("/settings")
    public String settings() {
        return "settings";
    }

    @GetMapping("/api/latest-temp")
    public ResponseEntity<java.util.Map<String,Object>> latestTemp() {
        java.util.Map<String,Object> resp = new java.util.HashMap<>();
        try {
            Temperature t = temperatureRepository.findTopByOrderByTimestampDesc();
            if (t != null) {
                resp.put("value", t.getValue());
                resp.put("timestamp", t.getTimestamp() != null ? t.getTimestamp().toString() : null);
            } else {
                resp.put("value", null);
                resp.put("timestamp", null);
            }
            return ResponseEntity.ok(resp);
        } catch (Exception ex) {
            log.error("Failed to fetch latest temperature", ex);
            resp.put("value", null);
            resp.put("timestamp", null);
            resp.put("error", "fetch_failed");
            return ResponseEntity.status(500).body(resp);
        }
    }
}
