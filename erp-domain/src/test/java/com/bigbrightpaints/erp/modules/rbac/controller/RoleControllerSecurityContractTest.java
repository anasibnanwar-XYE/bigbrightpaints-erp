package com.bigbrightpaints.erp.modules.rbac.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.modules.rbac.dto.RoleDto;
import com.bigbrightpaints.erp.modules.rbac.service.RoleService;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

class RoleControllerSecurityContractTest {

    @Test
    void adminRolesController_doesNotExposePostMutationEndpoint() {
        assertThat(Arrays.stream(RoleController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class)
                        || exposesPostRequestMapping(method.getAnnotation(RequestMapping.class)))
                .toList())
                .isEmpty();
    }

    @Test
    void getRoleByKey_normalizesBareRoleNameAgainstReadOnlyCatalog() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.listRolesForCurrentActor())
                .thenReturn(List.of(new RoleDto(1L, "ROLE_ADMIN", "Admin", List.of())));

        RoleController controller = new RoleController(roleService);

        var response = controller.getRoleByKey("admin");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().name()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void getRoleByKey_rejects_role_missing_from_persisted_catalog() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.listRolesForCurrentActor()).thenReturn(List.of());

        RoleController controller = new RoleController(roleService);

        assertThatThrownBy(() -> controller.getRoleByKey("admin"))
                .hasMessageContaining("Role not found: ROLE_ADMIN");
    }

    private boolean exposesPostRequestMapping(RequestMapping mapping) {
        if (mapping == null) {
            return false;
        }
        return Arrays.asList(mapping.method()).contains(RequestMethod.POST);
    }
}
