// doctorDashboard.js

import { getAllAppointments }
    from "./services/appointmentRecordService.js";

import { createPatientRow }
    from "./components/patientRows.js";

const patientTableBody =
    document.getElementById(
        "patientTableBody"
    );

let selectedDate =
    new Date()
        .toISOString()
        .split("T")[0];

const token =
    localStorage.getItem(
        "token"
    );

let patientName = null;

/*
 * Search Bar
 */
document.getElementById("searchBar").addEventListener("input", (e) => {
    const value = e.target.value.trim();
    patientName = value.length > 0 ? value : "all";
    loadAppointments();
});

// Event Listener: Today's Appointments Button
document.getElementById("todayButton").addEventListener("click", () => {
    selectedDate = new Date().toISOString().split('T')[0];
    document.getElementById("datePicker").value = selectedDate; // Update date picker too
    loadAppointments();
});

// Event Listener: Date Picker
document.getElementById("datePicker").addEventListener("change", (e) => {
    selectedDate = e.target.value;
    loadAppointments();
});


/*
 * Load Appointments
 */
async function loadAppointments() {

    try {

        const appointments =
            await getAllAppointments(
                selectedDate,
                patientName,
                token
            );

        patientTableBody.innerHTML =
            "";

        if (
            !appointments ||
            appointments.length === 0
        ) {

            patientTableBody.innerHTML =
                `
                <tr>
                    <td colspan="5"
                        class="noPatientRecord">
                        No Appointments found for today.
                    </td>
                </tr>
                `;

            return;
        }

        // doctorDashboard.js içindeki loadAppointments fonksiyonunun ilgili döngü kısmı:

        appointments.forEach(
            appointment => {

                // 🔥 KESİN ÇÖZÜM: 
                // Backend'den (JPA JOIN FETCH ile) gelen iç içe geçmiş patient nesnesini 
                // güvenle okuyoruz. Eğer DTO kullanıyorsanız ve alan düzleştirilmişse alternatifleri de bağlıyoruz.
                const patientObj = appointment.patient;

                const patient = {
                    id: patientObj ? patientObj.id : (appointment.patientId || "N/A"),
                    name: patientObj ? patientObj.name : (appointment.patientName || "Unknown"),
                    phone: patientObj ? patientObj.phone : (appointment.patientPhone || "N/A"),
                    email: patientObj ? patientObj.email : (appointment.patientEmail || "N/A")
                };

                // patientRows.js dosyanıza içi tamamen dolu, doğru hasta nesnesini fırlatıyoruz
                const row = createPatientRow(
                    patient,
                    appointment // Randevunun kendisini (id ve status için) 2. parametre olarak gönderiyoruz
                );

                patientTableBody.appendChild(row);
            }
        );


    } catch (error) {

        console.error(
            "Error loading appointments:",
            error
        );

        patientTableBody.innerHTML =
            `
            <tr>
                <td colspan="5"
                    class="noPatientRecord">
                    Error loading appointments.
                    Try again later.
                </td>
            </tr>
            `;
    }
}

/*
 * Initial Load
 */
document.addEventListener(
    "DOMContentLoaded",
    () => {
        if (typeof renderContent === "function") {
            renderContent();
        }

        // Bugünün tarihini varsayılan olarak kesinleştiriyoruz
        selectedDate = new Date().toISOString().split("T")[0];

        const datePicker = document.getElementById("datePicker");
        if (datePicker) {
            datePicker.value = selectedDate; // HTML input elementine tarihi yazıyoruz
        }

        console.log("İlk yükleme tarihi:", selectedDate);
        loadAppointments();
    }
);