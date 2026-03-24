package com.bigbrightpaints.erp.modules.rbac.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bigbrightpaints.erp.modules.rbac.dto.CreateRoleRequest;

class RoleControllerSecurityContractTest {

  @Test
  void createRole_usesRoleMutationGuardAtControllerBoundary() throws Exception {
    Method method = RoleController.class.getMethod("createRole", CreateRoleRequest.class);

    PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

    assertThat(annotation).isNotNull();
    assertThat(annotation.value()).isEqualTo("hasAnyAuthority('ROLE_ADMIN','ROLE_SUPER_ADMIN')");
  }
}
