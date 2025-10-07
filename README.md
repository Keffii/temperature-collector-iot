# Temperature Collector IoT

Spring Boot application that collects temperature telemetry from an IoT device,  
persists readings to a relational database, and visualizes the data using web pages and endpoints (list, graph).

---

## Group project made by:
- [Babak](https://github.com/6a6ak)  
- [Kevin](https://github.com/Keffii)  
- [Viktor](https://github.com/vbood)  

---

## Contents
- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Web UI](#web-ui)
- [Features & Usage](#features--usage)



---

## Overview
1. Sensor or client **POSTs temperature readings** to the service (or a user submits via the HTML form).  
2. Readings are **stored in a relational SQL database**.  
3. **Web UI pages** provide lists and graphs of stored temperature data.  

---

## Tech Stack
- **Language:** Java (Spring Boot)  
- **Web / Templating:** Thymeleaf
- **ORM:** Hibernate / Spring Data JPA  
- **Database:** MySQL 
- **Build:** Maven
- **Frontend:** HTML/CSS (Thymeleaf templates with **bootstrap**) 

---

## Prerequisites
- **Java JDK** (11+ recommended, match the project’s `pom.xml`)  
- **MySQL server** (or your preferred SQL DB)  
- *(Optional)* **Maven** if you want to run without the included wrapper  

---

## Web UI

**Temperature Monitor Dashboard** 
<img width="807" height="432" alt="Image" src="https://github.com/user-attachments/assets/5d085231-975a-4661-81fb-1173d627a7ff" />

---

## Features & Usage

### Graph readings over time
- View temperature data as interactive graphs directly in the web UI  
- Supports **different time ranges**:
  - **Day view** – see detailed temperature fluctuations throughout a single day  
  - **Month view** – analyze daily averages and trends across the month  
  - **Year view** – get a high-level overview of yearly trends  
- Includes a **live readings panel** that shows the most recent sensor data in real time  

### Submit readings
- REST endpoint: `POST /readings` (accepts JSON payloads from sensors)  
- HTML form in the web UI 

### Database persistence
- Uses Hibernate / Spring Data JPA to map readings to SQL tables  
