package com.bigbrightpaints.erp.core.exception;

import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void productionProfilesHideDetails() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        setActiveProfile(handler, "dev, production");

        ApplicationException ex = new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "invalid")
                .withDetail("field", "value");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        ResponseEntity<ApiResponse<Map<String, Object>>> response = handler.handleApplicationException(ex, request);

        ApiResponse<Map<String, Object>> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.data()).doesNotContainKey("details");
    }

    @Test
    void nonProductionProfilesIncludeDetails() throws Exception {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        setActiveProfile(handler, "dev,qa");

        ApplicationException ex = new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "invalid")
                .withDetail("field", "value");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");

        ResponseEntity<ApiResponse<Map<String, Object>>> response = handler.handleApplicationException(ex, request);

        ApiResponse<Map<String, Object>> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.data()).containsKey("details");
        assertThat(body.data().get("details"))
                .isInstanceOf(Map.class);
    }

    private static void setActiveProfile(GlobalExceptionHandler handler, String value) throws Exception {
        Field field = GlobalExceptionHandler.class.getDeclaredField("activeProfile");
        field.setAccessible(true);
        field.set(handler, value);
    }
}
