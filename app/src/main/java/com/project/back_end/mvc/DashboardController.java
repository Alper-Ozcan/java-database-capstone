package com.project.back_end.mvc;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller // 1. HTML şablon adları (Thymeleaf, JSP vb.) dönen MVC kontrolcüsü
public class DashboardController {

    // 2. Ortak doğrulama mantığını barındıran Service bileşeni
    private final com.project.back_end.services.Service mainService;

    // Constructor Injection ile bağımlılık enjeksiyonu
    public DashboardController(com.project.back_end.services.Service mainService) {
        this.mainService = mainService;
    }

    // 3. adminDashboard: Admin paneline güvenli geçiş sağlar
    @GetMapping("/adminDashboard/{token}")
    public String adminDashboard(@PathVariable("token") String token) {
        // Shared Service içindeki validateToken metodunu çağırıyoruz
        ResponseEntity<?> response = mainService.validateToken(token, "admin");

        // Eğer durum kodu 200 OK ise token geçerlidir, admin görünümüne yönlendirilir
        if (response.getStatusCode() == HttpStatus.OK) {
            return "admin/adminDashboard";
        }

        // Token geçersiz veya hatalıysa kök dizine (login/home) yönlendirilir
        return "redirect:/";
    }

    // 4. doctorDashboard: Doktor paneline güvenli geçiş sağlar
    @GetMapping("/doctorDashboard/{token}")
    public String doctorDashboard(@PathVariable("token") String token) {
        // Shared Service içindeki validateToken metodunu doktor rolü için çağırıyoruz
        ResponseEntity<?> response = mainService.validateToken(token, "doctor");

        // Token geçerliyse doktor şablon görünümüne yönlendirilir
        if (response.getStatusCode() == HttpStatus.OK) {
            return "doctor/doctorDashboard";
        }

        // Token geçersizse kök dizine yönlendirilir
        return "redirect:/";
    }
}
