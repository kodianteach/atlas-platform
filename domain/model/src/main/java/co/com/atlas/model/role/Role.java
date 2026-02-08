package co.com.atlas.model.role;

import co.com.atlas.model.module.Module;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Role {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String moduleCode;
    private Boolean isSystem;
    private Instant createdAt;
    private Instant updatedAt;
    private List<Module> modules;
}
