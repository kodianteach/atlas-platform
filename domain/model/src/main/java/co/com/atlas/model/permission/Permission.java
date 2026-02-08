package co.com.atlas.model.permission;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Permission {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String moduleCode;
    private String resource;
    private String action;
    private Instant createdAt;
    private Instant updatedAt;
}
