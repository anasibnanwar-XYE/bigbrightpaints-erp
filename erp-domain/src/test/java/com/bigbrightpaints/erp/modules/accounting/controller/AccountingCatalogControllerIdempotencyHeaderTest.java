package com.bigbrightpaints.erp.modules.accounting.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class AccountingCatalogControllerIdempotencyHeaderTest {

  @Test
  void retiredAccountingCatalogController_isNotInstantiableOrPublic() {
    int modifiers = AccountingCatalogController.class.getModifiers();

    assertThat(Modifier.isFinal(modifiers)).isTrue();
    assertThat(Modifier.isPublic(modifiers)).isFalse();

    Constructor<?>[] constructors = AccountingCatalogController.class.getDeclaredConstructors();
    assertThat(constructors).hasSize(1);
    assertThat(Modifier.isPrivate(constructors[0].getModifiers())).isTrue();
  }

  @Test
  void retiredAccountingCatalogController_exposesNoPublicMethods() {
    Method[] declaredMethods =
        Arrays.stream(AccountingCatalogController.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .filter(method -> !method.getName().startsWith("$jacoco"))
            .toArray(Method[]::new);

    assertThat(declaredMethods).isEmpty();
  }
}
