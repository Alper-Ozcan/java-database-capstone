package com.project.back_end.services;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.project.back_end.repo.AdminRepository;
import com.project.back_end.repo.DoctorRepository;
import com.project.back_end.repo.PatientRepository;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component // 1. Spring context tarafından yönetilen bir Bean olmasını sağlar
public class TokenService {

    private final AdminRepository adminRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    @Value("${jwt.secret}") // application.properties dosyasından secret key'i çeker
    private String jwtSecret;

    // 2. Constructor Injection: İdeal bağımlılık enjeksiyonu mimarisi
    public TokenService(AdminRepository adminRepository, 
                        DoctorRepository doctorRepository, 
                        PatientRepository patientRepository) {
        this.adminRepository = adminRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    // 3. getSigningKey Metodu: String anahtarı HMAC-SHA SecretKey nesnesine dönüştürür
    private SecretKey getSigningKey() {
        byte[] keyBytes = this.jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 4. generateToken Metodu: Kullanıcı e-postasına göre 7 gün geçerli token üretir
    /*public String generateToken(String email) {
        Date now = new Date();
        long expirationTimeInMilliseconds = 7L * 24 * 60 * 60 * 1000; // 7 gün
        Date expiryDate = new Date(now.getTime() + expirationTimeInMilliseconds);

        return Jwts.builder()
                .setSubject(email) // Email adresini subject olarak ayarlar
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // Güvenli imzalama
                .compact();
    }*/

    // Güncel JJWT v0.12.x+ Uyumlu generateToken Metodu
    public String generateToken(String email) {
        Date now = new Date();
        long expirationTimeInMilliseconds = 7L * 24 * 60 * 60 * 1000; // 7 gün
        Date expiryDate = new Date(now.getTime() + expirationTimeInMilliseconds);

        return Jwts.builder()
                .subject(email) // 👈 setSubject yerine doğrudan subject()
                .issuedAt(now)  // 👈 setIssuedAt yerine doğrudan issuedAt()
                .expiration(expiryDate) // 👈 setExpiration yerine doğrudan expiration()
                .signWith(getSigningKey()) // 👈 Algorithm parametresine artık gerek yok, anahtardan otomatik algılar
                .compact();
    }


    // 5. extractEmail Metodu: Token'ı doğrular ve içerisindeki e-posta bilgisini çözer
    public String extractEmail(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // for error
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // 6. validateToken Metodu: Rol bazlı olarak kullanıcının veri tabanında varlığını doğrular
    public boolean validateToken(String token, String role) {
        try {
            String email = extractEmail(token);
            
            if (email == null) {
                return false;
            }

            // Gelen role göre ilgili repository üzerinden varlık kontrolü yapılır
            switch (role.toLowerCase()) {
                case "admin":
                    // Admin entity'nizde username alanı vardı, eğer login için email/username eşleşmesi
                    // repository'nizde hazırsa: adminRepository.existsByUsername(email) veya existsByEmail
                    return adminRepository.existsByUsername(email);
                case "doctor":
                    return doctorRepository.existsByEmail(email);
                case "patient":
                    return patientRepository.existsByEmail(email);
                default:
                    return false;
            }
        } catch (Exception e) {
            // Token süresi dolmuşsa, sahteyse veya herhangi bir hata oluşursa yakalar ve false döner
            return false;
        }
    }
}
