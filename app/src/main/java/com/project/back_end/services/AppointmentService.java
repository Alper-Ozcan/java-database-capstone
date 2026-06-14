package com.project.back_end.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.back_end.models.Appointment;
import com.project.back_end.repo.AppointmentRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

@Service // 1. İş mantığını yürütmek üzere Spring tarafından yönetilen servis bileşeni
public class AppointmentService {

    // 2. Constructor Injection için bağımlılıklar
    private final AppointmentRepository appointmentRepository;
    private final com.project.back_end.services.Service mainService; // validateAppointment metodu barındıran ana servis
    private final TokenService tokenService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              com.project.back_end.services.Service mainService,
                              TokenService tokenService,
                              PatientRepository patientRepository,
                              DoctorRepository doctorRepository) {
        this.appointmentRepository = appointmentRepository;
        this.mainService = mainService;
        this.tokenService = tokenService;
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
    }

    // 3 & 4. Book Appointment Method: Yeni bir randevuyu veri tabanına kaydeder
    @Transactional
    public int bookAppointment(Appointment appointment) {
        try {
            // 🔥 KESİN ÇÖZÜM 1: NullPointerException hatalarını engellemek için 
            // gelen eksik doktor ve hasta nesnelerini MySQL veri tabanındaki tam halleriyle dolduruyoruz.
            if (appointment.getDoctor() == null || appointment.getDoctor().getId() == null ||
                appointment.getPatient() == null || appointment.getPatient().getId() == null) {
                return 0; // Girdiler eksikse kaydı engelle
            }

            // Veri tabanından gerçek kayıtları sorguluyoruz (İlişkisel bütünlük koruması)
            var realDoctor = doctorRepository.findById(appointment.getDoctor().getId());
            var realPatient = patientRepository.findById(appointment.getPatient().getId());

            if (!realDoctor.isPresent() || !realPatient.isPresent()) {
                return 0; // Eğer veri tabanında bu ID'lere sahip canlı bir doktor veya hasta yoksa iptal et
            }

            // Eksik gelen nesne bağlarını veri tabanındaki pürüzsüz nesnelerle değiştiriyoruz
            appointment.setDoctor(realDoctor.get());
            appointment.setPatient(realPatient.get());

            // Önce seçilen saatte doktorun müsaitlik (çakışma) kontrolü ana servis üzerinden tetiklenir
            int availabilityCheck = mainService.validateAppointment(
                    appointment.getDoctor().getId(), 
                    appointment.getAppointmentTime()
            );
            
            if (availabilityCheck != 1) {
                return -1; // Doktor müsait değilse ayırt edici olması için -1 dönüyoruz
            }

            // Randevuyu güvenle MySQL veri tabanına yazıyoruz
            appointmentRepository.save(appointment);
            return 1; // Başarılı kayıt durumunda 1 döner
            
        } catch (Exception e) {
            // 🔥 KESİN ÇÖZÜM 2: Hata oluşursa sessizce 500 fırlatıp çökmesini engellemek için 
            // hatanın asıl nedenini terminale basıp güvenli çıkış yapıyoruz.
            System.err.println("CRITICAL ERROR IN APPOINTMENT SERVICE SAVE BLOCK: " + e.getMessage());
            e.printStackTrace();
            return 0; 
        }
    }


    // 5. Update Appointment Method: Var olan randevu zamanını ve bilgilerini günceller
    @Transactional
    public String updateAppointment(Long appointmentId, Long patientId, LocalDateTime newTime) {
        try {
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (!appointmentOpt.isPresent()) {
                return "Appointment not found";
            }

            Appointment appointment = appointmentOpt.get();

            // Randevunun gerçekten o hastaya ait olup olmadığı doğrulanır
            if (!appointment.getPatient().getId().equals(patientId)) {
                return "Unauthorized: Patient ID mismatch";
            }

            // Seçilen yeni tarih için doktorun müsaitlik kontrolü yapılır
            int availabilityCheck = mainService.validateAppointment(appointment.getDoctor().getId(), newTime);
            if (availabilityCheck != 1) {
                return "Selected time slot is not available for this doctor";
            }

            // Zaman güncellemesi uygulanır ve kaydedilir
            appointment.setAppointmentTime(newTime);
            appointmentRepository.save(appointment);
            return "Success";

        } catch (Exception e) {
            return "An internal error occurred during update";
        }
    }

    // 6. Cancel Appointment Method: Randevuyu veri tabanından siler (İptal eder)
    @Transactional
    public String cancelAppointment(Long appointmentId, Long patientId) {
        try {
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (!appointmentOpt.isPresent()) {
                return "Appointment not found";
            }

            Appointment appointment = appointmentOpt.get();

            // Sadece randevu sahibi hastanın iptal işlemi yapabilmesi güvenceye alınır
            if (!appointment.getPatient().getId().equals(patientId)) {
                return "Unauthorized: You cannot cancel another patient's appointment";
            }

            appointmentRepository.delete(appointment);
            return "Success";

        } catch (Exception e) {
            return "An internal error occurred during cancellation";
        }
    }

    // 7. Get Appointments Method: Doktorun belirli bir gündeki randevularını (isteğe bağlı hasta ismiyle) listeler
    @Transactional(readOnly = true)
    public List<Appointment> getAppointments(Long doctorId, LocalDate date, String patientName) {
        if (patientName != null && !patientName.trim().isEmpty()) {
            // Hem doktor ID, hem tarih, hem de hasta adına (büyük/küçük harf duyarsız) göre filtreleme yapar
            return appointmentRepository.findByDoctorIdAndAppointmentDateAndPatientNameContainingIgnoreCase(doctorId, date, patientName);
        }
        // İsim filtresi yoksa sadece doktor ve ilgili günü getirir
        return appointmentRepository.findByDoctorIdAndAppointmentDate(doctorId, date);
    }

    // 8. Change Status Method: Randevunun durum bilgisini günceller
    @Transactional
    public boolean changeStatus(Long appointmentId, Integer newStatus) {
        try {
            Optional<Appointment> appointmentOpt = appointmentRepository.findById(appointmentId);
            if (!appointmentOpt.isPresent()) {
                return false;
            }

            Appointment appointment = appointmentOpt.get();
            appointment.setStatus(newStatus); // Eğer Enum kullanıyorsanız: appointment.setStatus(AppointmentStatus.values()[newStatus]);
            appointmentRepository.save(appointment);
            return true;

        } catch (Exception e) {
            return false;
        }
    }
}
