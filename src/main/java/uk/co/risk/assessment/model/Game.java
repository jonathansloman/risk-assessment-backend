package uk.co.risk.assessment.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.risk.assessment.dao.PlayerDAO;

/**
 * Encapsulating class for whole game including private information.
 *
 */
public class Game {
    private static final Logger LOG = LoggerFactory.getLogger(Game.class);
    
    Table table;
    List<Card> deck;
    Card[][] hands = new Card[Table.MAX_PLAYERS][2];
    Random random;
    
    PlayerDAO playerDAO;
    
    public Game(PlayerDAO playerDAO) {
        this.playerDAO = playerDAO;
        table = new Table();
        random = new Random();
    }
    
    private void shuffle() {
        deck = new LinkedList<>();
        for (Suit suit : Suit.values()) {
            for (int value = 1; value <= 13; value++) {
                deck.add(new Card(suit, value));
            }
        }
    }
    
    public Card dealCard() {
        int choice = random.nextInt(deck.size());
        Card card = deck.get(choice);
        deck.remove(choice);
        return card;
    }
    
    public void deal() {
        shuffle();
        table.nextHand(0);
        for (int i = 0; i < Table.MAX_PLAYERS; i++) {
            if (table.getPlayers()[i] != null) {
                hands[i][0] = dealCard();
                hands[i][1] = dealCard();
                LOG.info("Dealt hand: {}, {} to player {}", hands[i][0], hands[i][1],
                        table.getPlayers()[i].getName());
            }
        }
    }
    
    public Card[] getCardsFor(String thisPlayer) {
        int location = table.locatePlayer(thisPlayer);
        LOG.info("Got location {} for player {}", location, thisPlayer);
        if (location != -1) {
            return hands[location];
        }
        return null;
    }
    
    public synchronized String handleCommand(String playerName, String command) {
        if ("sit".equals(command)) {
            if (getTable().countPlayers() < Table.MAX_PLAYERS && !getTable().isSeated(playerName)) {
                Player player = playerDAO.getPlayer(playerName);
                if (player == null) {
                    LOG.warn("Couldn't find player {} - panic!", playerName);
                    return playerName + " could not sit";
                }
                getTable().sitPlayer(player);
                return playerName + " sat at table";
            } else {
                LOG.warn("Could not seat player {} - either no space or already seated.",
                        playerName);
                return playerName + " could not sit, no space or already seated.";
            }
        } else if ("deal".equals(command)) {
            if (getTable().getState() != TableState.PREDEAL) {
                return playerName + " tried to deal, game in progress!";
            } else if (getTable().countActivePlayers() < 2) {
                LOG.warn("Could not deal, not enough players");
                return "Could not deal, need at least 2 players";
            } else if (!getTable().isDealer(playerName)) {
                LOG.warn("Could not deal, not dealer");
                return playerName + " could not deal, not dealer";
            } else {
                deal();
                return playerName + " dealt." + getNextToBet();
            }
        } else if ("call".equals(command)) {
            if (!getTable().isNextToBet(playerName)) {
                return playerName + " tried to bet out of turn!";
            }
            // TODO handle affordability/split pots
            String result = getTable().getPlayers()[getTable().getNextToBet()]
                    .call(getTable().getCurrentBet());
            return checkNextBetter(playerName, result);
        } else if ("check".equals(command)) {
            if (!getTable().isNextToBet(playerName)) {
                return playerName + " tried to bet out of turn!";
            }
            Player p = getTable().getPlayers()[getTable().getNextToBet()];
            if (p.getBet() != getTable().getCurrentBet()) {
                return playerName + " tried to check but can't!";
            }
            getTable().getPlayers()[getTable().getNextToBet()].check();
            return checkNextBetter(playerName, " checked.");
        } else if (command.startsWith("raise ")) {
            if (!getTable().isNextToBet(playerName)) {
                return playerName + " tried to bet out of turn!";
            }
            int amount = Integer.parseInt(command.substring(6));
            if (amount < getTable().getMinimumRaise()) {
                return playerName + " tried to raise too little - minimum is: "
                        + getTable().getMinimumRaise();
            }
            // TODO handle affordability/split pots
            String result = getTable().getPlayers()[getTable().getNextToBet()]
                    .raise(getTable().getCurrentBet(), amount);
            getTable().clearCheckedCalled(playerName);
            getTable().setCurrentBet(amount);
            return checkNextBetter(playerName, result);
            
        }
        // just echo command back if we haven't dealt with it.
        return command;
    }
    
    private String getNextToBet() {
        return " Next to bet is: " + getTable().getPlayers()[getTable().getNextToBet()].getName();
    }
    
    private String checkNextBetter(String playerName, String actionResult) {
        boolean finishedRound = getTable().nextBetter();
        if (!finishedRound) {
            return playerName + actionResult + getNextToBet();
        } else {
            String finishText = null;
            getTable().endBettingRound();
            switch (getTable().getState()) {
                case PREFLOP:
                    for (int i = 0; i < 3; i++) {
                        getTable().getCards()[i] = dealCard();
                    }
                    getTable().setState(TableState.FLOP);
                    break;
                case FLOP:
                    getTable().getCards()[3] = dealCard();
                    getTable().setState(TableState.TURN);
                    break;
                case TURN:
                    getTable().setState(TableState.RIVER);
                    getTable().getCards()[4] = dealCard();
                    break;
                case RIVER:
                    finishText = getTable().finishHand();
                    getTable().setState(TableState.PREDEAL);
                    break;
                default:
                    LOG.warn("This should never happen, bad table state!");
                    break;
            }
            return playerName + actionResult + " Betting round finished." + finishText == null ? getNextToBet() : finishText;
            
        }
    }
    
    public Table getTable() {
        return table;
    }
    
    public void setTable(Table table) {
        this.table = table;
    }
    
    public List<Card> getDeck() {
        return deck;
    }
    
    public void setDeck(List<Card> deck) {
        this.deck = deck;
    }
    
    public Card[][] getHands() {
        return hands;
    }
    
    public void setHands(Card[][] hands) {
        this.hands = hands;
    }
    
}
