package sn.immosn.backend.auth.data.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "blacklisted_tokens")
public class BlacklistedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 2000)
    private String token;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDate;

    public BlacklistedToken() {
    }

    public BlacklistedToken(String token, Date expiryDate) {
        this.token = token;
        this.expiryDate = expiryDate;
    }

    public Long getId() {
        return id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }
}
