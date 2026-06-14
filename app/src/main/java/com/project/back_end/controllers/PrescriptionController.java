package com.project.back_end.controllers;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.models.Prescription;
import com.project.back_end.services.AppointmentService;
import com.project.back_end.services.PrescriptionService;

@RestController // 1. MongoDB reçete işlemleri için JSON yanıtları üreten REST kontrolcüsü
@RequestMapping("${api.path}prescription") // Properties dosyasından okunan esnek base path
public class PrescriptionController {

    private static final Logger LOGGER = Logger.getLogger(PrescriptionController.class.getName());

    // 2. Gerekli servis bağımlılıklarının tanımlanması
    private final PrescriptionService prescriptionService;
    private final com.project.back_end.services.Service mainService;
    private final AppointmentService appointmentService;

    // Temiz kod ve test edilebilirlik için Constructor Injection kullanımı
    public PrescriptionController(PrescriptionService prescriptionService,
                                  com.project.back_end.services.Service mainService,
                                  AppointmentService appointmentService) {
        this.prescriptionService = prescriptionService;
        this.mainService = mainService;
        this.appointmentService = appointmentService;
    }

    /**
     * 3. savePrescription: Doktorun yeni bir reçete kaydetmesini (POST) sağlar
     * 🔥 KESİN ÇÖZÜM: Mükerrer veritabanı kilitlenmesine yol açan ikinci appointmentService çağrısı kaldırıldı!
     * Durum güncelleme işi tamamen PrescriptionService içine devredildi.
     */
    @PostMapping("/{token}")
    public ResponseEntity<?> savePrescription(@RequestBody Prescription prescription,
                                             @PathVariable("token") String token) {
        try {
            // Token'ın "doctor" rolüne ait olup olmadığı ortak servis üzerinden doğrulanır
            ResponseEntity<?> tokenCheck = mainService.validateToken(token, "doctor");
            if (tokenCheck.getStatusCode() != HttpStatus.OK) {
                return tokenCheck; // Geçersizse 401 Unauthorized hata haritasını döner
            }

            // Güvenlik Duvarı: Girdi doğrulaması
            if (prescription == null || prescription.getAppointmentId() == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Appointment ID cannot be null.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Reçete servisi çağrılır (MongoDB yazma ve MySQL güncelleme işini tek hamlede halleder)
            ResponseEntity<?> saveResponse = prescriptionService.savePrescription(prescription);
            
            return saveResponse;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Controller level catch triggered for prescription POST block", e);
            Map<String, String> errResponse = new HashMap<>();
            errResponse.put("error", "An unexpected server error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errResponse);
        }
    }

    /**
     * 4. getPrescription: Belirli bir randevuya ait reçete detaylarını getirir (GET)
     */
    @GetMapping("/{appointmentId}/{token}")
    public ResponseEntity<?> getPrescription(@PathVariable("appointmentId") Long appointmentId,
                                             @PathVariable("token") String token) {
        try {
            // İstek atan kullanıcının "doctor" rolü doğrulanır
            ResponseEntity<?> tokenCheck = mainService.validateToken(token, "doctor");
            if (tokenCheck.getStatusCode() != HttpStatus.OK) {
                return tokenCheck;
            }

            // Doğrulama başarılıysa NoSQL katmanından reçete bilgileri çağrılır
            return prescriptionService.getPrescription(appointmentId);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Controller level catch triggered for prescription GET block", e);
            Map<String, String> errResponse = new HashMap<>();
            errResponse.put("error", "Failed to fetch prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errResponse);
        }
    }
}
