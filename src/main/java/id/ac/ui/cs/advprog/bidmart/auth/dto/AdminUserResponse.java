package id.ac.ui.cs.advprog.bidmart.auth.dto;

import id.ac.ui.cs.advprog.bidmart.auth.entity.Role;
import id.ac.ui.cs.advprog.bidmart.auth.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String email;
    private String displayName;
    private boolean enabled;
    private Role role;

    public static AdminUserResponse fromUser(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.isEnabled(),
                user.getRole()
        );
    }
}
