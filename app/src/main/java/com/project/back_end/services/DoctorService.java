package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.models.Appointment;
import com.project.back_end.models.Doctor;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;

@Service // 1. Spring-managed iş mantığı servis katmanı anotasyonu
public class DoctorService {

    // 2. Constructor Injection için bağımlılıklar
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final TokenService tokenService;

    public DoctorService(DoctorRepository doctorRepository, 
                         AppointmentRepository appointmentRepository, 
                         TokenService tokenService) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.tokenService = tokenService;
    }

    // 3 & 4. getDoctorAvailability: Belirli bir tarihte rezerve edilmemiş müsait saatleri listeler
    @Transactional(readOnly = true)
    public List<String> getDoctorAvailability(Long doctorId, LocalDate date) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (!doctorOpt.isPresent()) {
            return new ArrayList<>();
        }
        
        Doctor doctor = doctorOpt.get();
        // Eager load simülasyonu için koleksiyona erişim sağlanır
        List<String> allSlots = new ArrayList<>(doctor.getAvailableTimes());

        // O doktora ve o güne ait rezerve edilmiş randevular çekilir
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date);
        
        // Dolu olan saatlerin başlangıç kısımları toplanır (Örn: "09:00")
        Set<String> bookedTimes = appointments.stream()
                .map(app -> app.getAppointmentTime().toLocalTime().toString())
                .collect(Collectors.toSet());

        // Doktorun tüm slotlarından, dolu olan saat başlangıçları filtrelenir
        return allSlots.stream()
                .filter(slot -> {
                    String startTime = slot.split("-")[0].trim();
                    return !bookedTimes.contains(startTime);
                })
                .collect(Collectors.toList());
    }

    // 5. saveDoctor: E-posta çakışmasını kontrol ederek yeni doktor kaydeder
    @Transactional
    public int saveDoctor(Doctor doctor) {
        try {
            if (doctorRepository.existsByEmail(doctor.getEmail())) {
                return -1; // Çakışma (Conflict) durumunda -1 döner
            }
            doctorRepository.save(doctor);
            return 1; // Başarılı kayıtta 1 döner
        } catch (Exception e) {
            return 0; // İç hata durumunda 0 döner
        }
    }

    // 6. updateDoctor: Var olan doktor bilgilerini günceller
    @Transactional
    public int updateDoctor(Long id, Doctor updatedDoctor) {
        try {
            Optional<Doctor> doctorOpt = doctorRepository.findById(id);
            if (!doctorOpt.isPresent()) {
                return -1; // Doktor bulunamadıysa -1 döner
            }
            
            Doctor existingDoctor = doctorOpt.get();
            existingDoctor.setName(updatedDoctor.getName());
            existingDoctor.setSpecialty(updatedDoctor.getSpecialty());
            existingDoctor.setEmail(updatedDoctor.getEmail());
            existingDoctor.setPhone(updatedDoctor.getPhone());
            existingDoctor.setAvailableTimes(updatedDoctor.getAvailableTimes());
            
            doctorRepository.save(existingDoctor);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // 7. getDoctors: Tüm doktorları getirir ve koleksiyonu yükler
    @Transactional(readOnly = true)
    public List<Doctor> getDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        // ElementCollection (availableTimes) verisinin Eager yüklenmesini garanti altına alır
        doctors.forEach(d -> d.getAvailableTimes().size());
        return doctors;
    }

    // 8. deleteDoctor: Doktoru ve ona bağlı tüm randevuları siler
    @Transactional
    public int deleteDoctor(Long doctorId) {
        try {
            if (!doctorRepository.existsById(doctorId)) {
                return -1; // Doktor bulunamadı
            }
            // İlişkisel bütünlük için önce randevular temizlenir
            appointmentRepository.deleteByDoctorId(doctorId);
            doctorRepository.deleteById(doctorId);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    // 9. validateDoctor: Giriş doğrulaması yapar ve başarılı ise JWT döner
    public ResponseEntity<?> validateDoctor(String email, String password) {
        Optional<Doctor> doctorOpt = doctorRepository.findByEmail(email);
        if (doctorOpt.isPresent() && doctorOpt.get().getPassword().equals(password)) {
            String token = tokenService.generateToken(email);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            return ResponseEntity.ok(response);
        }
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // 10. findDoctorByName: İsme göre kısmi arama yapar ve saatleri yükler
    @Transactional(readOnly = true)
    public List<Doctor> findDoctorByName(String name) {
        List<Doctor> doctors = doctorRepository.findByNameContainingIgnoreCase(name);
        doctors.forEach(d -> d.getAvailableTimes().size());
        return doctors;
    }

    public List<Doctor> filterDoctorByName(List<Doctor> doctors, String name) {
        return doctors.stream().filter(doctor -> doctor.getName().toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList());
    }


    // 11. filterDoctorsByNameSpecilityandTime: İsim, uzmanlık ve günün bölümüne (AM/PM) göre filtreler
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsByNameSpecilityandTime(String name, String specialty, String timePeriod) {
        List<Doctor> baseDoctors = doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
        return filterDoctorByTime(baseDoctors, timePeriod);
    }

    // 12. filterDoctorByTime: Listelenen doktorları çalışma saatine (AM/PM) göre süzer
    public List<Doctor> filterDoctorByTime(List<Doctor> doctors, String timePeriod) {
        if (timePeriod == null || timePeriod.isEmpty()) {
            return doctors;
        }
        
        return doctors.stream()
                .filter(doctor -> doctor.getAvailableTimes().stream().anyMatch(slot -> {
                    String startTimeStr = slot.split("-")[0].trim();
                    LocalTime startTime = LocalTime.parse(startTimeStr);
                    // AM: 12:00 öncesi, PM: 12:00 ve sonrası
                    if ("AM".equalsIgnoreCase(timePeriod)) {
                        return startTime.isBefore(LocalTime.NOON);
                    } else if ("PM".equalsIgnoreCase(timePeriod)) {
                        return startTime.isAfter(LocalTime.NOON) || startTime.equals(LocalTime.NOON);
                    }
                    return false;
                }))
                .collect(Collectors.toList());
    }

    // 13. filterDoctorByNameAndTime: İsim ve günün bölümüne göre filtreler
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorByNameAndTime(String name, String timePeriod) {
        List<Doctor> baseDoctors = doctorRepository.findByNameContainingIgnoreCase(name);
        return filterDoctorByTime(baseDoctors, timePeriod);
    }

    // 14. filterDoctorByNameAndSpecility: İsim ve uzmanlığa göre filtreler
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorByNameAndSpecility(String name, String specialty) {
        return doctorRepository.findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(name, specialty);
    }

    // 15. filterDoctorByTimeAndSpecility: Uzmanlık ve günün bölümüne göre filtreler
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorByTimeAndSpecialty(String specialty, String timePeriod) {
        List<Doctor> baseDoctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        return filterDoctorByTime(baseDoctors, timePeriod);
    }

    // 16. filterDoctorBySpecility: Sadece uzmanlığa göre filtreler
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorBySpecialty(String specialty) {
        return doctorRepository.findBySpecialtyIgnoreCase(specialty);
    }

    public List<Doctor> filterDoctorBySpecialty(List<Doctor> doctors, String specialty) {
        return doctors.stream().filter(doctor -> doctor.getSpecialty().toLowerCase().contains(specialty.toLowerCase())).collect(Collectors.toList());
    }


    // 17. filterDoctorsByTime: Sistemdeki tüm doktorları günün bölümüne göre filtreler
    /*
    @Transactional(readOnly = true)
    public List<Doctor> filterDoctorsByTime(String timePeriod) {
        List<Doctor> allDoctors = doctorRepository.findAll();
        return filterDoctorByTime(allDoctors, timePeriod);
    }
    */
    
}
