// doctorCard.js

import { showBookingOverlay } from "../loggedPatient.js";
import { deleteDoctor } from "../services/doctorServices.js";
import { getPatientData } from "../services/patientServices.js";
import { bookAppointment } from "../services/appointmentRecordService.js";

export function createDoctorCard(doctor) {

    const card = document.createElement("div");
    card.className = "doctor-card";

    const role = localStorage.getItem("userRole");

    // Doctor Info
    const doctorInfo = document.createElement("div");
    doctorInfo.className = "doctor-info";

    const doctorName = document.createElement("h3");
    doctorName.textContent = doctor.name;

    // Uzmanlık alanı için hem küçük 'specialty' hem de alternatif 'speciality' alanlarını güvene alıyoruz
    const doctorSpecialization = document.createElement("p");
    doctorSpecialization.textContent =
        `Specialty: ${doctor.specialty || doctor.speciality || "General Physician"}`;

    const doctorEmail = document.createElement("p");
    doctorEmail.textContent =
        `Email: ${doctor.email}`;

    const doctorAvailability = document.createElement("p");

    // 🔥 KESİN ÇÖZÜM NOKTASI: 
    // availableAppointments yerine kesinlikle backend'den gelen 'availableTimes' alanı aranmalıdır.
    const times = doctor.availableTimes || doctor.availableAppointments;

    if (times && times.length > 0) {
        doctorAvailability.textContent =
            `Available: ${times.join(", ")}`;
    } else {
        doctorAvailability.textContent =
            "No available appointments";
    }

    doctorInfo.appendChild(doctorName);
    doctorInfo.appendChild(doctorSpecialization);
    doctorInfo.appendChild(doctorEmail);
    doctorInfo.appendChild(doctorAvailability);

    // Actions
    const cardActions = document.createElement("div");
    cardActions.className = "card-actions";

    // =========================
    // ADMIN
    // =========================
    if (role === "admin") {

        const deleteBtn = document.createElement("button");

        deleteBtn.className = "adminBtn";
        deleteBtn.textContent = "Delete Doctor";

        deleteBtn.addEventListener("click", async () => {

            const token = localStorage.getItem("token");

            if (!token) {
                alert("Admin session expired.");
                return;
            }

            const confirmed = confirm(
                `Delete Dr. ${doctor.name}?`
            );

            if (!confirmed) {
                return;
            }

            try {
                await deleteDoctor(doctor.id, token);
                alert("Doctor deleted successfully.");
                card.remove();
            } catch (error) {
                console.error(error);
                alert("Failed to delete doctor.");
            }
        });

        cardActions.appendChild(deleteBtn);
    }

    // =========================
    // PATIENT (NOT LOGGED IN)
    // =========================
    else if (role === "patient") {

        const bookBtn = document.createElement("button");
        bookBtn.textContent = "Book Now";

        bookBtn.addEventListener("click", () => {
            alert("Please login before booking an appointment.");
        });

        cardActions.appendChild(bookBtn);
    }

    // =========================
    // LOGGED PATIENT
    // =========================
    // doctorCard.js - loggedPatient Bloğu
    else if (role === "loggedPatient") {
        const bookBtn = document.createElement("button");
        bookBtn.textContent = "Book Now";

        // 🔥 KESİN ÇÖZÜM 2: Dalgalanma (ripple) efekti koordinatları için fonksiyona tıklama olayı (e) enjekte edildi
        bookBtn.addEventListener("click", async (e) => {
            const token = localStorage.getItem("token");

            if (!token) {
                window.location.href = "/pages/patientDashboard.html";
                return;
            }

            try {
                // Backend profil tetikleyicimiz çalışıyor
                const response = await getPatientData(token);
                
                if (!response) {
                    alert("Oturum süreniz dolmuş olabilir. Lütfen tekrar giriş yapın.");
                    return;
                }

                const patient = response.patient || response.data || response;
                console.log("Aktif hasta verisi overlay'e aktarılıyor:", patient);

                // 🔥 KESİN ÇÖZÜM 3: İlkel prompt pencereleri kaldırıldı! 
                // Tıklama olayı (e), doktor ve hasta nesneleri şık takvim penceresine paslanıyor.
                if (typeof showBookingOverlay === "function") {
                    showBookingOverlay(e, doctor, patient);
                } else {
                    alert(`Randevu ekranı tetikleniyor: Dr. ${doctor.name}`);
                }

            } catch (error) {
                console.error("Randevu butonu tıklama hatası:", error);
                alert("Randevu ekranı yüklenirken bir hata oluştu.");
            }
        });

        cardActions.appendChild(bookBtn);
    }

    card.appendChild(doctorInfo);
    card.appendChild(cardActions);

    return card;
}


/*
Import the overlay function for booking appointments from loggedPatient.js

  Import the deleteDoctor API function to remove doctors (admin role) from docotrServices.js

  Import function to fetch patient details (used during booking) from patientServices.js

  Function to create and return a DOM element for a single doctor card
    Create the main container for the doctor card
    Retrieve the current user role from localStorage
    Create a div to hold doctor information
    Create and set the doctor’s name
    Create and set the doctor's specialization
    Create and set the doctor's email
    Create and list available appointment times
    Append all info elements to the doctor info container
    Create a container for card action buttons
    === ADMIN ROLE ACTIONS ===
      Create a delete button
      Add click handler for delete button
     Get the admin token from localStorage
        Call API to delete the doctor
        Show result and remove card if successful
      Add delete button to actions container
   
    === PATIENT (NOT LOGGED-IN) ROLE ACTIONS ===
      Create a book now button
      Alert patient to log in before booking
      Add button to actions container
  
    === LOGGED-IN PATIENT ROLE ACTIONS === 
      Create a book now button
      Handle booking logic for logged-in patient   
        Redirect if token not available
        Fetch patient data with token
        Show booking overlay UI with doctor and patient info
      Add button to actions container
   
  Append doctor info and action buttons to the car
  Return the complete doctor card element
*/
