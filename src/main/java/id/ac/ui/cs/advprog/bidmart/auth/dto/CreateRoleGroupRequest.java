package id.ac.ui.cs.advprog.bidmart.auth.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleGroupRequest {
    @NotBlank
    private String name;
    private Set<Long> permissionIds;
}
