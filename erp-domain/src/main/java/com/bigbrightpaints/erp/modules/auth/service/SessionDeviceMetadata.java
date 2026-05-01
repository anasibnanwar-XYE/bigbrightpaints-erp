package com.bigbrightpaints.erp.modules.auth.service;

public record SessionDeviceMetadata(
    String deviceLabel, String userAgentHash, String ipAddressHash) {}
