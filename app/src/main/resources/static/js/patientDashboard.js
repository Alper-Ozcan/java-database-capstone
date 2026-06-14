// patientDashboard.js
//import { getDoctors } from './services/doctorServices.js';
import { openModal, closeModal } from './components/modals.js';
import { createDoctorCard } from './components/doctorCard.js';
import { getDoctors, getFilteredDoctors } from './services/doctorServices.js';
//import { patientSignup, patientLogin } from './services/patientServices.js';

import { API_BASE_URL } from "./config/config.js";


const PATIENT_API = API_BASE_URL + '/patient'

document.addEventListener("DOMContentLoaded", () => { loadDoctorCards(); });

/*
 * Open Add Patient Modal
 */
document.getElementById("patientSignup")?.addEventListener("click", () => { openModal("patientSignup"); });
document.getElementById("patientLogin")?.addEventListener("click", () => { openModal("patientLogin"); });

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


window.patientSingupHandler = async function () {
  try {
    const name = document.getElementById("pname").value;
    const email = document.getElementById("pemail").value;
    const password = document.getElementById("ppassword").value;
    const phone = document.getElementById("pphone").value;
    const address = document.getElementById("paddress").value;

    const data = {
      name,
      email,
      password,
      phone,
      address
    };

    console.log("Patient Signup attempt:", data);

    //const response = patientSignup(data);
    const response = await fetch(
      PATIENT_API ,
      {
        method: "POST",

        headers: {
          "Content-Type":
            "application/json"
        },

        body: JSON.stringify(data)
      }
    );

    if (response.ok) {
      alert("Patient registered successfully!");
      closeModal();
      location.reload(); // Listeyi yenilemek için sayfayı tazeler
    }

  } catch (error) {
    console.error("Add Patient error:", error);
    alert("Failed to add patient. Hint: Ensure mobile number is exactly 10 digits and email is unique.");
  }
};

window.patientLoginHandler = async function () {

  try {
    const email =
      document.getElementById("pemail").value;
    const password =
      document.getElementById("ppassword").value;

    const data = {
      email,
      password
    };

    console.log("Patient login attempt:", data);

    //const response = patientLogin(data);
    const response = await fetch(
      PATIENT_API + "/login",
      {
        method: "POST",

        headers: {
          "Content-Type":
            "application/json"
        },

        body: JSON.stringify(data)
      }
    );

    console.log("Patient login responese:", response);

    if (response.ok) {

      const loginInfo = await response.json();

      console.log("loginInfo", loginInfo);

      localStorage.setItem(
        "token",
        loginInfo.token
      );

      localStorage.setItem(
        "userRole",
        "loggedPatient"
      );

      selectRole("loggedPatient");
      alert("Login successfully!");
      closeModal();
    }
    else {

      alert(
        "Invalid credentials."
      );
    }

  } catch (error) {

    console.error(
      "Patient login error:",
      error
    );

    alert(
      "Unable to login. Please try again."
    );
  }
};

document.addEventListener("DOMContentLoaded", () => {
  const loginBtn = document.getElementById("loginBtn");
  const patientData = {
    email: document.getElementById("pemail")?.value,
    password: document.getElementById("ppassword")?.value
  };

  if (loginBtn) {
    const isSuccess = patientLogin(patientData);
  }
})

// Filter Input
document.getElementById("searchBar").addEventListener("input", patientFilterDoctorsOnChange);
document.getElementById("filterTime").addEventListener("change", patientFilterDoctorsOnChange);
document.getElementById("filterSpecialty").addEventListener("change", patientFilterDoctorsOnChange);


async function patientFilterDoctorsOnChange() {
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

window.signupPatient = async function () {
  try {
    const name = document.getElementById("name").value;
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    const phone = document.getElementById("phone").value;
    const address = document.getElementById("address").value;

    const data = { name, email, password, phone, address };
    const { success, message } = await patientSignup(data);
    if (success) {
      alert(message);
      document.getElementById("modal").style.display = "none";
      window.location.reload();
    }
    else alert(message);
  } catch (error) {
    console.error("Signup failed:", error);
    alert("❌ An error occurred while signing up.");
  }
};


window.loginPatient = async function () {
  try {
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;

    const data = {
      email,
      password
    }
    console.log("loginPatient :: ", data)
    const response = await patientLogin(data);
    console.log("Status Code:", response.status);
    console.log("Response OK:", response.ok);
    if (response.ok) {
      const result = await response.json();
      console.log(result);
      selectRole('loggedpatient');
      localStorage.setItem('token', result.token)
      window.location.href = '/pages/loggedPatientDashboard.html';
    } else {
      alert('❌ Invalid credentials!');
    }
  }
  catch (error) {
    alert("❌ Failed to Login : ", error);
    console.log("Error :: loginPatient :: ", error)
  }
}


