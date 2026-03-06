package id.ac.ui.cs.advprog.bidmart.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    @Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$",
        message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character"
    )
    private String password;

    @NotBlank(message = "Please select a role")
    @Pattern(regexp = "BUYER|SELLER", message = "Role must be either BUYER or SELLER")
    private String role;
}