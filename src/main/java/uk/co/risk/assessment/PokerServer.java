package uk.co.risk.assessment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.co.risk.assessment.dao.PlayerDAO;
import uk.co.risk.assessment.message.Message;
import uk.co.risk.assessment.message.MessageType;
import uk.co.risk.assessment.model.Game;
import uk.co.risk.assessment.model.Player;
import uk.co.risk.assessment.model.Table;

public class PokerServer extends WebSocketServer {
    
    private static final Logger LOG = LoggerFactory.getLogger(PokerServer.class);
    
    private HashMap<WebSocket, String> players;
    PlayerDAO playerDAO = new PlayerDAO();
    
    /* let's just have a single game for now */
    Game game;
    
    private Set<WebSocket> conns;
    
    ObjectMapper mapper = new ObjectMapper();
    
    private PokerServer(int port) {
        super(new InetSocketAddress(port));
        conns = new HashSet<>();
        players = new HashMap<>();
        game = new Game();
    }
    
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        conns.add(webSocket);
        
        LOG.info("Connection established from: {}, {}",
                webSocket.getRemoteSocketAddress().getHostString(),
                webSocket.getRemoteSocketAddress().getAddress().getHostAddress());
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        conns.remove(conn);
        // When connection is closed, remove the user.
        try {
            removePlayer(conn);
        } catch (JsonProcessingException e) {
            LOG.warn("Error removing user: ", e);
        }
        
        LOG.info("Connection closed to: {}, {} ", conn.getRemoteSocketAddress().getHostString(),
                conn.getRemoteSocketAddress().getAddress().getHostAddress());
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Message msg = mapper.readValue(message, Message.class);
            switch (msg.getType()) {
                case PLAYER_JOINED:
                    LOG.info("New player login: {}", msg.getNewPlayer().getName());
                    Player player = playerDAO.newPlayer(msg.getNewPlayer());
                    if (player == null) {
                        LOG.warn("Failed to join new player, bad password?");
                        Message m = new Message(MessageType.PLAYER_BADPASSWORD);
                        sendMessage(m, conn);
                        conn.close();
                    } else {
                        /* check for existing players with the same name and disconnect them */
                        for (WebSocket otherCon : conns) {
                            String playerName = players.get(otherCon);
                            if (playerName != null && playerName.equals(player.getName())) {
                                Message m = new Message(MessageType.PLAYER_OVERRIDDEN);
                                sendMessage(m, otherCon);
                                otherCon.close();
                            }
                        }
                        addPlayer(player.getName(), conn);
                    }
                    break;
                case PLAYER_LEFT:
                    LOG.info("Player left {}", players.get(conn));
                    removePlayer(conn);
                    break;
                case TEXT_MESSAGE:
                    LOG.info("Command message: {} from {}", msg, players.get(conn));
                    handleCommand(players.get(conn), msg.getData());
                    Message newMessage = new Message(MessageType.TEXT_MESSAGE);
                    newMessage.setData(msg.getData());
                    newMessage.setTable(game.getTable());
                    newMessage.setPlayerName(players.get(conn));
                    broadcastMessage(newMessage);
                    break;
                default:
                    LOG.warn("Invalid message received: {}", message);
            }
            

        } catch (IOException e) {
            LOG.error("Wrong message format.", e);
            // return error message to user
        }
    }
    
    private void handleCommand(String playerName, String command) {
        if ("sit".equals(command)) {
            if (game.getTable().countPlayers() < Table.MAX_PLAYERS && !game.getTable().isSeated(playerName)) {
                Player player = playerDAO.getPlayer(playerName);
                if (player == null) {
                    LOG.warn("Couldn't find player {} - panic!", playerName);
                }
                game.getTable().sitPlayer(player);
            } else {
                LOG.warn("Could not seat player {} - either no space or already seated.", playerName);
            }
        }
        if ("deal".equals(command)) {
            if (game.getTable().countPlayers() < 2) {
                LOG.warn("Could not deal, not enough players");
            } else if (!game.getTable().isDealer(playerName)) {
                LOG.warn("Could not deal, not dealer");
            } else {
                game.deal();
            }
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        
        if (conn != null) {
            LOG.error("Error on connection {}",
                    conn.getRemoteSocketAddress().getAddress().getHostAddress(), ex);
            conns.remove(conn);
        } else {
            LOG.error("Error with null connection", ex);
        }
    }
    
    private void broadcastMessage(Message msg) {
        try {
            String messageJson = mapper.writeValueAsString(msg);
            LOG.info("Sending broadcast message: {}", messageJson);
            for (WebSocket con : conns) {
                con.send(messageJson);
            }
        } catch (JsonProcessingException e) {
            LOG.error("Cannot convert message to json.");
        }
    }
    
    private void sendMessage(Message msg, WebSocket con) {
        try {
            String messageJson = mapper.writeValueAsString(msg);
            LOG.info("Sending message to user: {}, {}", players.get(con), messageJson);
            con.send(messageJson);
        } catch (JsonProcessingException e) {
            LOG.error("Cannot convert message to json.");
        }
    }
    
    private void addPlayer(String playerName, WebSocket conn) throws JsonProcessingException {
        players.put(conn, playerName);
        acknowledgePlayerJoined(playerDAO.getPlayer(playerName), conn);
        broadcastUserActivityMessage(MessageType.PLAYER_JOINED);
    }
    
    private void removePlayer(WebSocket conn) throws JsonProcessingException {
        players.remove(conn);
        broadcastUserActivityMessage(MessageType.PLAYER_LEFT);
    }
    
    private void acknowledgePlayerJoined(Player player, WebSocket conn)
            throws JsonProcessingException {
        Message message = new Message(MessageType.PLAYER_JOINED_ACK);
        message.setPlayerName(player.getName());
        conn.send(mapper.writeValueAsString(message));
    }
    
    private void broadcastUserActivityMessage(MessageType messageType)
            throws JsonProcessingException {
        
        Message newMessage = new Message(messageType);
        newMessage.setTable(game.getTable());
        broadcastMessage(newMessage);
    }
    
    public static void main(String[] args) {
        int port;
        try {
            port = Integer.parseInt(System.getenv("PORT"));
        } catch (NumberFormatException nfe) {
            port = 3001;
        }
        LOG.info("Starting on port: " + port);
        new PokerServer(port).start();
    }
    
    @Override
    public void onStart() {
        LOG.info("Started");
    }
    
}