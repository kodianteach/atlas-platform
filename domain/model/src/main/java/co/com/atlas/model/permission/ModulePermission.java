package co.com.atlas.model.permission;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class ModulePermission {
    private Long id;
    private Long moduleId;
    private String moduleName;
    private String moduleRoute;
    private Long viewId;
    private String viewName;
    private Integer permissionTypeId;
    private String permissionCode;
    private String permissionName;
    private boolean active;
}
