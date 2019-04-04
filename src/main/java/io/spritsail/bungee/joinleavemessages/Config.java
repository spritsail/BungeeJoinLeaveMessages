package io.spritsail.bungee.joinleavemessages;

import lombok.Getter;
import net.md_5.bungee.api.ChatColor;

import java.util.HashMap;
import java.util.Map;

import static io.spritsail.bungee.joinleavemessages.JoinLeaveMessages.MessageType;

@Getter
public class Config {
    //    protected boolean enabled = true;
    private String joinLocalMsg = "@p joined the game";
    private String joinOtherMsg = "@p joined to @d";
    private String leaveLocalMsg = "@p left the game";
    private String leaveOtherMsg = "@p left from @s";
    private String switchAwayMsg = "@p switched to @d";
    private String switchIntoMsg = "@p switched from @s";

    private Map<String, ChatColor> colors = new HashMap<String, ChatColor>();

    {
        colors.put("message", ChatColor.YELLOW);
        colors.put("server", ChatColor.YELLOW);
        colors.put("player", ChatColor.YELLOW);
    }

    /**
     * Returns the configured color, or white if unset
     */
    public ChatColor getColor(String key) {
        return colors.getOrDefault(key, ChatColor.WHITE);
    }

    public String getMsgFor(MessageType type, boolean local) {
        return local ? getLocalMsgFor(type) : getOtherMsgFor(type);
    }

    public String getLocalMsgFor(MessageType type) {
        switch (type) {
            case JOIN:
                return joinLocalMsg;
            case SWITCH:
                return switchIntoMsg;
            case LEAVE:
                return leaveLocalMsg;
        }
        return null;
    }

    public String getOtherMsgFor(MessageType type) {
        switch (type) {
            case JOIN:
                return joinOtherMsg;
            case SWITCH:
                return switchAwayMsg;
            case LEAVE:
                return leaveOtherMsg;
        }
        return null;
    }
}
