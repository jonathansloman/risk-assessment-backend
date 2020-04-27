package uk.co.risk.assessment.model;

import java.util.ArrayList;
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
    // leftover chips from previous hand due to split pots
    int leftover = 0;
    
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
    
    // shorthand for a common check
    private boolean isActive(Player p) {
        return p != null && !p.isPaused() && !p.isFolded();
    }
    
    private Player getPlayerFromTable(int i) {
        return getTable().getPlayers()[i];
    }
    
    /* prepares the deck, prepares the table, deals 2 cards to each active player */
    public void deal() {
        shuffle();
        getTable().nextHand(leftover);
        leftover = 0;
        for (int i = 0; i < Table.MAX_PLAYERS; i++) {
            Player p = getPlayerFromTable(i);
            if (isActive(p)) {
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
        String lowerCommand = command.toLowerCase();
        if ("sit".equals(lowerCommand)) {
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
        } else if ("deal".equals(lowerCommand)) {
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
        } else if ("call".equals(lowerCommand)) {
            if (!getTable().isNextToBet(playerName)) {
                return playerName + " tried to bet out of turn!";
            }
            // TODO handle affordability/split pots
            String result = getTable().call(getPlayerFromTable(getTable().getNextToBet()));
            return checkNextBetter(playerName, result);
        } else if ("check".equals(lowerCommand)) {
            if (!getTable().isNextToBet(playerName)) {
                return playerName + " tried to bet out of turn!";
            }
            Player p = getPlayerFromTable(getTable().getNextToBet());
            if (p.totalBet() != getTable().getCurrentBet()) {
                return playerName + " tried to check but can't!";
            }
            getPlayerFromTable(getTable().getNextToBet()).check();
            return checkNextBetter(playerName, " checked.");
        } else if (lowerCommand.startsWith("raise ")) {
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
            if (amount % table.getSmallBlind() != 0) {
                return playerName + " invalid raise, must be a multiple of small blind";
            }
            Player p = getPlayerFromTable(getTable().getNextToBet());
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
            
        } else if ("fold".equals(lowerCommand)) {
            if (!getTable().isNextToBet(playerName)) {
                return playerName + " tried to fold out of turn!";
            }
            getTable().foldPlayer();
            return checkNextBetter(playerName, " folded.");
        } else if ("buyin".equals(lowerCommand)) {
            Player player = playerDAO.getPlayer(playerName);
            if (player.getChips() > getTable().getMinimumBuyin()) {
                return playerName + " tried to buy in but has too many chips.";
            }
            if (getTable().getState() == TableState.PREDEAL || player.isPaused() || player.isFolded()) {
                player.buyIn();
                return playerName + " bought in.";
            } else {
                return playerName + " tried to buy in but is in a hand.";
            }
        } else if (lowerCommand.startsWith("setchips")) {
            // TODO for TESTING ONLY
            if (command.length() < 10) {
                return playerName + " invalid setchips";
            }
            int amount;
            try {
                amount = Integer.parseInt(command.substring(9));
            } catch (NumberFormatException e) {
                return playerName + " invalid setchips";
            }
            Player player = playerDAO.getPlayer(playerName);
            player.setChips(amount);
            return playerName = " set their chips to: " + amount;
        } else if (lowerCommand.startsWith("sethand")) {
            // TODO for TESTING ONLY
            if (command.length() != 12) {
                return playerName + " invalid sethand";
            }
            String cards = command.substring(8);
            for (int i = 0; i < Table.MAX_PLAYERS; i++) {
                Player p = getPlayerFromTable(i);
                if (p != null && p.getName().equals(playerName)) {
                    playerCards[i] = parseCards(cards.toUpperCase());
                    return playerName + " set their cards to: " + cards;
                }
            }
            
        }
        // just echo command back if we haven't dealt with it.
        return command;
    }
    
    // just used for testing card-setting - parse a string like 3DJS into three of diamonds, jack of spades
    private Card[] parseCards(String cardString) {
        Card[] cards = new Card[2];
        cards[0] = new Card(cardString.charAt(1), cardString.charAt(0));
        cards[1] = new Card(cardString.charAt(3), cardString.charAt(2));
        return cards;
    }
    
    /* Return message saying who is next to bet */
    private String getNextToBet() {
        return " Next to bet is: " + getPlayerFromTable(getTable().getNextToBet()).getName();
    }
    
    private String finishHand() {
        // work out who won what.
        String finishText = findWinners();
        getTable().setState(TableState.PREDEAL);
        getTable().nextDealer();
        getTable().setNextToBet(-1);
        return finishText;
    }
    
    /*
     * checks if betting round has finished and if so advances to next stage of game. Returns outcome to be sent to players.
     */
    private String checkNextBetter(String playerName, String actionResult) {
        if (getTable().remainingActivePlayers(false) <= 1) {
            // tidy bets into pots
            getTable().endBettingRound();
            // work out who won what.
            String finishText = finishHand();
            return playerName + actionResult + ". Hand finished. " + finishText;
        } else {
            boolean finishedRound = getTable().nextBetter();
            if (!finishedRound) {
                return playerName + actionResult + getNextToBet();
            } else {
                LOG.info("Round of betting finished, resolve next table state");
                String finishText = null;
                getTable().endBettingRound();
                // At this point, if everyone is all in, or all but one person are all in, we're actually finished
                if (getTable().remainingActivePlayers(true) <= 1) {
                    finishText = finishHand();
                    return playerName + actionResult + ". Hand finished. " + finishText;
                }
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
                        finishText = findWinners();
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
    
    private void dealRemainingCards() {
        switch (getTable().getState()) {
            case PREFLOP:
                for (int i = 0; i < 5; i++) {
                    getTable().getCards()[i] = dealCard();
                }
                break;
            case FLOP:
                getTable().getCards()[3] = dealCard();
                getTable().getCards()[4] = dealCard();
                break;
            case TURN:
                getTable().getCards()[4] = dealCard();
                break;
            case RIVER:
                break;
            default:
                LOG.warn("This should never happen, bad table state!");
                break;
        }
        getTable().setState(TableState.RIVER);
    }
    
    /* for each pot, work out who won what */
    private String findWinners() {
        // first check for the special case where only one player is left in and so wins everything
        Player winner = null;
        for (int i = 0; i < Table.MAX_PLAYERS; i++) {
            Player p = getPlayerFromTable(i);
            if (isActive(p)) {
                if (winner != null) {
                    // we have more than one person in
                    winner = null;
                    break;
                } else {
                    winner = p;
                }
            }
        }
        if (winner != null) {
            int winnings = 0;
            /* by definition this player must have been involved in all pots so don't need to check */
            for (int i = 0; i < getTable().getNumPots(); i++) {
                winnings += getTable().getPots()[i].getPot();
            }
            winner.addChips(winnings);
            return winner.getName() + " won " + winnings;
            
        }
        StringBuffer result = new StringBuffer();
        // we have more than one potential winner, so work out hands of all remaining players, so
        // we need to first deal any remaining cards.
        dealRemainingCards();
        // then work out the hands of all remaining players
        Hand[] hands = new Hand[Table.MAX_PLAYERS];
        for (int i = 0; i < Table.MAX_PLAYERS; i++) {
            Player p = getPlayerFromTable(i);
            if (isActive(p)) {
                hands[i] = new Hand(playerCards[i], getTable().getCards());
            }
        }
        // then for each pot, work out who the winners are
        for (int i = 0; i < getTable().getNumPots(); i++) {
            List<Integer> potWinners = new ArrayList<>();
            for (int j = 0; j < Table.MAX_PLAYERS; j++) {
                Player p = getTable().getPlayers()[j];
                if (isActive(p) && getTable().getPots()[i].getPlayers()[j]) {
                    if (potWinners.isEmpty()) {
                        potWinners.add(j);
                    } else {
                        int compare = hands[j].betterThan(hands[potWinners.get(0)]);
                        if (compare == 0) {
                            potWinners.add(j);
                        } else if (compare == 1) {
                            potWinners.clear();
                            potWinners.add(j);
                        }
                    }
                }
            }
            // work out how much each winner gets
            int numWinners = potWinners.size();
            if (numWinners > 0) {
                int toSplit = getTable().getPots()[i].getPot();
                while (toSplit % numWinners != 0) {
                    toSplit -= getTable().getSmallBlind();
                    leftover += getTable().getSmallBlind();
                }
                StringBuffer winningNames = new StringBuffer();
                for (int j = 0; j < numWinners; j++) {
                    Player p = getPlayerFromTable(potWinners.get(j));
                    winningNames.append(p.getName());
                    if (j < numWinners - 1) {
                        winningNames.append(", ");
                    }
                    p.addChips(toSplit / numWinners);
                }
                // clear down pot for display
                getTable().getPots()[i].setPot(0);
                result.append(
                        winningNames.toString() + " won " + (toSplit / numWinners) + " from pot "
                                + (i + 1) + " with " + hands[potWinners.get(0)].toString() + ". ");
            }
        }
        return result.toString();
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
