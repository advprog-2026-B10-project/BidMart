package id.ac.ui.cs.advprog.bidmart.auth.dto;

import id.ac.ui.cs.advprog.bidmart.auth.entity.Permission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PermissionResponse {
    private Long id;
    private String name;

    public static PermissionResponse fromEntity(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .name(permission.getName())
                .build();
    }
}
