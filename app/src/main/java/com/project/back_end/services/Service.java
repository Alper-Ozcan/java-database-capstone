package com.project.back_end.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.models.Admin;
import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.models.Patient;
import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

@org.springframework.stereotype.Service // 1. Spring tarafından otomatik taranan ve yönetilen servis bileşeni
public class Service {

    // 2. Bağımlılıkların Tanımlanması
    private final TokenService tokenService;
    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorService doctorService;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;

    // Constructor Injection (Gevşek bağlılık ve test edilebilirlik için)
    public Service(TokenService tokenService,
            AdminRepository adminRepository,
            DoctorRepository doctorRepository,
            DoctorService doctorService,
            PatientRepository patientRepository,
            AppointmentRepository appointmentRepository) {
        this.tokenService = tokenService;
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.doctorService = doctorService;
        this.patientRepository = patientRepository;
        this.appointmentRepository = appointmentRepository;
    }

    // 3. validateToken Metodu: JWT token doğrulaması yapar, geçersizse 401 döner
    public ResponseEntity<?> validateToken(String token, String role) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer " kısmını temizler
        }

        boolean isValid = tokenService.validateToken(token, role);
        if (!isValid) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unauthorized access - Invalid or expired token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        return ResponseEntity.ok().build(); // Token geçerliyse 200 OK döner
    }

    // 4. validateAdmin Method: Admin giriş bilgilerini ve şifresini doğrular
    public ResponseEntity<?> validateAdmin(String username, String password) {
        try {
            Optional<Admin> adminOpt = adminRepository.findByUsername(username);

            if (adminOpt.isPresent()) {
                Admin admin = adminOpt.get();
                // NOT: Gerçek projede şifre karşılaştırması encoder ile yapılmalıdır (Örn: passwordEncoder.matches)
                if (admin.getPassword().equals(password)) {
                    String token = tokenService.generateToken(username);
                    Map<String, String> response = new HashMap<>();
                    response.put("token", token);
                    return ResponseEntity.ok(response); // 200 OK
                }
            }
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error); // 401 Unauthorized

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error); // 500 Internal Server Error
        }
    }

    // 5. filterDoctor Method: İsim, uzmanlık ve müsaitlik zamanına göre doktorları filtreler
    public List<Doctor> filterDoctor(String name, String specialty, String time) {
        List<Doctor> allDoctors = doctorService.getDoctors();
        if (name != null) {
            allDoctors = doctorService.filterDoctorByName(allDoctors, name);
        } 
        if (specialty != null) {
            allDoctors = doctorService.filterDoctorBySpecialty(allDoctors, specialty);
        }
        if (time != null) {
            allDoctors = doctorService.filterDoctorByTime(allDoctors, time);
        }
        return allDoctors;
    }

    // 6. validateAppointment Method: Doktorun randevu için müsaitliğini doğrular
    public int validateAppointment(Long doctorId, LocalDateTime requestedTime) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (!doctorOpt.isPresent()) {
            return -1; // Doktor sistemde yoksa -1 döner
        }

        Doctor doctor = doctorOpt.get();
        // İstenen saati string formatına dönüştürüp doktorun çalışma saatleriyle eşleştiriyoruz (Örn: "09:00")
        String requestedTimeStr = requestedTime.toLocalTime().toString();

        boolean isTimeSlotAvailable = false;
        for (String slot : doctor.getAvailableTimes()) {
            // "09:00-10:00" gibi bir slottan başlangıç saatini "09:00" olarak alıp kıyaslıyoruz
            String startTime = slot.split("-")[0].trim();
            if (startTime.equals(requestedTimeStr)) {
                isTimeSlotAvailable = true;
                break;
            }
        }

        if (!isTimeSlotAvailable) {
            return 0; // Doktor o saatte çalışmıyorsa veya slot uyumsuzsa 0 döner
        }

        // Seçilen saatte başka bir hastanın randevusu var mı kontrolü (Çakışma önleme)
        boolean isOccupied = appointmentRepository.existsByDoctorIdAndAppointmentTime(doctorId, requestedTime);
        if (isOccupied) {
            return 0; // Saat doluysa 0 döner
        }

        return 1; // Her şey geçerliyse 1 döner
    }

    // 7. validatePatient Method: Yeni kayıtta e-posta veya telefonun benzersizliğini denetler
    public boolean validatePatient(String email, String phone) {
        boolean emailExists = patientRepository.existsByEmail(email);
        boolean phoneExists = patientRepository.existsByPhone(phone);

        // Eğer ikisi de sistemde yoksa yeni kayıt için GEÇERLİDİR (true), varsa (false) döner
        return !emailExists && !phoneExists;
    }

    // 8. validatePatientLogin Method: Hasta giriş işlemlerini ve şifresini doğrular
    public ResponseEntity<?> validatePatientLogin(String email, String password) {
        try {
            Optional<Patient> patientOpt = patientRepository.findByEmail(email);

            if (patientOpt.isPresent()) {
                Patient patient = patientOpt.get();
                if (patient.getPassword().equals(password)) {
                    String token = tokenService.generateToken(email);
                    Map<String, String> response = new HashMap<>();
                    response.put("token", token);
                    return ResponseEntity.ok(response); // 200 OK
                }
            }
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error); // 401 Unauthorized

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error); // 500 Internal Server Error
        }
    }

    // 9. filterPatient Method: Giriş yapan hastanın randevu geçmişini filtreler
    @Transactional(readOnly = true)
    public List<Appointment> filterPatient(String token, String statusStr, String doctorName) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        String patientEmail = tokenService.extractEmail(token);
        Optional<Patient> patientOpt = patientRepository.findByEmail(patientEmail);

        if (!patientOpt.isPresent()) {
            return new ArrayList<>();
        }

        Long patientId = patientOpt.get().getId();
        List<Appointment> result;

        // 🔥 KESİN ÇÖZÜM: 'all' veya null durumları pas geçilir, 'future' ise 0, 'past' ise 1 kabul edilir.
        Integer status = null;
        if (statusStr != null && !"all".equalsIgnoreCase(statusStr) && !"null".equalsIgnoreCase(statusStr)) {
            if ("future".equalsIgnoreCase(statusStr)) {
                status = 0; // Gelecek randevular (Planlandı)
            } else if ("past".equalsIgnoreCase(statusStr)) {
                status = 1; // Geçmiş randevular (Tamamlandı)
            }
        }

        // Temizlik: Filtreleme parametrelerindeki "all" ve "null" kelimeleri temizlenir
        String filterDocName = (doctorName == null || "all".equalsIgnoreCase(doctorName) || "null".equalsIgnoreCase(doctorName)) ? null : doctorName;

        // Filtre durumlarına göre veriler harika şekilde süzülür
        if (status != null && filterDocName != null) {
            result = appointmentRepository.findByPatientIdAndStatusAndDoctorNameContainingIgnoreCase(patientId, status, filterDocName);
        } else if (status != null) {
            result = appointmentRepository.findByPatientIdAndStatus(patientId, status);
        } else if (filterDocName != null) {
            result = appointmentRepository.findByPatientIdAndDoctorNameContainingIgnoreCase(patientId, filterDocName);
        } else {
            result = appointmentRepository.findByPatientId(patientId);
        }

        // no Session hatalarını engellemek için doktor saat koleksiyonlarını açıkça tetikliyoruz
        if (result != null) {
            result.forEach(app -> {
                if (app.getDoctor() != null && app.getDoctor().getAvailableTimes() != null) {
                    app.getDoctor().getAvailableTimes().size();
                }
            });
        }

        return result;
    }
}


/*

    public List<Doctor> filterDoctor(String name, String specialty, String time) {
        if (name != null && specialty != null && time != null) {
            return doctorService.filterDoctorsByNameSpecilityandTime(name, specialty, time);
        } else if (name != null && specialty != null) {
            return doctorService.filterDoctorByNameAndSpecility(name, specialty);
        } else if (name != null && time != null) {
            return doctorService.filterDoctorByNameAndTime(name, time);
        } else if (specialty != null && time != null) {
            return doctorService.filterDoctorByTimeAndSpecialty(specialty, time);
        } else if (name != null) {
            return doctorService.findDoctorByName(name);
        } else if (specialty != null) {
            return doctorService.filterDoctorBySpecialty(specialty);
        } else if (time != null) {
            return doctorService.filterDoctorsByTime(time);
        } else {
            return doctorService.getDoctors();
        }
    }
*/