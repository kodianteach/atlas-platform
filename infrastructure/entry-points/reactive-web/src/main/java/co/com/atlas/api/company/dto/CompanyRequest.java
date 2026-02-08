package co.com.atlas.api.company.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear/actualizar Company.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRequest {
    private String name;
    private String taxId;
    private String industry;
    private String website;
    private String address;
    private String country;
    private String city;
}
