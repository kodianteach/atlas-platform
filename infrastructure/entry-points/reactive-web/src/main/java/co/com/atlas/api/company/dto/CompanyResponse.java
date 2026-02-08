package co.com.atlas.api.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO de respuesta para Company.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyResponse {
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
}
