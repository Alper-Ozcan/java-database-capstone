package com.project.back_end.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.DTO.AppointmentDTO;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.PatientRepository;

@Service // 1. Spring-managed servis bileşeni anotasyonu
public class PatientService {

    // 9. Loglama mekanizması için logger tanımı
    private static final Logger LOGGER = Logger.getLogger(PatientService.class.getName());

    // 2. Constructor Injection için bağımlılıklar
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public PatientService(PatientRepository patientRepository, 
                          AppointmentRepository appointmentRepository, 
                          TokenService tokenService) {
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 3. createPatient: Yeni hasta kaydeder, hata durumunda loglama yapar
    @Transactional
    public int createPatient(Patient patient) {
        try {
            patientRepository.save(patient);
            return 1; // Başarılı kayıt
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while creating patient: " + e.getMessage(), e);
            return 0; // Başarısız kayıt
        }
    }

    // 4 & 10. getPatientAppointment: Hastanın tüm randevularını getirip DTO'ya dönüştürür
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPatientAppointment(Long patientId) {
        try {
            List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
            List<AppointmentDTO> dtoList = appointments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error fetching appointments for patient ID " + patientId, e);
            return createErrorResponse("An error occurred while fetching appointments.");
        }
    }

    // 5. filterByCondition: Geçmiş (status: 1) veya Gelecek (status: 0) randevuları filtreler
    @Transactional(readOnly = true)
    public ResponseEntity<?> filterByCondition(Long patientId, String condition) {
        try {
            int status;
            if ("future".equalsIgnoreCase(condition)) {
                status = 0;
            } else if ("past".equalsIgnoreCase(condition)) {
                status = 1;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorMap("Invalid condition. Use 'past' or 'future'."));
            }

            List<Appointment> appointments = appointmentRepository.findByPatientIdAndStatus(patientId, status);
            List<AppointmentDTO> dtoList = appointments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error filtering appointments by condition: " + condition, e);
            return createErrorResponse("An error occurred while filtering appointments by condition.");
        }
    }

    // 6. filterByDoctor: Doktor adına göre randevuları filtreler
    @Transactional(readOnly = true)
    public ResponseEntity<?> filterByDoctor(Long patientId, String doctorName) {
        try {
            List<Appointment> appointments = appointmentRepository.findByPatientIdAndDoctorNameContainingIgnoreCase(patientId, doctorName);
            List<AppointmentDTO> dtoList = appointments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error filtering appointments by doctor name: " + doctorName, e);
            return createErrorResponse("An error occurred while filtering appointments by doctor name.");
        }
    }

    // 7. filterByDoctorAndCondition: Hem doktor adına hem de duruma (past/future) göre filtreler
    @Transactional(readOnly = true)
    public ResponseEntity<?> filterByDoctorAndCondition(Long patientId, String doctorName, String condition) {
        try {
            int status;
            if ("future".equalsIgnoreCase(condition)) {
                status = 0;
            } else if ("past".equalsIgnoreCase(condition)) {
                status = 1;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorMap("Invalid condition. Use 'past' or 'future'."));
            }

            List<Appointment> appointments = appointmentRepository
                    .findByPatientIdAndStatusAndDoctorNameContainingIgnoreCase(patientId, status, doctorName);
            List<AppointmentDTO> dtoList = appointments.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtoList);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error filtering by doctor and condition", e);
            return createErrorResponse("An error occurred during combined filtering.");
        }
    }

    // 8. getPatientDetails: Token'dan e-posta çıkararak hasta detaylarını getirir
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPatientDetails(String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            String email = tokenService.extractEmail(token);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorMap("Invalid token."));
            }

            Optional<Patient> patientOpt = patientRepository.findByEmail(email);
            if (!patientOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorMap("Patient profile not found."));
            }

            return ResponseEntity.ok(patientOpt.get());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error extracting or fetching patient details from token", e);
            return createErrorResponse("An error occurred while fetching patient details.");
        }
    }

    // 10. Entity'den DTO'ya dönüşüm sağlayan özel yardımcı metot
    private AppointmentDTO convertToDTO(Appointment appointment) {
        return new AppointmentDTO(
                appointment.getId(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getName(),
                appointment.getPatient().getId(),
                appointment.getPatient().getName(),
                appointment.getPatient().getEmail(),
                appointment.getPatient().getPhone(),
                appointment.getPatient().getAddress(),
                appointment.getAppointmentTime(),
                appointment.getStatus()
        );
    }

    // Konsolide edilmiş hata haritası üreticisi
    private Map<String, String> createErrorMap(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    // 500 Internal Server hatası üreticisi
    private ResponseEntity<?> createErrorResponse(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(createErrorMap(message));
    }
}
