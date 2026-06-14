package com.project.back_end.DTO;

public class Login {

    @jakarta.validation.constraints.NotBlank(message = "Email cannot be blank")
    private String email;

    @jakarta.validation.constraints.NotBlank(message = "Password cannot be blank")
    private String password;


    // Varsayılan Yapıcı Metot (Java tarafından örtülü olarak da sağlanır, Jackson serileştirmesi için kritiktir)
    public Login() {
    }

    // --- Getters and Setters ---

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
