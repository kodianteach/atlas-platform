package co.com.atlas.model.module;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder(toBuilder = true)
public class Module {
    private Long id;
    private String name;
    private String description;
    private String route;
}
