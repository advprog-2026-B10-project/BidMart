package id.ac.ui.cs.advprog.bidmart.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShippingAddressRequest {

    @NotBlank(message = "address must not be blank")
    @Size(max = 500, message = "address must be at most 500 characters")
    private String address;
}
