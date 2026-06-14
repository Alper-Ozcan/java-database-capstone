package com.project.back_end.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.models.Admin;

@RestController // 1. Web isteklerini karşılayan ve doğrudan JSON dönen REST kontrolcüsü
@RequestMapping("${api.path}admin") // `${api.path}` properties dosyasından okunarak esnek base path oluşturur
public class AdminController {

    // 2. Çekirdek iş mantığını barındıran Service bağımlılığı
    private final com.project.back_end.services.Service mainService;

    // Temiz kod ve test edilebilirlik için Constructor Injection kullanımı
    public AdminController(com.project.back_end.services.Service mainService) {
        this.mainService = mainService;
    }

    // 3. adminLogin: Admin giriş işlemlerini yürüten POST uç noktası (endpoint)
    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody Admin adminCredentials) {
        // İstek gövdesinden (RequestBody) gelen kullanıcı adı ve şifreyi servis katmanına iletiyoruz
        return mainService.validateAdmin(
                adminCredentials.getUsername(), 
                adminCredentials.getPassword()
        );
    }
}
