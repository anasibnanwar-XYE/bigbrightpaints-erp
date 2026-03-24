package com.bigbrightpaints.erp.modules.factory.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.bigbrightpaints.erp.modules.factory.event.PackagingSlipEvent;

/**
 * Simple listener to surface packaging slip lifecycle events for factory visibility.
 * Can be extended to push to a queue/notification channel.
 */
@Component
public class FactorySlipEventListener {

  private static final Logger log = LoggerFactory.getLogger(FactorySlipEventListener.class);

  @EventListener
  public void onSlipEvent(PackagingSlipEvent event) {
    log.info(
        "Factory slip event: slipId={} orderId={} status={} reason={}",
        event.packagingSlipId(),
        event.salesOrderId(),
        event.status(),
        event.reason());
  }
}
