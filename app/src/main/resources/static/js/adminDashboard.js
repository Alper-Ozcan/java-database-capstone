// adminDashboard.js

import { openModal, closeModal } from "./components/modals.js";

import {
    getDoctors,
    saveDoctor,
    getFilteredDoctors
} from "./services/doctorServices.js";

import {
    createDoctorCard
} from "./components/doctorCard.js";

document.addEventListener("DOMContentLoaded", () => {loadDoctorCards();});

/*
 * Open Add Doctor Modal
 */
document.getElementById("addDocBtn")?.addEventListener("click", () => {openModal("addDoctor");});

/*
 * Initial Page Load
 */
//document.addEventListener("DOMContentLoaded",() => {loadDoctorCards();});

/*
 * Load All Doctors
 */
export async function loadDoctorCards() {
    try {
        const doctors = await getDoctors();
        renderDoctorCards(doctors);
        console.log("Doctors loaded:");
    } catch (error) {
        console.error( "Error loading doctors:",error);
    }
}

/*
 * set Filters Event Listeners
 */
// Filter Input
document.getElementById("searchBar").addEventListener("input", adminFilterDoctorsOnChange);
document.getElementById("filterTime").addEventListener("change", adminFilterDoctorsOnChange);
document.getElementById("filterSpecialty").addEventListener("change", adminFilterDoctorsOnChange);

/*
 * Filter Doctors 
 */
async function adminFilterDoctorsOnChange() {
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

/*
 * Render Doctor Cards
 */
function renderDoctorCards(doctors) {
    const content = document.getElementById("content");
    content.innerHTML = "";
    doctors.forEach(doctor => {
        const card = createDoctorCard(doctor);
        content.appendChild(card);
    });
}

window.adminAddDoctor = async function () {
    // 1. DÜZELTME: modals.js içindeki HTML ID'leri ile birebir eşitlendi
    const doctorName = document.getElementById("doctorName")?.value;
    const doctorEmail = document.getElementById("doctorEmail")?.value;
    const doctorPhone = document.getElementById("doctorPhone")?.value;
    const doctorPassword = document.getElementById("doctorPassword")?.value;
    const specialty = document.getElementById("specialization")?.value; // doctorSpecialty yerine specialization yapıldı

    // 2. DÜZELTME: Seçili checkbox saat dilimlerini diziye çeviriyoruz
    const checkedBoxes = document.querySelectorAll('input[name="availability"]:checked');
    const availableTimes = Array.from(checkedBoxes).map(box => box.value);

    const token = localStorage.getItem("token");

    if (!token) {
        alert("Admin session expired.");
        return;
    }

    // 3. DÜZELTME: Alan adı backend entity sınıfındaki 'availableTimes' ismiyle eşitlendi
    const doctor = {
        name: doctorName,
        email: doctorEmail,
        phone: doctorPhone,
        password: doctorPassword,
        specialty: specialty,
        availableTimes: availableTimes
    };

    // Ön Validasyon: Boş form gönderimini engeller
    if (!doctorName || !doctorEmail || !doctorPhone || !doctorPassword || !specialty) {
        alert("Please fill in all fields.");
        return;
    }

    try {
        const result = await saveDoctor(doctor, token);

        if (result) {
            alert("Doctor registered successfully!");
            closeModal();
            location.reload(); // Listeyi yenilemek için sayfayı tazeler
        }
    } catch (error) {
        console.error("Add doctor error:", error);
        alert("Failed to add doctor. Hint: Ensure mobile number is exactly 10 digits and email is unique.");
    }
};


/*
  This script handles the admin dashboard functionality for managing doctors:
  - Loads all doctor cards
  - Filters doctors by name, time, or specialty
  - Adds a new doctor via modal form


  Attach a click listener to the "Add Doctor" button
  When clicked, it opens a modal form using openModal('addDoctor')


  When the DOM is fully loaded:
    - Call loadDoctorCards() to fetch and display all doctors


  Function: loadDoctorCards
  Purpose: Fetch all doctors and display them as cards

    Call getDoctors() from the service layer
    Clear the current content area
    For each doctor returned:
    - Create a doctor card using createDoctorCard()
    - Append it to the content div

    Handle any fetch errors by logging them


  Attach 'input' and 'change' event listeners to the search bar and filter dropdowns
  On any input change, call filterDoctorsOnChange()


  Function: filterDoctorsOnChange
  Purpose: Filter doctors based on name, available time, and specialty

    Read values from the search bar and filters
    Normalize empty values to null
    Call filterDoctors(name, time, specialty) from the service

    If doctors are found:
    - Render them using createDoctorCard()
    If no doctors match the filter:
    - Show a message: "No doctors found with the given filters."

    Catch and display any errors with an alert


  Function: renderDoctorCards
  Purpose: A helper function to render a list of doctors passed to it

    Clear the content area
    Loop through the doctors and append each card to the content area


  Function: adminAddDoctor
  Purpose: Collect form data and add a new doctor to the system

    Collect input values from the modal form
    - Includes name, email, phone, password, specialty, and available times

    Retrieve the authentication token from localStorage
    - If no token is found, show an alert and stop execution

    Build a doctor object with the form values

    Call saveDoctor(doctor, token) from the service

    If save is successful:
    - Show a success message
    - Close the modal and reload the page

    If saving fails, show an error message
*/
