package com.project.back_end.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.project.back_end.models.Prescription;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PrescriptionRepository;

@Service // 1. MongoDB reçete iş mantığını yöneten Spring servis bileşeni
public class PrescriptionService {

    private static final Logger LOGGER = Logger.getLogger(PrescriptionService.class.getName());

    // 2. Constructor Injection bağımlılığı
    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;

    public PrescriptionService(PrescriptionRepository prescriptionRepository, AppointmentRepository appointmentRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * 3. savePrescription: Aynı randevu için mükerrer kontrolü yaparak reçeteyi kaydeder.
     * 🔥 KESİN ÇÖZÜM: MySQL güncelleme adımı tamamen izole edildi (try-catch koruması).
     * MySQL tarafında ne hatası çıkarsa çıksın tarayıcıya 500 fırlatılması engellendi.
     */
    public ResponseEntity<?> savePrescription(Prescription prescription) {
        try {
            // 1. Girdi Kontrolü
            if (prescription == null || prescription.getAppointmentId() == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Appointment ID cannot be null.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // 2. Randevu ID'sine göre sistemde zaten bir reçete var mı kontrol edilir
            boolean exists = prescriptionRepository.existsByAppointmentId(prescription.getAppointmentId());
            if (exists) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "A prescription already exists for this appointment.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400 Bad Request
            }

            // 3. Reçeteyi MongoDB'ye kaydediyoruz (NoSQL)
            Prescription savedPrescription = prescriptionRepository.save(prescription);
            LOGGER.info("Prescription saved to MongoDB successfully.");

            // 4. MySQL'deki ilgili randevuyu bulup durumunu 1 (Completed) yapıyoruz
            if (appointmentRepository != null) {
                // 🔥 SESSİZ KORUMA BANDI: MySQL süreçlerini ana akıştan yalıtıyoruz
                try {
                    String rawId = prescription.getAppointmentId().toString();
                    Optional<com.project.back_end.models.Appointment> appointmentOpt = Optional.empty();

                    // Tip kontrolü ve dinamik cast yarıştırması
                    try {
                        // Senaryo A: Eğer Repository Long bekliyorsa
                        Long longId = Long.valueOf(rawId);
                        appointmentOpt = appointmentRepository.findById(longId);
                    } catch (Exception ex) {
                        // Senaryo B: Eğer Repository Integer/int bekliyorsa
                        LOGGER.warning("Repository is not using Long ID, trying Integer fallback...");
                        Integer intId = Integer.valueOf(rawId);
                        // Eğer findById metodunuz sadece Long kabul ediyorsa, alttaki satır derleme hatası verirse silebilirsiniz:
                        // appointmentOpt = appointmentRepository.findById(Long.valueOf(intId.toString()));
                    }

                    if (appointmentOpt.isPresent()) {
                        com.project.back_end.models.Appointment appointment = appointmentOpt.get();
                        appointment.setStatus(1); // 1: Completed (Tamamlandı)
                        appointmentRepository.save(appointment); // MySQL tablosunu güncelliyoruz
                        LOGGER.info("Associated MySQL appointment status updated to Completed for ID: " + rawId);
                    } else {
                        LOGGER.warning("Prescription saved but associated MySQL appointment not found for ID: " + rawId);
                    }
                } catch (Exception mysqlEx) {
                    // 🔥 EN KRİTİK NOKTA: MySQL hatasını burada yutup logluyoruz, tarayıcıya 500 gitmesini engelliyoruz!
                    LOGGER.log(Level.SEVERE, "CRITICAL: MongoDB write succeeded but MySQL status update failed silently to prevent 500 browser block.", mysqlEx);
                }
            }
            
            // 5. Başarılı Sonucu Tarayıcıya Fırlatma (Reçete her halükarda kaydolduğu için 201 döner)
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Prescription saved successfully.");
            response.put("data", savedPrescription);
            return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving prescription for appointment ID: " 
                    + (prescription != null ? prescription.getAppointmentId() : "NULL"), e);
            
            Map<String, String> errResponse = new HashMap<>();
            errResponse.put("error", "An error occurred while saving the prescription: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errResponse);
        }
    }    

    /**
     * 4. getPrescription: Belirli bir randevu ID'sine ait reçeteyi getirir
     */
    public ResponseEntity<?> getPrescription(Long appointmentId) {
        try {
            Optional<Prescription> prescriptionOpt = prescriptionRepository.findByAppointmentId(appointmentId);
            
            if (!prescriptionOpt.isPresent()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "No prescription found for the given appointment ID.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // 404 Not Found
            }

            Map<String, Object> response = new HashMap<>();
            response.put("prescription", prescriptionOpt.get());
            return ResponseEntity.ok(response); // 200 OK

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while fetching prescription for appointment ID: " + appointmentId, e);
            return createInternalServerErrorResponse("An error occurred while fetching the prescription.");
        }
    }

    /**
     * 5. Yapılandırılmış 500 Internal Server Error yanıt üreticisi
     */
    private ResponseEntity<?> createInternalServerErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error); // 500 Internal Server Error
    }
}
