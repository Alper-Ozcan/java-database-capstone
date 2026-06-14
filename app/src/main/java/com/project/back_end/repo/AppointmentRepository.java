package com.project.back_end.repo;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.models.Appointment;

@Repository // 4. Spring Data JPA repository bileşeni olarak işaretler
public interface AppointmentRepository extends JpaRepository<Appointment, Long> { // 1. Temel CRUD özelliklerini miras alır

    // 2. Özel Sorgu Metotları (Custom Query Methods)

    // 🔥 DÜZELTME: d.availableTimes koleksiyonu da JOIN FETCH edilerek 'no Session' hatası kökten çözüldü
    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.doctor d " +
           "LEFT JOIN FETCH d.availableTimes " + 
           "WHERE d.id = :doctorId AND a.appointmentTime BETWEEN :start AND :end")
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(@Param("doctorId") Long doctorId, 
                                                              @Param("start") LocalDateTime start, 
                                                              @Param("end") LocalDateTime end);

    // 🔥 DÜZELTME: d.availableTimes koleksiyonu da JOIN FETCH edilerek 'no Session' hatası kökten çözüldü
    @Query("SELECT a FROM Appointment a " +
           "LEFT JOIN FETCH a.doctor d " +
           "LEFT JOIN FETCH d.availableTimes " + 
           "LEFT JOIN FETCH a.patient p " +
           "WHERE d.id = :doctorId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :patientName, '%')) " +
           "AND a.appointmentTime BETWEEN :start AND :end")
    List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(
            @Param("doctorId") Long doctorId, 
            @Param("patientName") String patientName, 
            @Param("start") LocalDateTime start, 
            @Param("end") LocalDateTime end);

    // 3. Veri silme işlemi için @Modifying ve @Transactional yapılandırması
    @Modifying
    @Transactional
    @Query("DELETE FROM Appointment a WHERE a.doctor.id = :doctorId")
    void deleteAllByDoctorId(@Param("doctorId") Long doctorId);

    // PatientService (satır 38) ve Service (satır 141) uyumu için hastaya ait tüm randevuları getirir
    List<Appointment> findByPatientId(Long patientId);

    // PatientService (satır 60) ve Service (satır 132) uyumu için sıralı getirme filtresi
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a.status = :status ORDER BY a.appointmentTime ASC")
    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(@Param("patientId") Long patientId, @Param("status") int status);

    // PatientService (satır 74) ve Service (satır 135) uyumu için doktor ismine göre filtreleme
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))")
    List<Appointment> filterByDoctorNameAndPatientId(@Param("doctorName") String doctorName, @Param("patientId") Long patientId);

    // PatientService (satır 96) ve Service (satır 129) uyumu için çoklu filtreleme
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a.status = :status AND LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))")
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(@Param("doctorName") String doctorName, @Param("patientId") Long patientId, @Param("status") int status);

    // Veri güncelleme (UPDATE) işlemi için @Modifying yapısı
    @Modifying
    @Transactional
    @Query("UPDATE Appointment a SET a.status = :status WHERE a.id = :id")
    void updateStatus(@Param("status") int status, @Param("id") long id);

    // --- Servis Katmanlarınızdaki Diğer Çağrılar İçin Eklenen Zorunlu Köprü Metotlar ---

    // Service.java (satır 101) içerisindeki doktor randevu çakışma kontrolü için:
    boolean existsByDoctorIdAndAppointmentTime(Long doctorId, LocalDateTime appointmentTime);

    // DoctorService.java (satır 85) içerisindeki doktor silme bağımlılığı için:
    void deleteByDoctorId(Long doctorId);

    // Tarih bazlı listeleme köprü metodu
    default List<Appointment> findByDoctorIdAndAppointmentDate(Long doctorId, java.time.LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay(); 
        LocalDateTime endOfDay = date.atTime(java.time.LocalTime.MAX); 
        return findByDoctorIdAndAppointmentTimeBetween(doctorId, startOfDay, endOfDay);
    }

    // İsim kırılımlı tarih listelemesi köprü metodu
    default List<Appointment> findByDoctorIdAndAppointmentDateAndPatientNameContainingIgnoreCase(
            Long doctorId, java.time.LocalDate date, String patientName) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(java.time.LocalTime.MAX);
        return findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(doctorId, patientName, startOfDay, endOfDay);
    }

    // PatientService.java içindeki isimlendirme standart varyasyonları için alias köprüleri:
    default List<Appointment> findByPatientIdAndStatus(Long patientId, int status) {
        return findByPatient_IdAndStatusOrderByAppointmentTimeAsc(patientId, status);
    }
    default List<Appointment> findByPatientIdAndDoctorNameContainingIgnoreCase(Long patientId, String doctorName) {
        return filterByDoctorNameAndPatientId(doctorName, patientId);
    }
    default List<Appointment> findByPatientIdAndStatusAndDoctorNameContainingIgnoreCase(Long patientId, int status, String doctorName) {
        return filterByDoctorNameAndPatientIdAndStatus(doctorName, patientId, status);
    }
}


/*
@Repository // 4. Spring Data JPA repository bileşeni olarak işaretler
public interface AppointmentRepository extends JpaRepository<Appointment, Long> { // 1. Temel CRUD özelliklerini miras alır

    // 2. Özel Sorgu Metotları (Custom Query Methods)

    // Eager fetching sağlamak için JOIN FETCH içeren JPQL sorgusu
    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.doctor d WHERE d.id = :doctorId AND a.appointmentTime BETWEEN :start AND :end")
    List<Appointment> findByDoctorIdAndAppointmentTimeBetween(@Param("doctorId") Long doctorId, 
                                                              @Param("start") LocalDateTime start, 
                                                              @Param("end") LocalDateTime end);

    // Doktor ve hasta detaylarını JOIN FETCH ile birlikte çeken case-insensitive arama sorgusu
    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.doctor d LEFT JOIN FETCH a.patient p " +
           "WHERE d.id = :doctorId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :patientName, '%')) " +
           "AND a.appointmentTime BETWEEN :start AND :end")
    List<Appointment> findByDoctorIdAndPatient_NameContainingIgnoreCaseAndAppointmentTimeBetween(@Param("doctorId") Long doctorId, 
                                                                                                  @Param("patientName") String patientName, 
                                                                                                  @Param("start") LocalDateTime start, 
                                                                                                  @Param("end") LocalDateTime end);

    // 3. Veri silme işlemi için @Modifying ve @Transactional yapılandırması
    @Modifying
    @Transactional
    @Query("DELETE FROM Appointment a WHERE a.doctor.id = :doctorId")
    void deleteAllByDoctorId(@Param("doctorId") Long doctorId);

    // PatientService (satır 38) ve Service (satır 141) uyumu için hastaya ait tüm randevuları getirir
    List<Appointment> findByPatientId(Long patientId);

    // PatientService (satır 60) ve Service (satır 132) uyumu için sıralı getirme filtresi
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a.status = :status ORDER BY a.appointmentTime ASC")
    List<Appointment> findByPatient_IdAndStatusOrderByAppointmentTimeAsc(@Param("patientId") Long patientId, @Param("status") int status);

    // PatientService (satır 74) ve Service (satır 135) uyumu için doktor ismine göre filtreleme
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))")
    List<Appointment> filterByDoctorNameAndPatientId(@Param("doctorName") String doctorName, @Param("patientId") Long patientId);

    // PatientService (satır 96) ve Service (satır 129) uyumu için çoklu filtreleme
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a.status = :status AND LOWER(a.doctor.name) LIKE LOWER(CONCAT('%', :doctorName, '%'))")
    List<Appointment> filterByDoctorNameAndPatientIdAndStatus(@Param("doctorName") String doctorName, @Param("patientId") Long patientId, @Param("status") int status);

    // Veri güncelleme (UPDATE) işlemi için @Modifying yapısı
    @Modifying
    @Transactional
    @Query("UPDATE Appointment a SET a.status = :status WHERE a.id = :id")
    void updateStatus(@Param("status") int status, @Param("id") long id);

    // --- Servis Katmanlarınızdaki Diğer Çağrılar İçin Eklenen Zorunlu Köprü Metotlar ---

    // Service.java (satır 101) içerisindeki doktor randevu çakışma kontrolü için:
    boolean existsByDoctorIdAndAppointmentTime(Long doctorId, LocalDateTime appointmentTime);

    // DoctorService.java (satır 85) içerisindeki doktor silme bağımlılığı için:
    void deleteByDoctorId(Long doctorId);

    // AppointmentService.java (satır 102) ve DoctorService.java (satır 36) tarih bazlı listeleme için:
    // NOT: Veri tabanında LocalDateTime tutulduğu için JPQL içinde tarih dönüşümü yapılarak eşleme sağlanır.
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND CAST(a.appointmentTime AS date) = :date")
    List<Appointment> findByDoctorIdAndAppointmentDate(@Param("doctorId") Long doctorId, @Param("date") java.time.LocalDate date);

    // AppointmentService.java (satır 100) için isim kırılımlı tarih listelemesi:
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND CAST(a.appointmentTime AS date) = :date AND LOWER(a.patient.name) LIKE LOWER(CONCAT('%', :patientName, '%'))")
    List<Appointment> findByDoctorIdAndAppointmentDateAndPatientNameContainingIgnoreCase(@Param("doctorId") Long doctorId, @Param("date") java.time.LocalDate date, @Param("patientName") String patientName);

    // PatientService.java içindeki isimlendirme standart varyasyonları için alias köprüleri:
    default List<Appointment> findByPatientIdAndStatus(Long patientId, int status) {
        return findByPatient_IdAndStatusOrderByAppointmentTimeAsc(patientId, status);
    }
    default List<Appointment> findByPatientIdAndDoctorNameContainingIgnoreCase(Long patientId, String doctorName) {
        return filterByDoctorNameAndPatientId(doctorName, patientId);
    }
    default List<Appointment> findByPatientIdAndStatusAndDoctorNameContainingIgnoreCase(Long patientId, int status, String doctorName) {
        return filterByDoctorNameAndPatientIdAndStatus(doctorName, patientId, status);
    }
}
*/