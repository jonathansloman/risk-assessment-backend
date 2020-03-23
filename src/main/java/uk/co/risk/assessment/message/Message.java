package uk.co.risk.assessment.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uk.co.risk.assessment.model.Player;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    private Player player;
    private MessageType type;
    private String data;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData(){
        return data;
    }
}
