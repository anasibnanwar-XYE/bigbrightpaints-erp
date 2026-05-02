package com.bigbrightpaints.erp.modules.company.service;

import java.util.List;

public interface OwnerSetupTeamInvitePort {

  record TeamInvitation(String email, String displayName, List<String> roles) {}

  void createTenantUser(TeamInvitation invitation);
}
