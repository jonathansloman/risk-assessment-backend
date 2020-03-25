package uk.co.risk.assessment.dao;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.risk.assessment.message.NewPlayerMessage;
import uk.co.risk.assessment.model.Player;

public class PlayerDAO {
    
    private static final Logger LOG = LoggerFactory.getLogger(PlayerDAO.class);

    private Map<String, String> passwords = new HashMap<>();
    private Map<String, Player> players = new HashMap<>();
    
    public Player newPlayer(NewPlayerMessage newPlayerMessage) {
        String password = passwords.get(newPlayerMessage.getName());
        if (password != null) {
            LOG.info("Got existing player for {} with password {}", newPlayerMessage.getName(), password );
            if (!password.equals(newPlayerMessage.getPassword())) {
                return null;
            } else {
                return players.get(newPlayerMessage.getName());
            }
        } else {
            passwords.put(newPlayerMessage.getName(), newPlayerMessage.getPassword());
            Player player = new Player(newPlayerMessage.getName());
            players.put(newPlayerMessage.getName(), player);
            return player;
        }
    }
    
    public Player getPlayer(String name) {
        return players.get(name);
    }
}
