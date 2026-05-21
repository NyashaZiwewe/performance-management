package hr.performancemanagement.utils.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommonResponse<T> {

    private boolean isSuccess;
    private int statusCode;
    private String message;
    private T data;
}
