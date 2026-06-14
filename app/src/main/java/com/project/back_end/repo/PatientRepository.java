package com.project.back_end.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.back_end.models.Patient;

@Repository // 3. Spring Data JPA repository bileşeni olarak işaretler
public interface PatientRepository extends JpaRepository<Patient, Long> { // 1. Temel CRUD özelliklerini miras alır

    // 2. Özel Sorgu Metotları (Custom Query Methods)

    // findByEmail: Hastayı e-posta adresine göre getirir.
    // NOT: PatientService (satır 117) ve Service (satır 114) içinde .isPresent() kontrolü yaptığınız için
    // NullPointerException riskini önlemek adına modern yaklaşımla Optional<Patient> yapılmıştır.
    Optional<Patient> findByEmail(String email);

    // findByEmailOrPhone: Hastayı e-posta VEYA telefon numarasına göre getirir.
    Optional<Patient> findByEmailOrPhone(String email, String phone);

    // --- Servis Katmanlarınızdaki Diğer Çağrılar İçin Eklenen Zorunlu Köprü Metotlar ---

    // Service.java (satır 108) ve TokenService.java (satır 89) içindeki e-posta doğrulama kontrolü için:
    boolean existsByEmail(String email);

    // Service.java (satır 109) içindeki telefon numarası doğrulama kontrolü için:
    boolean existsByPhone(String phone);
}
