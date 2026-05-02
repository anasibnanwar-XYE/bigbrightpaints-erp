package com.bigbrightpaints.erp.modules.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

class SupportTicketMessageRepositoryContractTest {

  @Test
  void visibilityHistoryQueriesStayRepositoryLevelBounded() {
    List<Method> visibilityInQueries =
        List.of(SupportTicketMessageRepository.class.getMethods()).stream()
            .filter(
                method ->
                    method.getName().equals("findByTicketAndVisibilityInOrderByCreatedAtAscIdAsc"))
            .toList();

    assertThat(visibilityInQueries)
        .singleElement()
        .satisfies(
            method -> {
              assertThat(method.getReturnType()).isEqualTo(Page.class);
              assertThat(method.getParameterTypes())
                  .containsExactly(SupportTicket.class, Collection.class, Pageable.class);
            });
  }
}
