// patientAppointment.js - Tamamen Güncellenmiş Kararlı Sürüm

import { getPatientAppointments, getPatientData, filterAppointments } from "./services/patientServices.js";

const tableBody = document.getElementById("patientTableBody");
const token = localStorage.getItem("token");

let allAppointments = [];
let filteredAppointments = [];
let patientId = null;

document.addEventListener("DOMContentLoaded", initializePage);

async function initializePage() {
  try {
    if (!token) throw new Error("No token found");

    const patient = await getPatientData(token);
    if (!patient) throw new Error("Failed to fetch patient details");

    // Tip güvenliği için hasta ID'sini sayıya çeviriyoruz
    patientId = Number(patient.id || patient.patient?.id);

    // Backend Controller 6. maddeyle tam uyumlu parametre sırası
    const appointmentData = await getPatientAppointments(patientId, token) || [];
    
    console.log("İlk yüklemede gelen ham randevu verileri:", appointmentData);

    // 🔥 KESİN ÇÖZÜM 1: app.patientId yerine backend ilişkisel nesne modeli olan app.patient.id sorgulanır.
    // Eğer veri tabanından gelen randevu listesi zaten sadece bu hastaya aitse, filtreye gerek kalmadan direkt de atanabilir:
    allAppointments = Array.isArray(appointmentData) 
        ? appointmentData.filter(app => Number(app?.patient?.id || app?.patientId) === patientId)
        : [];

    renderAppointments(allAppointments);
  } catch (error) {
    console.error("Error loading appointments:", error);
    alert("❌ Failed to load your appointments.");
  }
}

function renderAppointments(appointments) {
  if (!tableBody) return;
  tableBody.innerHTML = "";

  const actionTh = document.querySelector("#patientTable thead tr th:last-child");
  if (actionTh) {
    actionTh.style.display = "table-cell"; // Kolonu her zaman görünür kıl
  }

  const safeAppointments = Array.isArray(appointments) ? appointments : [];

  if (!safeAppointments.length) {
    tableBody.innerHTML = `<tr><td colspan="5" style="text-align:center; padding:15px; color:#6c757d;">No Appointments Found</td></tr>`;
    return;
  }

  safeAppointments.forEach(appointment => {
    // Backend modeline göre alternatif alan adlarını (doctor.name veya doctorName) güvene alıyoruz
    const docName = appointment?.doctor?.name || appointment?.doctorName || "Doctor";
    const patName = appointment?.patient?.name || appointment?.patientName || "You";
    
    // Tarih alanını düzenleme (LocalDateTime gelirse T harfini boşlukla temizler)
    const rawTime = appointment?.appointmentTime || appointment?.appointmentTimeOnly || "N/A";
    const formattedTime = rawTime.includes("T") ? rawTime.replace("T", " ").substring(0, 16) : rawTime;

    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${patName}</td>
      <td>${docName}</td>
      <td colspan="2">${formattedTime}</td>
      <td style="text-align:center;">
        ${appointment.status == 0 
          ? `<img src="../assets/images/edit/edit.png" alt="Edit" class="prescription-btn" style="cursor:pointer;" data-id="${appointment.id}">` 
          : `<span style="color:#198754; font-weight:bold;">Completed</span>`
        }
      </td>
    `;

    if (appointment.status == 0) {
      const actionBtn = tr.querySelector(".prescription-btn");
      actionBtn?.addEventListener("click", () => redirectToUpdatePage(appointment));
    }

    tableBody.appendChild(tr);
  });
}

function redirectToUpdatePage(appointment) {
  const queryString = new URLSearchParams({
    appointmentId: appointment.id,
    patientId: appointment?.patient?.id || appointment.patientId,
    patientName: appointment?.patient?.name || appointment.patientName || "You",
    doctorName: appointment?.doctor?.name || appointment.doctorName,
    doctorId: appointment?.doctor?.id || appointment.doctorId,
    appointmentTime: appointment.appointmentTime,
  }).toString();

  setTimeout(() => {
    window.location.href = `/pages/updateAppointment.html?${queryString}`;
  }, 100);
}

// Search and Filter Event Listeners (Güvenli bağlama)
document.getElementById("searchBar")?.addEventListener("input", handleFilterChange);
document.getElementById("appointmentFilter")?.addEventListener("change", handleFilterChange);

async function handleFilterChange() {
  const searchBarValue = document.getElementById("searchBar")?.value?.trim() || "";
  const filterValue = document.getElementById("appointmentFilter")?.value || "all";

  const name = searchBarValue === "" ? null : searchBarValue;
  // 'allAppointments' veya 'all' ise backend esnek araması için null gönderiyoruz
  const condition = (filterValue === "allAppointments" || filterValue === "all") ? null : filterValue;

  try {
    const response = await filterAppointments(condition, name, token);
    
    console.log("Filtre tetiğinden dönen ham response paket:", response);

    // 🔥 KESİN ÇÖZÜM 2: response.appointments aramak yerine gelen ham dizi (Array) kontrol edilir.
    const appointmentsList = Array.isArray(response) ? response : (response?.appointments || []);
    
    // 🔥 KESİN ÇÖZÜM 3: app.patientId yerine nesne içi id (app.patient.id) kontrolü ile süzme yapılır
    filteredAppointments = appointmentsList.filter(app => Number(app?.patient?.id || app?.patientId) === patientId);

    renderAppointments(filteredAppointments);
  } catch (error) {
    console.error("Failed to filter appointments:", error);
    alert("❌ An error occurred while filtering appointments.");
  }
}
