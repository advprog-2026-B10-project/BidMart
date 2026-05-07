package id.ac.ui.cs.advprog.bidmart.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @Size(min = 2, max = 100, message = "Display name must be between 2 and 100 characters")
    private String displayName;

    @Pattern(
            regexp = "^\\+?[0-9]{8,15}$",
            message = "Phone number must contain 8 to 15 digits and may start with +"
    )
    private String phoneNumber;
}
