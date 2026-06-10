package sn.immosn.backend.auth.data.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)

public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(nullable = false)
    protected String nomComplet;

    @Column(unique = true, nullable = false)
    protected String email;

    @Column(nullable = false)
    protected String telephone;

    protected String adresse;

    protected String photo;

    protected String password;

    @Column(nullable = false)
    protected LocalDateTime creationDate;

    @Column(nullable = false)
    protected boolean isArchived = false;

    @Column(name = "dernier_connexion")
    protected LocalDateTime dernierConnexion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    protected Set<Role> roles=new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (creationDate == null) {
            creationDate = LocalDateTime.now();
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole().name()))
                .toList();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !isArchived;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isArchived;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !isArchived;
    }

    @Override
    public boolean isEnabled() {
        return !isArchived;
    }

}