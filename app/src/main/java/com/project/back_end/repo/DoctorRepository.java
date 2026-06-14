package com.project.back_end.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.project.back_end.models.Doctor;

@Repository // 3. Spring Data JPA repository bileşeni olarak işaretler
public interface DoctorRepository extends JpaRepository<Doctor, Long> { // 1. Temel CRUD özelliklerini miras alır

    // 2. Özel Sorgu Metotları (Custom Query Methods)

    // findByEmail: Doktoru e-posta adresine göre getirir.
    // NOT: DoctorService (satır 100) içinde .isPresent() kontrolü yaptığınız için 
    // NullPointerException riskini önlemek adına modern yaklaşımla Optional<Doctor> yapılmıştır.
    Optional<Doctor> findByEmail(String email);

    // findByNameLike: İsme göre kısmi ve büyük/küçük harf duyarlı (case-sensitive) arama yapar.
    @Query("SELECT d FROM Doctor d WHERE d.name LIKE CONCAT('%', :name, '%')")
    List<Doctor> findByNameLike(@Param("name") String name);

    // findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase: 
    // Hem isme hem de uzmanlığa göre büyük/küçük harf duyarsız (case-insensitive) arama yapar.
    List<Doctor> findByNameContainingIgnoreCaseAndSpecialtyIgnoreCase(String name, String specialty);

    // findBySpecialtyIgnoreCase: Uzmanlığa göre büyük/küçük harf duyarsız arama yapar.
    List<Doctor> findBySpecialtyIgnoreCase(String specialty);

    // --- Servis Katmanlarınızdaki Diğer Çağrılar İçin Eklenen Zorunlu Köprü Metotlar ---

    // DoctorService (satır 51) ve Service (satır 73) içerisindeki e-posta çakışma kontrolü için:
    boolean existsByEmail(String email);

    // Service (satır 91) ve DoctorService (satır 109, 128) içindeki isme göre büyük/küçük harf duyarsız arama için:
    List<Doctor> findByNameContainingIgnoreCase(String name);
}
