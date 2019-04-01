package io.spritsail.bungee.msgfilter;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.ServerChatEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.ofNullable;

public class MsgFilter extends Plugin implements Listener {
    private Config config = new Config();

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
    }

    /**
     * Handle server-sent messages and cancel those that are vanilla join/leave messages
     */
    @EventHandler
    public void onServerChat(ServerChatEvent sce) {
        // Only handle messages sent _from_ a server
        if (!(sce.getSender() instanceof Server)) {
            return;
        }

        // In theory, all connect/disconnect messages _should_ be SYSTEM messages
        if (sce.getPosition() != ChatMessageType.SYSTEM)
            return;

        if (sce.getMessage()[0] instanceof TranslatableComponent) {

            //getLogger().info(((TranslatableComponent) sce.getMessage()[0]).getWith().get(0).getHoverEvent().toString());

            String key = ((TranslatableComponent) sce.getMessage()[0]).getTranslate();
            if (key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.left")) {
                sce.setCancelled(true);
                return;
            }
        } else {
            // Also cancel events that just happen to use plaintext (old servers?)
            String wholeMsg = BaseComponent.toPlainText(sce.getMessage());
            if (wholeMsg.endsWith("joined the game") ||
                    wholeMsg.endsWith("left the game")) {
                sce.setCancelled(true);
                return;
            }
        }
    }

    /*
    /**
     * Announce to all players when a player joins the proxy
     */
    /*
    @EventHandler
    public void onPostLoginEvent(PostLoginEvent ple) {
        getLogger().info(ple.toString());
//        if (config.isEnabled()) {
//            if (!ple.getPlayer().hasPermission(config.getSilentPermission())) {

        ProxiedPlayer player = ple.getPlayer();
        Optional<ServerInfo> server = ofNullable(player.getServer()).map(Server::getInfo);

        String tmpl = config
                .getLoginMessage()
                .replaceAll("%s", player.getDisplayName())
                .replaceAll("%from%", "the multiplayer screen")
                .replaceAll("%to%", server.map(ServerInfo::getName).orElse("???"));

        BaseComponent[] message = TextComponent.fromLegacyText(
                ChatColor.translateAlternateColorCodes('&', tmpl + " (" + ple.getClass().getSimpleName() + ")")
        );

        this.getProxy().getPlayers()
                .stream()
                .filter(Objects::nonNull)
                .filter(p -> !p.equals(player))
                // TODO: Only send login messages to players on other upstream servers
                .forEach(r -> r.sendMessage(message));
    }
    */

    /**
     * Announce to all players when a player leaves the proxy
     */
    @EventHandler
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent pde) {
//        getLogger().info(pde.toString());

//        if (config.isEnabled()) {
//            if (!pde.getPlayer().hasPermission(config.getSilentPermission())) {

        ProxiedPlayer player = pde.getPlayer();
        Optional<ServerInfo> from = ofNullable(player.getServer()).map(Server::getInfo);

        String tmpl = config
                .getLogoutMessage()
                .replaceAll("%s", player.getDisplayName())
                .replaceAll("%from%", from.map(ServerInfo::getName).orElse("???"))
                .replaceAll("%to%", "the multiplayer screen");

        BaseComponent[] message = TextComponent.fromLegacyText(
                ChatColor.translateAlternateColorCodes('&', tmpl + " (" + pde.getClass().getSimpleName() + ")")
        );

        if (this.getProxy().getPlayers() != null) {
            for (ProxiedPlayer p : this.getProxy().getPlayers()) {
                if (p != null && p != player) {
                    p.sendMessage(message);
                }
            }
//            }
//        }
        }
    }

    /*
    @EventHandler
    public void onServerSwitchEvent(ServerSwitchEvent sse) {

//        if (this.geoProxy().getPlayers() != null) {
        ProxiedPlayer player = sse.getPlayer();
        Optional<ServerInfo> from = ofNullable(sse.getFrom());
        Optional<ServerInfo> to = ofNullable(sse.getTo());

        if (!from.isPresent() || !to.isPresent()) {
            getLogger().fine("skipping " + sse.getClass().getSimpleName() + ": " + sse.toString());
            return;
        }
        getLogger().info(sse.toString());

        String tmpl = config
                .getSwitchMessage()
                .replaceAll("%s", player.getDisplayName())
                .replaceAll("%from%", from.map(ServerInfo::getName).orElse("???"))
                .replaceAll("%to%", to.map(ServerInfo::getName).orElse("???"));

        BaseComponent[] message = TextComponent.fromLegacyText(
                ChatColor.translateAlternateColorCodes('&', tmpl + " (" + sse.getClass().getSimpleName() + ")")
        );

        this.getProxy().getPlayers()
                .stream()
                .filter(Objects::nonNull)
                .filter(r -> !r.equals(player))
                // Only send to recipients on servers that aren't being left/joined by the player
                .filter(r -> !serverEqualsServerInfo(r.getServer(), from))
                .filter(r -> !serverEqualsServerInfo(r.getServer(), sse.getTo()))
                .forEach(r -> r.sendMessage(message));
//        }
    }
    */

    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent sce) {
//        getLogger().info(sce.toString());
//        if (config.getSwitchMessages().isEnabled()) {
//            if (!sce.getPlayer().hasPermission(config.getSwitchMessages().getSilentPermission())) {

        ProxiedPlayer player = sce.getPlayer();
        Optional<ServerInfo> to = ofNullable(sce.getTarget());
        Optional<ServerInfo> from = ofNullable(player.getServer()).map(Server::getInfo);

        if (!to.isPresent()) {
            getLogger().warning("Target 'to' is null: " + sce.toString());
        }

        String erm = (sce.getReason() == ServerConnectEvent.Reason.JOIN_PROXY
                ? config.getLoginMessage()
                : config.getSwitchMessage()
        );

        String tmpl = erm
                .replaceAll("%s", player.getDisplayName())
                .replaceAll("%from%", from.map(ServerInfo::getName).orElse("???"))
                .replaceAll("%to%", to.map(ServerInfo::getName).orElse("???"));

        BaseComponent[] message = TextComponent.fromLegacyText(
                ChatColor.translateAlternateColorCodes('&', tmpl + " (" + sce.getClass().getSimpleName() + ")")
        );

        this.getProxy().getPlayers()
                .stream()
                .filter(Objects::nonNull)
                .filter(r -> !r.equals(player))
                .forEach(r -> r.sendMessage(message));

            /*
            String[] parts = tmpl.split("%s");
            String playerName = sce.getPlayer().getDisplayName();
            TextComponent nameCmp = new TextComponent(playerName);
            nameCmp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + playerName));
            nameCmp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY,
                    new BaseComponent[]{new TextComponent("{name:\" " + "" + "\"")}
            ));

            BaseComponent[] message = new ComponentBuilder(parts[0])
                    .color(ChatColor.YELLOW)
                    .append(nameCmp)
                    .append(parts[1])
                    .create();


        getLogger().info("[" + ((Server) sce.getSender()).getInfo().getName() +
                "] (" +
                sce.getPosition().toString() +
                ") -> " +
                ComponentSerializer.toString(sce.getMessage()));
         */
    }

    /*
    @EventHandler
    public void onServerDisconnectEvent(ServerDisconnectEvent sde) {
        getLogger().info(sde.toString());
//        if (config.getSwitchMessages().isEnabled()) {
//            if (!sde.getPlayer().hasPermission(config.getSwitchMessages().getSilentPermission())) {

        ProxiedPlayer player = sde.getPlayer();
        Optional<ServerInfo> from = ofNullable(sde.getTarget());
        Optional<ServerInfo> to = ofNullable(player.getServer()).map(Server::getInfo);

//        if (to.isPresent()) {
//            String tmpl = "%s disconnected %from% -> %to%";
        String tmpl = config.getSwitchMessage()
                .replaceAll("%s", player.getDisplayName())
                .replaceAll("%from%", from.map(ServerInfo::getName).orElse("???"))
                .replaceAll("%to%", to.map(ServerInfo::getName).orElse("???"));
        BaseComponent[] message = TextComponent.fromLegacyText(
                ChatColor.translateAlternateColorCodes('&', tmpl + " (" + sde.getClass().getSimpleName() + ")")
        );

        from.ifPresent(si -> si.getPlayers()
                .stream()
                .filter(Objects::nonNull)
                // Don't send to the player disconnecting
                .filter(r -> !r.equals(player))
//                .filter(r -> !serverEqualsServerInfo(r.getServer(), to))
                .filter(r -> !serverEqualsServerInfo(r.getServer(), from))
                .forEach(r -> r.sendMessage(message))
        );
//        }
//            }
//        }
    }
    */

    private boolean serverEquals(Server a, Server b) {
        return serverEquals(ofNullable(a), ofNullable(b));
    }

    private boolean serverInfoEquals(ServerInfo a, ServerInfo b) {
        return serverInfoEquals(ofNullable(a), ofNullable(b));
    }

    private boolean serverEqualsServerInfo(Server a, ServerInfo b) {
        return serverEqualsServerInfo(ofNullable(a), ofNullable(b));
    }

    private boolean serverInfoEqualsServer(ServerInfo a, Server b) {
        return serverInfoEqualsServer(ofNullable(a), ofNullable(b));
    }

    private <S extends Optional<Server>> boolean serverEquals(S a, Server b) {
        return serverEquals(a, ofNullable(b));
    }

    private <I extends Optional<ServerInfo>> boolean serverInfoEquals(I a, ServerInfo b) {
        return serverInfoEquals(a, ofNullable(b));
    }

    private <I extends Optional<ServerInfo>> boolean serverEqualsServerInfo(Server a, I b) {
        return serverEqualsServerInfo(ofNullable(a), b);
    }

    private <I extends Optional<ServerInfo>> boolean serverInfoEqualsServer(I a, Server b) {
        return serverInfoEqualsServer(a, ofNullable(b));
    }

    private <S extends Optional<Server>> boolean serverEquals(S a, S b) {
        return a.map(Server::getInfo).equals(b.map(Server::getInfo));
    }

    private <S extends Optional<ServerInfo>> boolean serverInfoEquals(S a, S b) {
        return a.equals(b);
    }

    private <I extends Optional<ServerInfo>, S extends Optional<Server>> boolean serverEqualsServerInfo(S a, I b) {
        return a.map(Server::getInfo).equals(b);
    }

    private <I extends Optional<ServerInfo>, S extends Optional<Server>> boolean serverInfoEqualsServer(I a, S b) {
        return a.equals(b.map(Server::getInfo));
    }
}
