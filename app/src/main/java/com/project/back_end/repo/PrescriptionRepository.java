package com.project.back_end.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.project.back_end.models.Prescription;

@Repository // Spring Data MongoDB repository bileşeni olarak işaretler
public interface PrescriptionRepository extends MongoRepository<Prescription, String> { // 1. MongoRepository'den miras alır

    // 2. Custom Query Method: findByAppointmentId
    // Açıklamalarınızda dönüş tipini List<Prescription> istemiş olsanız da, 
    // PrescriptionService (satır 48) içinde "prescriptionOpt.isPresent()" kontrolü yaptığınız için 
    // derleme hatasını önlemek adına modern yaklaşımla Optional<Prescription> yapılmıştır.
    Optional<Prescription> findByAppointmentId(Long appointmentId);

    // PrescriptionService (satır 31) içerisindeki mükerrer reçete kaydı kontrolünün 
    // hatasız çalışması için eklenen zorunlu NoSQL yardımcı metodu:
    boolean existsByAppointmentId(Long appointmentId);
}
