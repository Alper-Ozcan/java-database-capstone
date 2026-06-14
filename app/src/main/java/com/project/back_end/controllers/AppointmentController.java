package com.project.back_end.controllers;

import com.project.back_end.models.Appointment;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.TokenService; // 🔥 EKLENDİ
import com.project.back_end.repo.DoctorRepository; // 🔥 EKLENDİ
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController // 1. REST API isteklerini yöneten kontrolcü anotasyonu
@RequestMapping("/appointments") // Tüm randevu uç noktaları için temel yol (base path)
public class AppointmentController {

    // 2. Bağımlılıkların enjekte edilmesi
    private final AppointmentService appointmentService;
    private final com.project.back_end.services.Service mainService;
    private final TokenService tokenService; // 🔥 EKLENDİ
    private final DoctorRepository doctorRepository; // 🔥 EKLENDİ

    // Temiz mimari ve test edilebilirlik için Constructor Injection güncellendi
    public AppointmentController(AppointmentService appointmentService, 
                                 com.project.back_end.services.Service mainService,
                                 TokenService tokenService,
                                 DoctorRepository doctorRepository) {
        this.appointmentService = appointmentService;
        this.mainService = mainService;
        this.tokenService = tokenService;
        this.doctorRepository = doctorRepository;
    }

    // 3. getAppointments: Doktorun belirli bir tarihteki randevularını filtreleyerek getirir
    // 🔥 GÜVENLİK DÜZELTMESİ: Artık dışarıdan gelen doctorId parametresine güvenmiyoruz!
    @GetMapping("/{date}/{patientName}/{token}")
    public ResponseEntity<?> getAppointments(
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable("patientName") String patientName,
            @PathVariable("token") String token,
            @RequestParam(value = "doctorId", required = false) Long doctorId) { // Geriye uyumluluk için parametre durabilir ama ezilecek!

        // Token, "doctor" rolü için ana servis üzerinden doğrulanır
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, "doctor");
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck; // 401 Unauthorized hata haritasını doğrudan döner
        }

        // 🔥 SİZİN ÖNERDİĞİNİZ AKILLI VE GÜVENLİ ÇÖZÜM UYGULANIYOR:
        // 1. Token içinden güvenle doktor e-postasını söküyoruz
        String doctorEmail = tokenService.extractEmail(token);
        
        // 2. E-posta ile veritabanından (MySQL) o canlı doktor kaydını çekiyoruz
        Optional<com.project.back_end.models.Doctor> doctorOpt = doctorRepository.findByEmail(doctorEmail);
        
        if (!doctorOpt.isPresent()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Authenticated doctor profile not found in database.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // 3. Sistem tarafından doğrulanmış, dışarıdan müdahale edilemez gerçek Doktor ID'si:
        Long authenticatedDoctorId = doctorOpt.get().getId();

        // Esneklik kontrolü (Orijinal kodunuz)
        String filterName = "all".equalsIgnoreCase(patientName) ? "" : patientName;
        
        // 🔥 KRİTİK DEĞİŞİKLİK: Dışarıdan gelen şüpheli parametre (doctorId) yerine, 
        // Token'dan çözdüğümüz %100 güvenli ID'yi (authenticatedDoctorId) servise paslıyoruz!
        return ResponseEntity.ok(appointmentService.getAppointments(authenticatedDoctorId, date, filterName));
    }

    // 4. bookAppointment: Hastanın yeni bir randevu kaydetmesini (POST) sağlar
    @PostMapping("/{token}")
    public ResponseEntity<?> bookAppointment(@RequestBody Appointment appointment, 
                                             @PathVariable("token") String token) {
        
        // Token, "patient" rolü için doğrulanır
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, "patient");
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck;
        }

        int result = appointmentService.bookAppointment(appointment);
        
        Map<String, String> response = new HashMap<>();
        if (result == 1) {
            response.put("message", "Appointment booked successfully.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            response.put("error", "Failed to book appointment. Slot is already taken or doctor is invalid.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // 5. updateAppointment: Var olan bir randevunun zamanını günceller (PUT)
    @PutMapping("/{token}")
    public ResponseEntity<?> updateAppointment(@RequestBody Appointment appointment, 
                                               @PathVariable("token") String token) {
        
        // Token, "patient" rolü için doğrulanır
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, "patient");
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck;
        }

        String result = appointmentService.updateAppointment(
                appointment.getId(), 
                appointment.getPatient().getId(), 
                appointment.getAppointmentTime()
        );

        Map<String, String> response = new HashMap<>();
        if ("Success".equalsIgnoreCase(result)) {
            response.put("message", "Appointment updated successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", result);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    // 6. cancelAppointment: Hastanın kendi randevusunu iptal etmesini (DELETE) sağlar
    @DeleteMapping("/{appointmentId}/{patientId}/{token}")
    public ResponseEntity<?> cancelAppointment(@PathVariable("appointmentId") Long appointmentId,
                                               @PathVariable("patientId") Long patientId,
                                               @PathVariable("token") String token) {
        
        // Token, "patient" rolü için doğrulanır
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, "patient");
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck;
        }

        String result = appointmentService.cancelAppointment(appointmentId, patientId);

        Map<String, String> response = new HashMap<>();
        if ("Success".equalsIgnoreCase(result)) {
            response.put("message", "Appointment cancelled successfully.");
            return ResponseEntity.ok(response);
        } else {
            response.put("error", result);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}
