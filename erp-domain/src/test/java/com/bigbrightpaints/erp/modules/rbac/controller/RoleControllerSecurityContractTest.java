package com.bigbrightpaints.erp.modules.rbac.controller;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;

import static org.assertj.core.api.Assertions.assertThat;

class RoleControllerSecurityContractTest {

    @Test
    void adminRolesController_doesNotExposePostMutationEndpoint() {
        assertThat(Arrays.stream(RoleController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class))
                .toList())
                .isEmpty();
    }
}
