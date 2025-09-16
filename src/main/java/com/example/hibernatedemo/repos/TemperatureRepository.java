package com.example.hibernatedemo.repos;

import com.example.hibernatedemo.models.Temperature;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemperatureRepository extends JpaRepository<Temperature, Long> {}
