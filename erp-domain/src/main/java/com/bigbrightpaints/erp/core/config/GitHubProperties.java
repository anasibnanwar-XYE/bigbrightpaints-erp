package com.bigbrightpaints.erp.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "erp.github")
public class GitHubProperties {

    private boolean enabled = false;
    private String token;
    private String repoOwner;
    private String repoName;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    public void setRepoOwner(String repoOwner) {
        this.repoOwner = repoOwner;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(token)
                && StringUtils.hasText(repoOwner)
                && StringUtils.hasText(repoName);
    }
}
