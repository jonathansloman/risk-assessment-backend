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

import uk.co.risk.assessment.message.Message;
import uk.co.risk.assessment.message.MessageType;
import uk.co.risk.assessment.model.Game;
import uk.co.risk.assessment.model.Player;

public class PokerServer extends WebSocketServer {
    
    private static final Logger logger = LoggerFactory.getLogger(PokerServer.class);
    
    private HashMap<WebSocket, Player> players;
    
    /* let's just have a single game for now */
    Game game;
    
    private Set<WebSocket> conns;
    
    private PokerServer(int port) {
        super(new InetSocketAddress(port));
        conns = new HashSet<>();
        players = new HashMap<>();
        game = new Game();
    }
    
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        conns.add(webSocket);
        
        logger.info("Connection established from: {}, {}",
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
            logger.warn("Error removing user: ", e);
        }
        
        logger.info("Connection closed to: {}, {} ", conn.getRemoteSocketAddress().getHostString(),
                conn.getRemoteSocketAddress().getAddress().getHostAddress());
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            Message msg = mapper.readValue(message, Message.class);
            
            switch (msg.getType()) {
                case PLAYER_JOINED:
                    addPlayer(new Player(msg.getPlayer().getName()), conn);
                    break;
                case PLAYER_LEFT:
                    removePlayer(conn);
                    break;
                case TEXT_MESSAGE:
                    broadcastMessage(msg);
            }
            
            logger.info("Message from player: {}, type: {}, text: {}", msg.getPlayer(), msg.getType(), msg.getData());
        } catch (IOException e) {
            logger.error("Wrong message format.", e);
            // return error message to user
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        
        if (conn != null) {
            logger.error("Error on connection {}", conn.getRemoteSocketAddress().getAddress().getHostAddress(), ex);
            conns.remove(conn);
        } else {
            logger.error("Error with null connection", ex);
        }
    }
    
    private void broadcastMessage(Message msg) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String messageJson = mapper.writeValueAsString(msg);
            for (WebSocket sock : conns) {
                sock.send(messageJson);
            }
        } catch (JsonProcessingException e) {
            logger.error("Cannot convert message to json.");
        }
    }
    
    private void addPlayer(Player player, WebSocket conn) throws JsonProcessingException {
        players.put(conn, player);
        acknowledgePlayerJoined(player, conn);
        broadcastUserActivityMessage(MessageType.PLAYER_JOINED);
    }
    
    private void removePlayer(WebSocket conn) throws JsonProcessingException {
        players.remove(conn);
        broadcastUserActivityMessage(MessageType.PLAYER_LEFT);
    }
    
    private void acknowledgePlayerJoined(Player player, WebSocket conn) throws JsonProcessingException {
        Message message = new Message();
        message.setType(MessageType.PLAYER_JOINED_ACK);
        message.setPlayer(player);
        conn.send(new ObjectMapper().writeValueAsString(message));
    }
    
    private void broadcastUserActivityMessage(MessageType messageType)
            throws JsonProcessingException {
        
        Message newMessage = new Message();
        
        ObjectMapper mapper = new ObjectMapper();
        String data = mapper.writeValueAsString(players.values());
        newMessage.setData(data);
        newMessage.setType(messageType);
        broadcastMessage(newMessage);
    }
    
    public static void main(String[] args) {
        int port;
        try {
            port = Integer.parseInt(System.getenv("PORT"));
        } catch (NumberFormatException nfe) {
            port = 3001;
        }
        logger.info("Starting on port: " + port);
        new PokerServer(port).start();
    }
    
    @Override
    public void onStart() {
        logger.info("Started");
    }
    
}
