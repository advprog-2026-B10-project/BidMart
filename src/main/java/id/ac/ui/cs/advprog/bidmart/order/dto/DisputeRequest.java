package id.ac.ui.cs.advprog.bidmart.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisputeRequest {

    @NotBlank(message = "reason must not be blank")
    @Size(max = 1000, message = "reason must be at most 1000 characters")
    private String reason;
}
