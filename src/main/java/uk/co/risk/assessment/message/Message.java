package uk.co.risk.assessment.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import uk.co.risk.assessment.model.Table;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {
    // for outgoing messages, we send the whole table state, which is all public data
    private Table table;
    // for various sorts of incoming messages
    private NewPlayerMessage newPlayer;
    private MessageType type;
    private String data;
    // the name of the player this message is going to. Might only be used for PLAYER_ACK message?
    private String playerName;

    public Message() {
    }
    
    public Message(MessageType type) {
        this.type = type;
    }
    
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getData(){
        return data;
    }

    public NewPlayerMessage getNewPlayer() {
        return newPlayer;
    }

    public void setNewPlayer(NewPlayerMessage newPlayer) {
        this.newPlayer = newPlayer;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }
    
    
 
}
