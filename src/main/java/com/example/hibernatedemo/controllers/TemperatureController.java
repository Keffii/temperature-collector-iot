package com.example.hibernatedemo.controllers;

import com.example.hibernatedemo.models.Temperature;
import com.example.hibernatedemo.repos.TemperatureRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/temperature")
public class TemperatureController {

    private final TemperatureRepository repository;

    public TemperatureController(TemperatureRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @ResponseBody
    public List<Temperature> getAllTemperatures() {
        return repository.findAll();
    }


    @GetMapping("/addTemperatureByHTML")
    public String addTemperatureForm() {
        return "addTemperatureByHTML.html";
    }


    @PostMapping("/addTemperatureByHTML")
    @ResponseBody
    public String addTemperatureByHtml(@RequestParam double value) {
        repository.save(new Temperature(value));
        return "Temperature added successfully";
    }

    @PostMapping("/add")
    @ResponseBody
    public Temperature addTemperature(@RequestBody Temperature temp) {
        return repository.save(temp);
    }
}
