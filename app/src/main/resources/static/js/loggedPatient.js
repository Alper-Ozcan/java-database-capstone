// loggedPatient.js 
import { createDoctorCard } from './components/doctorCard.js';
import { getDoctors, getFilteredDoctors } from './services/doctorServices.js';
import { bookAppointment } from './services/appointmentRecordService.js';


document.addEventListener("DOMContentLoaded", () => {
  loadDoctorCards();
});

/*
 * Load All Doctors
 */
export async function loadDoctorCards() {
  try {
    const doctors = await getDoctors();
    renderDoctorCards(doctors);
    console.log("Doctors loaded:");
  } catch (error) {
    console.error("Error loading doctors:", error);
  }
}

export async function showBookingOverlay(e, doctor, patient) {
  // Eğer doctorCard.js olay parametresini (e) göndermeyi unuttuysa koruma sağlıyoruz:
  if (!patient && doctor) {
      console.error("Parametre sırası kaymış! doctorCard.js içinden tıklama olayı (e) gönderilmelidir.");
  }

  const button = e.target;
  const rect = button.getBoundingClientRect();
  console.log("Overlay Hasta Adı:", patient?.name);
  console.log("Overlay Hasta Verisi:", patient);

  const ripple = document.createElement("div");
  ripple.classList.add("ripple-overlay");
  ripple.style.left = `${e.clientX}px`;
  ripple.style.top = `${e.clientY}px`;
  document.body.appendChild(ripple);

  setTimeout(() => ripple.classList.add("active"), 50);

  const modalApp = document.createElement("div");
  modalApp.classList.add("modalApp");

  // Doktorun saat listesini güvenli bir şekilde döngüye alıyoruz
  const times = doctor?.availableTimes || doctor?.availableAppointments || [];

  modalApp.innerHTML = `
    <h2>Book Appointment</h2>
    <input class="input-field" type="text" value="${patient?.name || 'Kayıtlı Hasta'}" disabled />
    <input class="input-field" type="text" value="${doctor?.name || 'Doktor'}" disabled />
    <input class="input-field" type="text" value="${doctor?.specialty || doctor?.speciality || 'Genel'}" disabled/>
    <input class="input-field" type="email" value="${doctor?.email || ''}" disabled/>
    <input class="input-field" type="date" id="appointment-date" />
    <select class="input-field" id="appointment-time">
      <option value="">Select time</option>
      ${times.map(t => `<option value="${t}">${t}</option>`).join('')}
    </select>
    <button class="confirm-booking">Confirm Booking</button>
    <button class="cancel-booking" style="background:#ccc; margin-top:5px;">Cancel</button>
  `;

  document.body.appendChild(modalApp);

  setTimeout(() => modalApp.classList.add("active"), 600);

  // Kapatma/İptal Butonu Desteği
  modalApp.querySelector(".cancel-booking").addEventListener("click", () => {
      ripple.remove();
      modalApp.remove();
  });

  modalApp.querySelector(".confirm-booking").addEventListener("click", async () => {
    const date = modalApp.querySelector("#appointment-date").value;
    const time = modalApp.querySelector("#appointment-time").value;
    const token = localStorage.getItem("token");

    if (!date || !time) {
        alert("⚠ Lütfen tarih ve saat dilimini seçiniz.");
        return;
    }

    // 🔥 KESİN ÇÖZÜM 2: .trim() eklenerek boşluklar temizlendi ve temiz "09:00" formatı alındı
    const startTime = time.split('-')[0].trim(); 
    
    const appointment = {
      doctor: { id: parseInt(doctor.id) },
      patient: { id: parseInt(patient.id) },
      // Java'nın tam istediği temiz LocalDateTime: "2026-06-14T09:00:00"
      appointmentTime: `${date}T${startTime}:00`,
      status: 0
    };

    console.log("MySQL'e atılan nihai overlay randevu paketi:", appointment);

    // Dışarıdan import edilen bookAppointment servisi çağrılıyor
    const { success, message } = await bookAppointment(appointment, token);

    if (success) {
      alert("🎉 Appointment Booked successfully!");
      ripple.remove();
      modalApp.remove();
      window.location.reload(); 
    } else {
      // 🔥 KESİN ÇÖZÜM: Backend'den (400 Bad Request ile) gelen detaylı hata mesajını yakalıyoruz
      // Eğer sunucudan özel bir açıklama gelmediyse varsayılan "Time slot taken" uyarısını basar
      const friendlyMessage = message && message !== "Something went wrong" 
        ? message 
        : "The selected time slot is already taken by another patient. Please choose another time or date.";
      
      alert("⚠ Booking Restriction:\n" + friendlyMessage);
    }

  });
}



// Filter Input
document.getElementById("searchBar").addEventListener("input", loggedPatientFilterDoctorsOnChange);
document.getElementById("filterTime").addEventListener("change", loggedPatientFilterDoctorsOnChange);
document.getElementById("filterSpecialty").addEventListener("change", loggedPatientFilterDoctorsOnChange);



async function loggedPatientFilterDoctorsOnChange() {
  console.log("Filter change detected");
  try {
    const name = document.getElementById("searchBar")?.value?.trim() || null;
    const time = document.getElementById("filterTime")?.value || null;
    const specialty = document.getElementById("filterSpecialty")?.value || null;

    console.log("Filter parameters:", name, time, specialty);

    const doctors = await getFilteredDoctors(name, time, specialty);

    console.log("HTML'e gönderilmeye hazırlanan doktor listesi:", doctors);

    if (doctors.length > 0) {
      renderDoctorCards(doctors);
    } else {
      const content = document.getElementById("content");
      if (content) {
        content.innerHTML = `
                    <p class="noDoctorMessage">
                        No doctors found with the given filters.
                    </p>
                `;
      }
    }
  } catch (error) {
    console.error("Filter error:", error);
    alert("Unable to filter doctors.");
  }
}

export function renderDoctorCards(doctors) {
  const contentDiv = document.getElementById("content");
  contentDiv.innerHTML = "";

  doctors.forEach(doctor => {
    const card = createDoctorCard(doctor);
    contentDiv.appendChild(card);
  });

}
