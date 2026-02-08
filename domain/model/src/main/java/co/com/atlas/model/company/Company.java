package co.com.atlas.model.company;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Modelo de dominio para Company (Holding de organizaciones).
 */
@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Company {
    private Long id;
    private String name;
    private String slug;
    private String taxId;
    private String industry;
    private String website;
    private String address;
    private String country;
    private String city;
    private String status;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
}
