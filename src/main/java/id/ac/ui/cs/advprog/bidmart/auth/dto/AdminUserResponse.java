package id.ac.ui.cs.advprog.bidmart.auth.dto;

import java.util.Set;
import java.util.stream.Collectors;

import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AdminUserResponse {
    private Long id;
    private String email;
    private String displayName;
    private boolean enabled;
    private Role role;
    private long activeSessions;
    private Set<RoleGroupResponse> roleGroups;

    public static AdminUserResponse fromUser(User user) {
        return builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .enabled(user.isEnabled())
                .role(user.getRole())
                .activeSessions(0)
                .roleGroups(user.getRoleGroups().stream()
                        .map(RoleGroupResponse::fromEntity)
                        .collect(Collectors.toSet()))
                .build();
    }

    public static AdminUserResponse fromUser(User user, long activeSessions) {
        return builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .enabled(user.isEnabled())
                .role(user.getRole())
                .activeSessions(activeSessions)
                .roleGroups(user.getRoleGroups().stream()
                        .map(RoleGroupResponse::fromEntity)
                        .collect(Collectors.toSet()))
                .build();
    }
}
