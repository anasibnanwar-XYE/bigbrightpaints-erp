package com.bigbrightpaints.erp.modules.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MeResponseTest {

    @Test
    void constructor_defensivelyCopiesRoleAndPermissionLists() {
        List<String> roles = new ArrayList<>(List.of("ROLE_ADMIN"));
        List<String> permissions = new ArrayList<>(List.of("users:read"));

        MeResponse response = new MeResponse(
                "admin@bbp.com",
                "Admin",
                "ACME",
                true,
                false,
                roles,
                permissions);

        roles.add("ROLE_SALES");
        permissions.add("users:write");

        assertThat(response.roles()).containsExactly("ROLE_ADMIN");
        assertThat(response.permissions()).containsExactly("users:read");
        assertThatThrownBy(() -> response.roles().add("ROLE_SUPER_ADMIN"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> response.permissions().add("users:delete"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
