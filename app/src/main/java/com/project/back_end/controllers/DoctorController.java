package com.project.back_end.controllers;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.back_end.DTO.Login;
import com.project.back_end.models.Doctor;
import com.project.back_end.services.DoctorService;

@RestController // 1. JSON yanıtları dönen REST kontrolcüsü anotasyonu
@RequestMapping("${api.path}doctor") // Konfigüre edilebilir esnek dinamik base path
public class DoctorController {

    // 2. Gerekli servis bağımlılıklarının tanımlanması
    private final DoctorService doctorService;
    private final com.project.back_end.services.Service mainService;

    // Temiz kod mimarisi için Constructor Injection
    public DoctorController(DoctorService doctorService, com.project.back_end.services.Service mainService) {
        this.doctorService = doctorService;
        this.mainService = mainService;
    }

    // 3. getDoctorAvailability: Belirli bir tarihte doktorun rezerve edilmemiş müsait saatlerini listeler (GET)
    @GetMapping("/availability/{user}/{doctorId}/{date}/{token}")
    public ResponseEntity<?> getDoctorAvailability(
            @PathVariable("user") String userRole,
            @PathVariable("doctorId") Long doctorId,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PathVariable("token") String token) {

        // İstek atan kullanıcının rolüne göre token geçerliliği doğrulanır
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, userRole);
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck; // Geçersizse 401 hatasını doğrudan fırlatır
        }

        // Token geçerliyse müsait saat dilimleri listelenir
        List<String> availability = doctorService.getDoctorAvailability(doctorId, date);
        return ResponseEntity.ok(availability);
    }

    // 4. getDoctor: Sistemdeki tüm doktorları listeler (GET)
    @GetMapping
    public ResponseEntity<Map<String, List<Doctor>>> getDoctor() {
        List<Doctor> doctors = doctorService.getDoctors();
        Map<String, List<Doctor>> response = new HashMap<>();
        response.put("doctors", doctors);
        return ResponseEntity.ok(response); // 200 OK durum koduyla haritayı döner
    }

    // 5. saveDoctor: Yeni bir doktor kaydeder (POST)
    @PostMapping("/{token}")
    public ResponseEntity<?> saveDoctor(@RequestBody Doctor doctor, @PathVariable("token") String token) {
        // İşlemin sadece admin tarafından yapılabileceği güvence altına alınır
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, "admin");
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck;
        }

        int result = doctorService.saveDoctor(doctor);
        Map<String, String> response = new HashMap<>();

        if (result == -1) {
            response.put("error", "Conflict: A doctor with this email already exists.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response); // 409 Conflict
        } else if (result == 1) {
            response.put("message", "Doctor registered successfully.");
            return ResponseEntity.status(HttpStatus.CREATED).body(response); // 201 Created
        } else {
            response.put("error", "An internal error occurred while registering the doctor.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response); // 500
        }
    }

    // 6. doctorLogin: Doktor giriş işlemlerini ve JWT üretimini yürüten uç nokta (POST)
    @PostMapping("/login")
    public ResponseEntity<?> doctorLogin(@RequestBody Login loginDto) {
        // Kimlik doğrulama adımları DoctorService katmanına devredilir
        return doctorService.validateDoctor(loginDto.getEmail(), loginDto.getPassword());
    }

    // 7. updateDoctor: Var olan bir doktorun bilgilerini günceller (PUT)
    @PutMapping("/{token}")
    public ResponseEntity<?> updateDoctor(@RequestBody Doctor doctor, @PathVariable("token") String token) {
        // Güncelleme yetkisi sadece admin rolüne atanır
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, "admin");
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck;
        }

        int result = doctorService.updateDoctor(doctor.getId(), doctor);
        Map<String, String> response = new HashMap<>();

        if (result == -1) {
            response.put("error", "Doctor not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // 404 Not Found
        } else if (result == 1) {
            response.put("message", "Doctor updated successfully.");
            return ResponseEntity.ok(response); // 200 OK
        } else {
            response.put("error", "An internal error occurred during update.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 8. deleteDoctor: Doktoru ve ona ait randevuları sistemden tamamen siler (DELETE)
    @DeleteMapping("/{doctorId}/{token}")
    public ResponseEntity<?> deleteDoctor(@PathVariable("doctorId") Long doctorId, @PathVariable("token") String token) {
        // Silme yetkisi kontrolü
        ResponseEntity<?> tokenCheck = mainService.validateToken(token, "admin");
        if (tokenCheck.getStatusCode() != HttpStatus.OK) {
            return tokenCheck;
        }

        int result = doctorService.deleteDoctor(doctorId);
        Map<String, String> response = new HashMap<>();

        if (result == -1) {
            response.put("error", "Doctor not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response); // 404 Not Found
        } else if (result == 1) {
            response.put("message", "Doctor and associated appointments deleted successfully.");
            return ResponseEntity.ok(response); // 200 OK
        } else {
            response.put("error", "An internal error occurred during deletion.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 9. filter: İsim, zaman dilimi ve uzmanlık parametrelerine göre esnek arama sağlar (GET)
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<Map<String, List<Doctor>>> filter(@PathVariable("name") String name,
            @PathVariable("time") String time,
            @PathVariable("speciality") String speciality) {

        // Hem "all" kelimesini hem de JS'den string olarak gelebilecek "null" kelimesini gerçek Java null'ına çeviriyoruz
        String filterName = ("all".equalsIgnoreCase(name) || "null".equalsIgnoreCase(name)) ? null : name;
        String filterTime = ("all".equalsIgnoreCase(time) || "null".equalsIgnoreCase(time)) ? null : time;
        String filterSpec = ("all".equalsIgnoreCase(speciality) || "null".equalsIgnoreCase(speciality)) ? null : speciality;

        // Filtreleme mantığı ana servis (Service) üzerinden tetiklenir
        List<Doctor> filteredDoctors = mainService.filterDoctor(filterName, filterSpec, filterTime);
        Map<String, List<Doctor>> response = new HashMap<>();
        response.put("doctors", filteredDoctors);
        return ResponseEntity.ok(response);
    }
    

    

 
    /*
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<?> filter(@PathVariable("name") String name,
            @PathVariable("time") String time,
            @PathVariable("speciality") String speciality) {

        // Hem "all" kelimesini hem de JS'den string olarak gelebilecek "null" kelimesini gerçek Java null'ına çeviriyoruz
        String filterName = ("all".equalsIgnoreCase(name) || "null".equalsIgnoreCase(name)) ? null : name;
        String filterTime = ("all".equalsIgnoreCase(time) || "null".equalsIgnoreCase(time)) ? null : time;
        String filterSpec = ("all".equalsIgnoreCase(speciality) || "null".equalsIgnoreCase(speciality)) ? null : speciality;

        // Filtreleme mantığı ana servis (Service) üzerinden tetiklenir
        List<Doctor> filteredDoctors = mainService.filterDoctor(filterName, filterSpec, filterTime);
        return ResponseEntity.ok(filteredDoctors);
    }
    */

    /*
    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<?> filter(@PathVariable("name") String name,
                                    @PathVariable("time") String time,
                                    @PathVariable("speciality") String speciality) {
        
        // "all" veya boş geçilen parametreleri esnek sorgu için null olarak düzenliyoruz
        String filterName = "all".equalsIgnoreCase(name) ? null : name;
        String filterTime = "all".equalsIgnoreCase(time) ? null : time;
        String filterSpec = "all".equalsIgnoreCase(speciality) ? null : speciality;

        // Filtreleme mantığı ana servis (Service) üzerinden tetiklenir
        List<Doctor> filteredDoctors = mainService.filterDoctor(filterName, filterSpec, filterTime);
        return ResponseEntity.ok(filteredDoctors);
    }

    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<?> filter(@PathVariable("name") String name,
            @PathVariable("time") String time,
            @PathVariable("speciality") String speciality) {

        // Hem "all" kelimesini hem de JS'den string olarak gelebilecek "null" kelimesini gerçek Java null'ına çeviriyoruz
        String filterName = ("all".equalsIgnoreCase(name) || "null".equalsIgnoreCase(name)) ? null : name;
        String filterTime = ("all".equalsIgnoreCase(time) || "null".equalsIgnoreCase(time)) ? null : time;
        String filterSpec = ("all".equalsIgnoreCase(speciality) || "null".equalsIgnoreCase(speciality)) ? null : speciality;

        // Filtreleme mantığı ana servis (Service) üzerinden tetiklenir
        List<Doctor> filteredDoctors = mainService.filterDoctor(filterName, filterSpec, filterTime);
        return ResponseEntity.ok(filteredDoctors);
    }
        
     */

    /*

    @GetMapping("/filter/{name}/{time}/{speciality}")
    public ResponseEntity<?> filter(@PathVariable("name") String name,
                                    @PathVariable("time") String time,
                                    @PathVariable("speciality") String speciality) {
        
        // "all" veya tarayıcıdan gelebilecek "null" metinlerini temizliyoruz
        String filterName = ("all".equalsIgnoreCase(name) || "null".equalsIgnoreCase(name)) ? null : name;
        String filterTime = ("all".equalsIgnoreCase(time) || "null".equalsIgnoreCase(time)) ? null : time;
        String filterSpec = ("all".equalsIgnoreCase(speciality) || "null".equalsIgnoreCase(speciality)) ? null : speciality;

        List<Doctor> filteredDoctors;

        // 🔥 YENİ ESNEK FİLTRELEME MOTORU (DoctorService kombinasyonları)
        if (filterName != null && filterSpec != null && filterTime != null) {
            // İsim + Uzmanlık + Zaman (AM/PM)
            filteredDoctors = doctorService.filterDoctorsByNameSpecilityandTime(filterName, filterSpec, filterTime);
        } else if (filterName != null && filterTime != null) {
            // İsim + Zaman
            filteredDoctors = doctorService.filterDoctorByNameAndTime(filterName, filterTime);
        } else if (filterSpec != null && filterTime != null) {
            // Uzmanlık + Zaman
            filteredDoctors = doctorService.filterDoctorByTimeAndSpecility(filterSpec, filterTime);
        } else if (filterTime != null) {
            // Sadece Zaman (AM/PM)
            filteredDoctors = doctorService.filterDoctorsByTime(filterTime);
        } else if (filterName != null && filterSpec != null) {
            // İsim + Uzmanlık
            filteredDoctors = doctorService.filterDoctorByNameAndSpecility(filterName, filterSpec);
        } else if (filterName != null) {
            // Sadece İsim
            filteredDoctors = doctorService.findDoctorByName(filterName);
        } else if (filterSpec != null) {
            // Sadece Uzmanlık
            filteredDoctors = doctorService.filterDoctorBySpecility(filterSpec);
        } else {
            // Hiçbiri seçilmediyse (all/all/all) tüm doktorları getirir
            filteredDoctors = doctorService.getDoctors();
        }

        return ResponseEntity.ok(filteredDoctors);
    }
    */
}
