package id.ac.ui.cs.advprog.bidmart.auth.dto;

import java.util.Set;
import java.util.stream.Collectors;

import id.ac.ui.cs.advprog.bidmart.auth.entity.RoleGroup;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class RoleGroupResponse {
    private Long id;
    private String name;
    private Set<PermissionResponse> permissions;

    public static RoleGroupResponse fromEntity(RoleGroup roleGroup) {
        return RoleGroupResponse.builder()
                .id(roleGroup.getId())
                .name(roleGroup.getName())
                .permissions(roleGroup.getPermissions().stream()
                        .map(PermissionResponse::fromEntity)
                        .collect(Collectors.toSet()))
                .build();
    }
}
