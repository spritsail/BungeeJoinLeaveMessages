package io.spritsail.bungee.joinleavemessages;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DummyServerInfo implements ServerInfo {

    public static final ServerInfo UNKNOWN = DummyServerInfo.of("Unknown");

    public static ServerInfo of(String name) {
        return new DummyServerInfo(name);
    }

    @Getter
    private String name;

    @Override
    public InetSocketAddress getAddress() {
        return InetSocketAddress.createUnresolved("::", 0);
    }

    @Override
    public Collection<ProxiedPlayer> getPlayers() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getMotd() {
        return name;
    }

    @Override
    public boolean isRestricted() {
        return false;
    }

    @Override
    public String getPermission() {
        return "";
    }

    @Override
    public boolean canAccess(CommandSender sender) {
        return true;
    }

    @Override
    public void sendData(String channel, byte[] data) {
    }

    @Override
    public boolean sendData(String channel, byte[] data, boolean queue) {
        return true;
    }

    @Override
    public void ping(Callback<ServerPing> callback) {
    }
}
