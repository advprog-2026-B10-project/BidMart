package id.ac.ui.cs.advprog.bidmart.auth.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRolesRequest {
    @NotNull
    private Set<Long> roleGroupIds;
}
