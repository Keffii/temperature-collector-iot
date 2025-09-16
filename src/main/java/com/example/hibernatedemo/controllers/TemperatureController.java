package com.example.hibernatedemo.controllers;

import com.example.hibernatedemo.models.Temperature;
import com.example.hibernatedemo.repos.TemperatureRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/temperature")
public class TemperatureController {

    private final TemperatureRepository repository;

    public TemperatureController(TemperatureRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Temperature> getAllTemperatures() {
        return repository.findAll();
    }

    @PostMapping("/add")
    public Temperature addTemperature(@RequestBody Temperature temp) {
        return repository.save(temp);
    }
}
