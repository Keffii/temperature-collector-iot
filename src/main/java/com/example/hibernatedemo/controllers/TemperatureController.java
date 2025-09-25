package com.example.hibernatedemo.controllers;

import com.example.hibernatedemo.models.Temperature;
import com.example.hibernatedemo.repos.TemperatureRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.time.format.TextStyle;
import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.Locale;

@Controller
@RequestMapping("/temperature")
public class TemperatureController {

    private final TemperatureRepository repository;

    public TemperatureController(TemperatureRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/list")
    public String listTemperatures(@RequestParam(name = "group", defaultValue = "day") String group,
                                   @RequestParam(name = "year", required = false) Integer year,
                                   @RequestParam(name = "month", required = false) Integer month,
                                   Model model) {
        List<Temperature> allTemps = repository.findAll();
        if (allTemps == null) allTemps = Collections.emptyList();
        List<Temperature> validTemps = allTemps.stream()
            .filter(t -> t != null && t.getTimestamp() != null)
            .collect(Collectors.toList());

        // Day tab: last 10 records per day (as before)
        Comparator<Temperature> byTimestampDesc = Comparator.comparing(Temperature::getTimestamp).reversed();
        java.util.function.Function<Temperature, String> fiveMinKey = t -> {
            var ts = t.getTimestamp();
            return ts.toLocalDate() + " " + ts.getHour() + ":" + (ts.getMinute() / 5 * 5);
        };
        Map<LocalDate, List<Temperature>> last10DayGroups = new TreeMap<>(Comparator.reverseOrder());
        validTemps.stream()
            .collect(Collectors.groupingBy(t -> t.getTimestamp().toLocalDate()))
            .forEach((date, temps) -> {
                List<Temperature> filtered = temps.stream()
                    .collect(Collectors.groupingBy(fiveMinKey, Collectors.maxBy(byTimestampDesc)))
                    .values().stream()
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted(byTimestampDesc)
                    .limit(10)
                    .collect(Collectors.toList());
                last10DayGroups.put(date, filtered);
            });

        // Month tab: daily averages for all available months/years
        Map<String, Map<Integer, Double>> monthDayAverages = new LinkedHashMap<>(); // "YYYY-MM" -> (day -> avg)
        validTemps.stream()
            .collect(Collectors.groupingBy(t -> String.format("%04d-%02d", t.getTimestamp().getYear(), t.getTimestamp().getMonthValue())))
            .forEach((ym, temps) -> {
                Map<Integer, List<Double>> dayToTemps = temps.stream()
                    .collect(Collectors.groupingBy(t -> t.getTimestamp().getDayOfMonth(),
                            Collectors.mapping(Temperature::getValue, Collectors.toList())));
                Map<Integer, Double> dayAvg = new LinkedHashMap<>();
                dayToTemps.forEach((day, tlist) -> {
                    double avg = tlist.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                    dayAvg.put(day, avg);
                });
                monthDayAverages.put(ym, dayAvg);
            });
        model.addAttribute("monthDayAverages", monthDayAverages);

        // Year tab: monthly averages for all available years
        Map<Integer, Map<Integer, Double>> yearMonthAverages = new LinkedHashMap<>(); // year -> (month -> avg)
        validTemps.stream()
            .collect(Collectors.groupingBy(t -> t.getTimestamp().getYear()))
            .forEach((yr, temps) -> {
                Map<Integer, List<Double>> monthToTemps = temps.stream()
                    .collect(Collectors.groupingBy(t -> t.getTimestamp().getMonthValue(),
                            Collectors.mapping(Temperature::getValue, Collectors.toList())));
                Map<Integer, Double> monthAvg = new LinkedHashMap<>();
                monthToTemps.forEach((monthNum, tlist) -> {
                    double avg = tlist.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
                    monthAvg.put(monthNum, avg);
                });
                yearMonthAverages.put(yr, monthAvg);
            });
        model.addAttribute("yearMonthAverages", yearMonthAverages);

        model.addAttribute("last10DayGroups", last10DayGroups);
        model.addAttribute("group", group);
        return "temperatureList";
    }
}
