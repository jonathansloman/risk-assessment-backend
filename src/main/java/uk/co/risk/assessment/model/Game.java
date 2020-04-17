package uk.co.risk.assessment.model;

import java.util.Arrays;
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
    Card[][] playerCards = new Card[Table.MAX_PLAYERS][2];
    Random random;
    
    PlayerDAO playerDAO;
    
    public Game(PlayerDAO playerDAO) {
        this.playerDAO = playerDAO;
        table = new Table();
        random = new Random();
    }
    
    /* aces are high other than low straights, so 2 to 14 rather than 1 to 13. */
    private void shuffle() {
        deck = new LinkedList<>();
        for (Suit suit : Suit.values()) {
            for (int value = 2; value <= 14; value++) {
                deck.add(new Card(suit, value));
            }
        }
    }
    
    /* returns a random card, removing it from the list of remaining cards */
    public Card dealCard() {
        int choice = random.nextInt(deck.size());
        Card card = deck.get(choice);
        deck.remove(choice);
        return card;
    }
    
    /* prepares the deck, prepares the table, deals 2 cards to each active player */
    public void deal() {
        shuffle();
        table.nextHand(0);
        for (int i = 0; i < Table.MAX_PLAYERS; i++) {
            Player p = table.getPlayers()[i];
            if (p != null && !p.isPaused()) {
                playerCards[i][0] = dealCard();
                playerCards[i][1] = dealCard();
            }
        }
    }
    
    /* retrieve the cards for a player based on name */
    public Card[] getCardsFor(String thisPlayer) {
        int location = table.locatePlayer(thisPlayer);
        if (location != -1) {
            return playerCards[location];
        }
        return null;
    }
    
    /* deal with player input */
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
                return playerName + " could not sit, no space or already seated.";
            }
        } else if ("deal".equals(command)) {
            if (getTable().getState() != TableState.PREDEAL) {
                return playerName + " tried to deal, game in progress!";
            }
            if (!getTable().isDealer(playerName)) {
                return playerName + " could not deal, not dealer";
            } else {
                String dealAttempt = getTable().checkCanDeal();
                if (dealAttempt != null) {
                    return dealAttempt;
                } else {
                    deal();
                    return playerName + " dealt." + getNextToBet();
                }
            }
        } else if ("call".equals(command)) {
            if (!getTable().isNextToBet(playerName)) {
                return playerName + " tried to bet out of turn!";
            }
            // TODO handle affordability/split pots
            String result = getTable().call(getTable().getPlayers()[getTable().getNextToBet()]);
            return checkNextBetter(playerName, result);
        } else if ("check".equals(command)) {
            if (!getTable().isNextToBet(playerName)) {
                return playerName + " tried to bet out of turn!";
            }
            Player p = getTable().getPlayers()[getTable().getNextToBet()];
            if (p.totalBet() != getTable().getCurrentBet()) {
                return playerName + " tried to check but can't!";
            }
            getTable().getPlayers()[getTable().getNextToBet()].check();
            return checkNextBetter(playerName, " checked.");
        } else if (command.startsWith("raise ")) {
            if (!getTable().isNextToBet(playerName)) {
                return playerName + " tried to bet out of turn!";
            }
            if (command.length() < 7) {
                return playerName + " invalid raise";
            }
            int amount;
            try {
                amount = Integer.parseInt(command.substring(6));
            } catch (NumberFormatException e) {
                return playerName + " invalid raise";
            }
            Player p = getTable().getPlayers()[getTable().getNextToBet()];
            // allow for all in raise even if under raise limit.
            if (amount < getTable().getMinimumRaise() && amount != p.getChips() - p.totalBet()) {
                return playerName + " tried to raise too little - minimum is: "
                        + getTable().getMinimumRaise();
            }
            if (p.getChips() - p.totalBet() < amount) {
                return playerName + " tried to raise by more chips than they have!";
            }
            String result = getTable().raise(p, amount);
            getTable().clearCheckedCalled(playerName);
            getTable().setCurrentBet(amount);
            return checkNextBetter(playerName, result);
            
        } else if ("fold".equals(command)) {
            if (!getTable().isNextToBet(playerName)) {
                return playerName + " tried to fold out of turn!";
            }
            getTable().foldPlayer();
            return checkNextBetter(playerName, " folded.");
        } else if (command.startsWith("setchips")) {
            if (command.length() < 10) {
                return playerName + " invalid setchips";
            }
            int amount;
            try {
                amount = Integer.parseInt(command.substring(9));
            } catch (NumberFormatException e) {
                return playerName + " invalid setchips";
            }
            // TODO for TESTING ONLY
            Player player = playerDAO.getPlayer(playerName);
            player.setChips(amount);
            return playerName = " set their chips to: " + amount;
        }
        // just echo command back if we haven't dealt with it.
        return command;
    }
    
    /* Return message saying who is next to bet */
    private String getNextToBet() {
        return " Next to bet is: " + getTable().getPlayers()[getTable().getNextToBet()].getName();
    }
    
    /*
     * checks if betting round has finished and if so advances to next stage of game. Returns outcome to be sent to players.
     */
    private String checkNextBetter(String playerName, String actionResult) {
        int winner = getTable().checkAllFolded();
        if (winner >= 0) {
            getTable().endBettingRound();
            String finishText = getTable().finishHand(winner);
            getTable().setState(TableState.PREDEAL);
            return playerName + actionResult + " No other players left." + finishText;
        } else {
            boolean finishedRound = getTable().nextBetter();
            if (!finishedRound) {
                return playerName + actionResult + getNextToBet();
            } else {
                LOG.info("Round of betting finished, resolve next table state");
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
                        finishText = getTable().finishHand(getWinner());
                        getTable().setState(TableState.PREDEAL);
                        break;
                    default:
                        LOG.warn("This should never happen, bad table state!");
                        break;
                }
                return playerName + actionResult + " Betting round finished."
                        + (finishText == null ? getNextToBet() : finishText);
            }
        }
    }
    
    public int getWinner() {
        Hand[] hands = new Hand[Table.MAX_PLAYERS];
        int winner = -1;
        for (int i = 0; i < Table.MAX_PLAYERS; i++) {
            Player p = getTable().getPlayers()[i];
            if (p != null && !p.isPaused() && !p.isFolded()) {
                hands[i] = new Hand(playerCards[i], getTable().getCards());
                if (winner == -1) {
                    winner = i;
                } else {
                    // TODO - handle ties with multiple winners.
                    if (hands[i].betterThan(hands[winner]) > 0) {
                        winner = i;
                    }
                }
            }
        }
        LOG.info("Winning hand is {} {}", hands[winner].getHandType().getDescription(),
                Arrays.toString(hands[winner].getOrdinals()));
        return winner;
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
        return playerCards;
    }
    
    public void setHands(Card[][] hands) {
        this.playerCards = hands;
    }
    
}
