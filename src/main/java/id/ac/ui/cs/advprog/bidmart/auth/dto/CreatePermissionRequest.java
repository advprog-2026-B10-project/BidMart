package id.ac.ui.cs.advprog.bidmart.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreatePermissionRequest {
    @NotBlank
    @Pattern(regexp = "^[a-z]+:[a-z]+$", message = "Permission must follow the format 'resource:action' (e.g. auction:create)")
    private String name;
}
