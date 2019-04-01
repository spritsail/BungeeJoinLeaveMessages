package io.spritsail.bungee.msgfilter;

import lombok.Getter;

@Getter
public class Config {
    protected boolean enabled = true;
    private String loginMessage = "%s logged in to %to% from %from%";
    private String logoutMessage = "%s logged out from %from% to %to%";
    private String switchMessage = "%s switched from %from% to %to%";
}
