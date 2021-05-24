package uk.co.risk.assessment;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Paths;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.Undertow;
import io.undertow.server.handlers.resource.PathResourceManager;

import static io.undertow.Handlers.resource;

import uk.co.risk.assessment.dao.PlayerDAO;
import uk.co.risk.assessment.message.Message;
import uk.co.risk.assessment.message.MessageType;
import uk.co.risk.assessment.model.Game;
import uk.co.risk.assessment.model.Player;

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
        game = new Game(playerDAO);
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
        
        LOG.info("Connection closed to: {} ", conn.getRemoteSocketAddress() == null ? "no connection" : conn.getRemoteSocketAddress().getHostString()
              /*  conn.getRemoteSocketAddress().getAddress().getHostAddress()*/);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        LOG.info("In onMessage, got: {}", message);
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
                    msg.setData(game.handleCommand(players.get(conn), msg.getData()));
                    updateGameState(players.get(conn), msg.getData());
                    break;
                default:
                    LOG.warn("Invalid message received: {}", message);
            }
            
        } catch (IOException e) {
            LOG.error("Wrong message format.", e);
            // return error message to user
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        
        if (conn != null) {
            LOG.error("Error on connection {}",
                    (conn != null && conn.getRemoteSocketAddress() != null && conn.getRemoteSocketAddress().getAddress() != null) ? 
                            conn.getRemoteSocketAddress().getAddress().getHostAddress() : "no address", ex);
            conns.remove(conn);
        } else {
            LOG.error("Error with null connection", ex);
            System.exit(-1);
        }
    }
    
    private void updateGameState(String playerName, String message) {
        Message newMessage = new Message(MessageType.TEXT_MESSAGE);
        newMessage.setData(message);
        newMessage.setTable(game.getTable());
        newMessage.setPlayerName(playerName);
        for (WebSocket con : conns) {
            String thisPlayer = players.get(con);
            if (thisPlayer != null) {
                newMessage.setCards(game.getCardsFor(thisPlayer));
                String messageJson;
                try {
                    messageJson = mapper.writeValueAsString(newMessage);
                    LOG.info("Sending to player {} message {}", thisPlayer, messageJson);
                    con.send(messageJson);
                } catch (JsonProcessingException e) {
                    LOG.error("Cannot convert message to json.", e);
                }
            }
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
        updateGameState(playerName, "Joined game");
    }
    
    private void removePlayer(WebSocket conn) throws JsonProcessingException {
        String playerName = players.get(conn);
        Player p = playerDAO.getPlayer(playerName);
        String ret;
        if (p != null && game.getTable().locatePlayer(playerName) != -1) {
            ret = game.playerLeft(playerName);
        } else {
            ret = "Left game";
        }
        players.remove(conn);
        updateGameState(playerName, ret);
    }
    
    private void acknowledgePlayerJoined(Player player, WebSocket conn)
            throws JsonProcessingException {
        Message message = new Message(MessageType.PLAYER_JOINED_ACK);
        message.setPlayerName(player.getName());
        conn.send(mapper.writeValueAsString(message));
    }
    
    public static void main(String[] args) {
        int port;
        try {
            port = Integer.parseInt(System.getenv("PORT"));
        } catch (NumberFormatException nfe) {
            port = 3001;
        }
        try {
            LOG.info("Starting websocket server on port: " + port);
            new PokerServer(port).start();
            LOG.info("Starting undertow for static content on port 8081");
            Undertow server = Undertow.builder().addHttpListener(8081, "0.0.0.0").setHandler(resource(new PathResourceManager(Paths.get("www"), 100))).build();
            server.start();
        } catch (Exception e) {
            LOG.error("Failed to start up, exiting", e);
            System.exit(-1);
        }
    }
    
    @Override
    public void onStart() {
        LOG.info("Started");
    }
    
}
