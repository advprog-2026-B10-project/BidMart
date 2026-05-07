package id.ac.ui.cs.advprog.bidmart.auth.exception;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ApiErrorResponse {
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> details;
}
