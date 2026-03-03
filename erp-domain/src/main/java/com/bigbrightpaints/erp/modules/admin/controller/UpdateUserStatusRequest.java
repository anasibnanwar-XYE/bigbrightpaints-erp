package com.bigbrightpaints.erp.modules.admin.controller;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull Boolean enabled) {
}
