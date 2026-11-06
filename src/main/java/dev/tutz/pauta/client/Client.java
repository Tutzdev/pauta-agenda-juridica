package dev.tutz.pauta.client;

import java.time.Instant;
import java.util.Objects;

import dev.tutz.pauta.shared.Text;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Client() {
    }

    public Client(String name, PhoneNumber phone, String email, String notes, Instant now) {
        this.createdAt = now;
        update(name, phone, email, notes, now);
    }

    public void update(String name, PhoneNumber phone, String email, String notes, Instant now) {
        this.name      = Objects.requireNonNull(Text.trimToNull(name), "name");
        this.phone     = phone == null ? null : phone.digits();
        this.email     = email == null ? null : Text.trimToNull(email.toLowerCase());
        this.notes     = Text.trimToNull(notes);
        this.updatedAt = now;
    }

    public Long getId()           { return id; }
    public String getName()       { return name; }
    public String getPhone()      { return phone; }
    public String getEmail()      { return email; }
    public String getNotes()      { return notes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
