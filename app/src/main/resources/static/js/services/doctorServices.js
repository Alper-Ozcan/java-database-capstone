// doctorServices.js
import { API_BASE_URL } from "../config/config.js";

const DOCTOR_API = API_BASE_URL + '/doctor';

/*
 * Function: getDoctors
 */
export async function getDoctors() {
    try {
        const response = await fetch(DOCTOR_API);
        console.log("Donen getDoctors Responese", response);
        const data = await response.json();
        return data.doctors || data.doctor || [];
    } catch (error) {
        console.error("Error fetching doctors:", error);
        return [];
    }
}

export async function getFilteredDoctors(name, time, specialty) {
    try {
        const finalName = (!name || name === null || name.trim() === "") ? "all" : name.trim();
        const finalTime = (!time || time === null || time === "null") ? "all" : time;
        const finalSpec = (!specialty || specialty === null || specialty === "null") ? "all" : specialty;

        console.log("Tarayıcının göndereceği net değerler:", { finalName, finalTime, finalSpec });

        const url = `${DOCTOR_API}/filter/${encodeURIComponent(finalName)}/${encodeURIComponent(finalTime)}/${encodeURIComponent(finalSpec)}`;

        const response = await fetch(url);

        console.log("Donen getFilteredDoctors Responese", response);

        const data = await response.json();
        console.log("Doctors json: ", data.doctors || data.doctor || []);
        return data.doctors || data.doctor || [];
    } catch (error) {
        console.error("Error fetching doctors:", error);
        return [];
    }
}

/*
 * Function: deleteDoctor
 */
export async function deleteDoctor(doctorId, token) {
    try {
        const response = await fetch(`${DOCTOR_API}/${doctorId}/${token}`, {
            method: "DELETE"
        });
        const data = await response.json();
        return {
            success: response.ok,
            message: data.message || data.error || "Doctor deleted successfully."
        };
    } catch (error) {
        console.error("Error deleting doctor:", error);
        return { success: false, message: "Failed to delete doctor." };
    }
}

/*
 * Function: saveDoctor
 */
export async function saveDoctor(doctor, token) {
    try {
        const response = await fetch(`${DOCTOR_API}/${token}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(doctor)
        });
        const data = await response.json();
        return {
            success: response.ok,
            message: data.message || data.error || "Doctor saved successfully."
        };
    } catch (error) {
        console.error("Error saving doctor:", error);
        return { success: false, message: "Failed to save doctor." };
    }
}

/*
 * Function: filterDoctors
 * Purpose: Filter doctors by name, time and specialty
 */

/*
export async function filterDoctors(name, time, specialty) {
    try {
        const finalName = (!name || name === null || name.trim() === "") ? "all" : name.trim();
        const finalTime = (!time || time === null || time === "null") ? "all" : time;
        const finalSpec = (!specialty || specialty === null || specialty === "null") ? "all" : specialty;

        console.log("Tarayıcının göndereceği net değerler:", { finalName, finalTime, finalSpec });
       
        const url = `${DOCTOR_API}/filter/${encodeURIComponent(finalName)}/${encodeURIComponent(finalTime)}/${encodeURIComponent(finalSpec)}`;

        const response = await fetch(url, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
            },
        });

        if (response.ok) {
            const data = await response.json();
            console.log("BAŞARILI! Backend'den dönen doktorlar:", data);
            return data;
        } else {
            console.error("Sunucu isteği kabul etti ama hata döndü. Kod:", response.status);
            return [];
        }
    } catch (error) {
        console.error("Bağlantı Hatası:", error);
        return [];
    }
}
*/

/*
  Import the base API URL from the config file
  Define a constant DOCTOR_API to hold the full endpoint for doctor-related actions


  Function: getDoctors
  Purpose: Fetch the list of all doctors from the API

   Use fetch() to send a GET request to the DOCTOR_API endpoint
   Convert the response to JSON
   Return the 'doctors' array from the response
   If there's an error (e.g., network issue), log it and return an empty array


  Function: deleteDoctor
  Purpose: Delete a specific doctor using their ID and an authentication token

   Use fetch() with the DELETE method
    - The URL includes the doctor ID and token as path parameters
   Convert the response to JSON
   Return an object with:
    - success: true if deletion was successful
    - message: message from the server
   If an error occurs, log it and return a default failure response


  Function: saveDoctor
  Purpose: Save (create) a new doctor using a POST request

   Use fetch() with the POST method
    - URL includes the token in the path
    - Set headers to specify JSON content type
    - Convert the doctor object to JSON in the request body

   Parse the JSON response and return:
    - success: whether the request succeeded
    - message: from the server

   Catch and log errors
    - Return a failure response if an error occurs


  Function: filterDoctors
  Purpose: Fetch doctors based on filtering criteria (name, time, and specialty)

   Use fetch() with the GET method
    - Include the name, time, and specialty as URL path parameters
   Check if the response is OK
    - If yes, parse and return the doctor data
    - If no, log the error and return an object with an empty 'doctors' array

   Catch any other errors, alert the user, and return a default empty result
*/
