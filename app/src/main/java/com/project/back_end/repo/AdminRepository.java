package com.project.back_end.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.project.back_end.models.Admin;

@Repository // 3. Spring Data JPA repository bileşeni olarak işaretler
public interface AdminRepository extends JpaRepository<Admin, Long> { // 1. JpaRepository'den miras alarak CRUD özelliklerini kazanır

    // 2. Custom Query Method: findByUsername
    // Açıklamalarınızda dönüş tipini doğrudan Admin istemiş olsanız da, 
    // Service katmanınızda (Service.java satır 47) "adminOpt.isPresent()" kontrolü yaptığınız için 
    // buranın derleme hatası vermemesi adına modern Java standardı olan Optional<Admin> kullanılmıştır.
    Optional<Admin> findByUsername(String username);

    // TokenService'deki (satır 85) adminRepository.existsByUsername(email) çağrısının 
    // hatasız çalışması için gerekli olan yardımcı metot:
    boolean existsByUsername(String username);
}
