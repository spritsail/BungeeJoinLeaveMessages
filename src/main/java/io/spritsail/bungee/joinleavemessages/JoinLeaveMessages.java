package io.spritsail.bungee.joinleavemessages;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
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
import net.md_5.bungee.chat.ComponentSerializer;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

public class JoinLeaveMessages extends Plugin implements Listener {

    private Config config = new Config();

    private static final boolean
            LOCAL = true,
            OTHER = false;

    public enum MessageType {
        JOIN,
        SWITCH,
        LEAVE
    }

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

        try {
            // Use getMessageJson() instead of getMessage because in most cases it should prevent
            // Bungee from re-serialising the message in the case that it was never changed.
            BaseComponent[] message = ComponentSerializer.parse(sce.getMessageJson());

            if (message[0] instanceof TranslatableComponent) {

                String key = ((TranslatableComponent) message[0]).getTranslate();
                if (key.equals("multiplayer.player.joined") || key.equals("multiplayer.player.left")) {
                    sce.setCancelled(true);
                    return;
                }
            } else {
                // Also cancel events that just happen to use plaintext (old servers?)
                String wholeMsg = BaseComponent.toPlainText(message);
                if (wholeMsg.endsWith("joined the game") ||
                        wholeMsg.endsWith("left the game")) {
                    sce.setCancelled(true);
                }
            }
        } catch (Exception e) {
            // In the case that shit hits the fan, just fail quietly and get
            // on with our day. If this exception isn't caught then the player
            // is unfairly kicked just because the server and Bungee disagreed.
            getLogger().severe(e::getMessage);
            sce.setCancelled(false);
        }
    }

    /**
     * Announce to all players when a player leaves the proxy
     */
    @EventHandler
    public void onPlayerDisconnectEvent(PlayerDisconnectEvent pde) {
        ProxiedPlayer player = pde.getPlayer();
        Optional<ServerInfo> from = ofNullable(player.getServer()).map(Server::getInfo);

        // TODO: Allow customisability of the 'to' for leave events
        // 'to' server is empty because one cannot leave 'to' anywhere
        notifyEveryone(MessageType.LEAVE, player, from, empty());
    }

    @EventHandler
    public void onServerConnectEvent(ServerConnectEvent sce) {
//        getLogger().info(sce.toString());
//        if (config.getSwitchMessages().isEnabled()) {
//            if (!sce.getPlayer().hasPermission(config.getSwitchMessages().getSilentPermission())) {

        ProxiedPlayer player = sce.getPlayer();
        // TODO: Allow customisability of the 'from' for join events
        Optional<ServerInfo> to = ofNullable(sce.getTarget());
        Optional<ServerInfo> from = ofNullable(player.getServer()).map(Server::getInfo);

        // Suppress player trying to change to the server they're already on
        if (to.equals(from))
            return;

        // Assume if a player isn't joining the proxy for the first time that they're switching servers
        MessageType type = (sce.getReason() == ServerConnectEvent.Reason.JOIN_PROXY ?
                MessageType.JOIN :
                MessageType.SWITCH);

        notifyEveryone(type, player, from, to);
    }

    public <I extends Optional<ServerInfo>> void notifyEveryone(MessageType type, ProxiedPlayer actor, I from, I to) {

        Optional<ServerInfo> localServer = (type == MessageType.LEAVE ? from : to);

        Map<Boolean, List<ProxiedPlayer>> groups = this.getProxy().getPlayers()
                .stream()
                .filter(Objects::nonNull)
                .filter(r -> !r.equals(actor))
                .collect(Collectors.partitioningBy(r -> serverEqualsInfo(r.getServer(), localServer)));

        sendTo(type, LOCAL, actor, from, to, groups.get(LOCAL));
        sendTo(type, OTHER, actor, from, to, groups.get(OTHER));
    }

    public <I extends Optional<ServerInfo>> void sendTo(MessageType type, boolean local, ProxiedPlayer actor,
                                                        I from, I to, Collection<ProxiedPlayer> whom) {

        // If nobody is around to listen, is the message even generated?
        if (whom.size() < 1)
            return;

        // Find the correct join/switch/leave message template
        String msgTemplate = config.getMsgFor(type, local);

        List<BaseComponent> components = new ArrayList<>();

        String[] split = msgTemplate.split(String.format("((?<=%1$s)|(?=%1$s))", "@[dsp]"));
        for (String part : split) {
            BaseComponent cmp = null;
            String colorKey = null;
            switch (part) {
                case "@d":
                    cmp = makeServerComponent(to.orElse(DummyServerInfo.UNKNOWN));
                    colorKey = "server";
                    break;
                case "@s":
                    cmp = makeServerComponent(from.orElse(DummyServerInfo.UNKNOWN));
                    colorKey = "server";
                    break;
                case "@p":
                    cmp = makePlayerNameComponent(actor);
                    colorKey = "player";
                    break;
                default:
                    cmp = new TextComponent(part);
                    colorKey = "message";
                    break;
            }
            cmp.setColor(config.getColor(colorKey));
            components.add(cmp);
        }

        BaseComponent[] message = components.toArray(new BaseComponent[0]);

        // Send to all recipients
        whom.forEach(r -> r.sendMessage(message));
    }

    /**
     * Generates a chat Component that mirrors the vanilla behaviour, for Bungee :)
     */
    private TextComponent makePlayerNameComponent(ProxiedPlayer actor) {
        String playerName = actor.getDisplayName();
        UUID uniqueId = actor.getUniqueId();

        TextComponent cmp = new TextComponent(playerName);
        cmp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + playerName + " "));
        cmp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY,
                new BaseComponent[]{
                        new TextComponent("{" +
                                "name:\"{\\\"text\\\":\\\"" + playerName + "\\\"}\"," +
                                "id:\"" + uniqueId + "\"," +
                                "type:\"minecraft:player\"" +
                                "}"
                        )
                }
        ));

        return cmp;
    }

    /**
     * Generates a clickable chat Component for servers that is a little more interesting than a lone word
     */
    private TextComponent makeServerComponent(ServerInfo server) {
        final TextComponent clickToJoin = new TextComponent("Click to join");
        clickToJoin.setItalic(true);

        String name = server.getName();
        TextComponent cmp = new TextComponent(name);
        cmp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + name));
        cmp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new BaseComponent[]{
                        new TextComponent(name.substring(0, 1).toUpperCase() + name.substring(1) + "\n"),
                        clickToJoin
                }
        ));
        return cmp;
    }

    private <I extends Optional<ServerInfo>> boolean serverEqualsInfo(Server a, I b) {
        return serverEqualsInfo(ofNullable(a), b);
    }

    private <I extends Optional<ServerInfo>, S extends Optional<Server>> boolean serverEqualsInfo(S a, I b) {
        return a.map(Server::getInfo).equals(b);
    }
}
