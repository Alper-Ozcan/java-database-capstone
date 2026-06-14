package com.project.back_end.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Patient;
import com.project.back_end.services.PatientService;

@RestController // 1. Hasta işlemlerine ait JSON yanıtları üreten REST kontrolcüsü
@RequestMapping("/patient") // Tüm hasta uç noktaları için ortak temel yol (base path)
public class PatientController {

    // 2. Gerekli servis bağımlılıklarının tanımlanması
    private final PatientService patientService;
    private final com.project.back_end.services.Service mainService;

    // Bağımlılıkların temiz enjeksiyonu için Constructor Injection
    public PatientController(PatientService patientService, com.project.back_end.services.Service mainService) {
        this.patientService = patientService;
        this.mainService = mainService;
    }

    // 3. getPatient: Token kullanarak giriş yapan hastanın profil detaylarını getirir (GET)
    @GetMapping("/{token}")
    public ResponseEntity<?> getPatient(@PathVariable("token") String token) {
        // Token'ın "patient" rolüne ait olup olmadığı doğrulanır
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, "patient");
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck; // Geçersizse 401 Unauthorized hata gövdesini doğrudan fırlatır
        }

        // Token geçerliyse hasta bilgileri getirilir
        return patientService.getPatientDetails(token);
    }

    // 4. createPatient: Yeni hasta kayıt (registrasyon) işlemini yürütür (POST)
    @PostMapping
    public ResponseEntity<?> createPatient(@RequestBody Patient patient) {
        // Ortak servis üzerinden e-posta veya telefon numarasının benzersizliği kontrol edilir
        boolean isUnique = mainService.validatePatient(patient.getEmail(), patient.getPhone());
        
        Map<String, String> response = new HashMap<>();
        if (!isUnique) {
            response.put("error", "Validation failed: A patient with this email or phone number already exists.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response); // 409 Conflict
        }

        // Doğrulama başarılıysa hasta veri tabanına kaydedilir
        int result = patientService.createPatient(patient);
        if (result == 1) {
            response.put("message", "Patient registered successfully.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created
        } else {
            response.put("error", "An internal server error occurred during registration.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500
        }
    }

    // 5. login: Hasta giriş işlemlerini ve kimlik doğrulamasını yapar (POST)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Login loginDto) {
        // Kimlik doğrulama ve JWT üretim süreci ortak servise (Service) devredilir
        return mainService.validatePatientLogin(loginDto.getEmail(), loginDto.getPassword());
    }

    // 6. getPatientAppointment: Belirli bir hastaya ait randevuları listeler (GET)
    @GetMapping("/appointments/{patientId}/{user}/{token}")
    public ResponseEntity<?> getPatientAppointment(@PathVariable("patientId") Long patientId,
                                                   @PathVariable("user") String userRole,
                                                   @PathVariable("token") String token) {
        // İsteği atan kullanıcının rolüne göre token doğrulanır (Hasta kendisi veya doktor bakabilir)
        if ("loggedpatient".equalsIgnoreCase(userRole)) {
            userRole = "patient";
        }
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, userRole);
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck;
        }

        // Token geçerliyse randevular listelenir (Dönüşüm DTO olarak PatientService içinde tamamlanır)
        return patientService.getPatientAppointment(patientId);
    }

    // 7. filterPatientAppointment: Hastanın randevu geçmişini doktor adına veya duruma göre filtreler (GET)
    @GetMapping("/appointments/filter/{condition}/{name}/{token}")
    public ResponseEntity<?> filterPatientAppointment(@PathVariable("condition") String condition,
                                                      @PathVariable("name") String name,
                                                      @PathVariable("token") String token) {
        // İşlemin sadece "patient" rolü tarafından yapılabileceği kontrol edilir
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, "patient");
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck;
        }

        // "all" veya boş geçilen filtre parametreleri esnek sorgu için temizlenir
        String filterCondition = "all".equalsIgnoreCase(condition) ? null : condition;
        String filterName = "all".equalsIgnoreCase(name) ? null : name;

        // Filtreleme senaryolarına göre PatientService üzerindeki ilgili metotlar tetiklenir
        // Çıkartılan email bilgisi üzerinden patientId bulunarak ilgili filtreye yönlendirme yapılır
        if (filterCondition != null && filterName != null) {
            // Hem doktor ismi hem durum (past/future) aktifse
            return mainService.validateToken(token, "patient").getStatusCode() == HttpStatus.OK ?
                   ResponseEntity.ok(mainService.filterPatient(token, filterCondition, filterName)) : tokenCheck;
        }

        // Not: filterPatient metodu halihazırda Service.java (satır 122) içinde 
        // token'ı, durum dizesini ve doktor ismini alacak şekilde kurgulandığından doğrudan oraya delege edilir.
        return ResponseEntity.ok(mainService.filterPatient(token, filterCondition, filterName));
    }
}
