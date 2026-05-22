package id.ac.ui.cs.advprog.bidmart.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipOrderRequest {

    @NotBlank(message = "trackingNumber must not be blank")
    @Size(max = 100, message = "trackingNumber must be at most 100 characters")
    private String trackingNumber;
}
