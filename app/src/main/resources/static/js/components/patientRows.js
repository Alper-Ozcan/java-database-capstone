// patientRows.js
// patientRows.js (Kontrol Edin)
export function createPatientRow(patient, appointment) {
  const tr = document.createElement("tr");
  
  const appointmentId = appointment?.id || "N/A";
  // doctorDashboard.js içindeki appointment nesnesinden doktor id'sini veya lokal hafızayı okuyoruz
  const doctorId = appointment?.doctor?.id || appointment?.doctorId || localStorage.getItem("doctorId") || "1";

  tr.innerHTML = `
      <td class="patient-id" style="cursor:pointer; color:#007bff; text-decoration:underline;">${patient.id}</td>
      <td>${patient.name}</td>
      <td>${patient.phone}</td>
      <td>${patient.email}</td>
      <td>
        <img src="/assets/images/addPrescriptionIcon/addPrescription.png" 
             alt="addPrescriptionIcon" 
             class="prescription-btn" 
             style="cursor:pointer; width:24px;"
             data-id="${patient.id}">
      </td>
    `;

  // Tıklama Olayları (Event Listeners)
  tr.querySelector(".patient-id").addEventListener("click", () => {
    window.location.href = `/pages/patientRecord.html?id=${patient.id}&doctorId=${doctorId}`;
  });

  tr.querySelector(".prescription-btn").addEventListener("click", () => {
    window.location.href = `/pages/addPrescription.html?appointmentId=${appointmentId}&patientName=${encodeURIComponent(patient.name)}`;
  });

  return tr;
}

/*
export function createPatientRow(patient, appointment) {
  const tr = document.createElement("tr");
  
  // appointmentId ve doctorId bilgilerini güvenle nesnelerin içerisinden çekiyoruz
  const appointmentId = appointment?.id || "N/A";
  const doctorId = appointment?.doctorId || localStorage.getItem("doctorId") || "all";

  console.log("CreatePatientRow :: Doctor ID:", doctorId, "Appointment ID:", appointmentId);

  // Görsel yolu Spring Boot standartlarına uygun olarak '/' ile kökten başlatıldı
  tr.innerHTML = `
      <td class="patient-id" style="cursor:pointer; color:#007bff; text-decoration:underline;">${patient.id}</td>
      <td>${patient.name}</td>
      <td>${patient.phone}</td>
      <td>${patient.email}</td>
      <td>
        <img src="/assets/images/addPrescriptionIcon/addPrescription.png" 
             alt="addPrescriptionIcon" 
             class="prescription-btn" 
             style="cursor:pointer;"
             data-id="${patient.id}">
      </td>
    `;

  // Hasta ID'sine tıklandığında geçmiş randevu/kayıt sayfasına güvenle yönlendirir
  tr.querySelector(".patient-id").addEventListener("click", () => {
    window.location.href = `/pages/patientRecord.html?id=${patient.id}&doctorId=${doctorId}`;
  });

  // Reçete ikonuna tıklandığında Reçete Yazma sayfasına randevu ID'si ve hasta adıyla uçurur
  tr.querySelector(".prescription-btn").addEventListener("click", () => {
    window.location.href = `/pages/addPrescription.html?appointmentId=${appointmentId}&patientName=${encodeURIComponent(patient.name)}`;
  });

  return tr;
}
*/