package co.com.atlas.api.unit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO de respuesta para detalle de unidad con sus miembros (propietarios y residentes).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitMemberResponse {

    private UnitResponse unit;
    private List<MemberDto> owners;
    private List<MemberDto> residents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberDto {
        private Long userId;
        private String names;
        private String email;
        private String phone;
        private String documentType;
        private String documentNumber;
        private String ownershipType;
        private String status;
        private Instant joinedAt;
    }
}
