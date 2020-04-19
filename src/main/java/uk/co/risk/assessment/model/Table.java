package uk.co.risk.assessment.model;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * All public details of a table.
 *
 */
public class Table {
    private static final Logger LOG = LoggerFactory.getLogger(Table.class);

    public final static int MAX_PLAYERS = 10;
    public final static int BUYIN = 500;
    
    Player players[] = new Player[MAX_PLAYERS];
    int dealer = -1;
    int nextToBet = -1;
    int currentBet;
    int lastRaise;
    int bigBlind = 10;
    int smallBlind = 5;
    int minimumBuyin = 100;
    Card[] cards = new Card[5];
    TableState state = TableState.PREDEAL;
    int numPots = 0;
    // how much each player has put in so far from previous betting rounds.
    int potLevel = 0;
    int maximumBet = 0;
    Pot[] pots = new Pot[MAX_PLAYERS];
    
    public Table() {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            pots[i] = new Pot();
        }
    }
    
    public Player[] getPlayers() {
        return players;
    }
    
    public int countPlayers() {
        int result = 0;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null) {
                result++;
            }
        }
        return result;
    }
    
    public int countActivePlayers() {
        int result = 0;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null && !players[i].isPaused()) {
                result++;
            }
        }
        return result;
    }
    
    // at this point we pause any players who don't have at least bigBlind chips.
    // returns null if we can deal, message saying why not if can't.
    public String checkCanDeal() {
        int result = 0;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player p = players[i];
            if (p != null) {
                if (p.getChips() < bigBlind) {
                    p.setPaused(true);
                }
                if (!p.isPaused()) {
                    result++;
                }
            }
        }
        if (result < 2) {
            return "Could not deal, not enough players.";
        }
        if (players[dealer].isPaused()) {
            dealer = findNextPlayer(dealer);
            return "Could not deal, not enough chips, skipping to next dealer.";
        }
        return null;
    }
    
    // returns the number of still active players. If this is 1 or lower, hand is finished.
    public int remainingActivePlayers(boolean countAllIn) {
        int count = 0;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player p = players[i];
            if (p != null && !p.isPaused() && !p.isFolded() && (!countAllIn  || !p.isAllIn())) {
                count++;
            }
        }
        return count;
    }
    
    /* prepare table for next hand, resetting everything */
    public void nextHand(int leftover) {
        cards = new Card[5];
        state = TableState.PREFLOP;
        pots[0].pot = leftover;
        leftover = 0;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player p = players[i];
            if (p != null && !p.isPaused()) {
                if (p.getChips() < bigBlind) {
                    p.setPaused(true);
                } else {
                    p.resetForNextHand();
                }
            }
        }
        int smallBlindIndex = findNextPlayer(dealer);
        int bigBlindIndex = findNextPlayer(smallBlindIndex);
        nextToBet = findNextPlayer(bigBlindIndex);
        initialisePots();
        processBlind(smallBlindIndex, smallBlind);
        processBlind(bigBlindIndex, bigBlind);
        
        currentBet = bigBlind;
        lastRaise = bigBlind;
    }
    
    public boolean nextBetter() {
        int next = findNextPlayer(nextToBet);
        if (next == nextToBet || players[next].isCheckedCalled()) {
            return true;
        } else {
            nextToBet = next;
            return false;
        }
    }
    
    public void nextDealer() {
        if (countActivePlayers() > 1) {
            do {
                dealer = (dealer + 1) % MAX_PLAYERS;
            } while (players[dealer] == null || players[dealer].isPaused());
        }
    }
    
    public int locatePlayer(String playerName) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null) {
                if (playerName.equals(players[i].getName())) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    // this is only used for blinds, so always in main pot
    private void processBlind(int player, int amount) {
        players[player].makeBet(0, amount);
    }
    
    /* locate the next player from a given position who is still in the game */
    private int findNextPlayer(int start) {
        int index = start + 1;
        while (start != index && (players[index] == null || players[index].isPaused()
                || players[index].isFolded() || players[index].isAllIn())) {
            index = (index + 1) % MAX_PLAYERS;
        }
        return index;
    }
    
    /* sit player at first available place */
    public void sitPlayer(Player p) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] == null) {
                players[i] = p;
                // TODO may be returning player who already has chips
                p.buyIn();
                if (dealer < 0) {
                    dealer = i;
                }
                break;
            }
        }
    }
    
    public boolean isSeated(String playerName) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null && players[i].getName().equals(playerName)) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isDealer(String playerName) {
        if (dealer < 0 || players[dealer] == null) {
            return false;
        }
        return playerName.equals(players[dealer].getName());
    }
    
    public boolean isNextToBet(String playerName) {
        if (nextToBet < 0 || players[nextToBet] == null) {
            return false;
        }
        return playerName.equals(players[nextToBet].getName());
    }
    
    public int getMinimumRaise() {
        return currentBet + lastRaise;
    }
    
    public void clearCheckedCalled(String playerName) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player p = players[i];
            if (p != null && !p.isPaused() && !p.isFolded() && !playerName.equals(p.getName())) {
                p.setCheckedCalled(false);
            }
        }
    }
    
    private void betsToPots(Player p) {
        for (int j = 0; j < MAX_PLAYERS; j++) {
            pots[j].pot += p.getBets()[j];
            p.setBet(j, 0);
        }
    }
    
    public void endBettingRound() {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player p = players[i];
            if (p != null && !p.isPaused() && !p.isFolded()) {
                p.setCheckedCalled(false);
                betsToPots(p);
            }
        }
        potLevel += currentBet;
        currentBet = 0;
        lastRaise = 10;
        nextToBet = findNextPlayer(dealer);
    }
    
    public void foldPlayer() {
        Player p = players[nextToBet];
        betsToPots(p);
        p.setFolded(true);
    }
    
    /* for each hand, we set up the possible required pots in advanced */
    private void initialisePots() {
        int[] chipsLeft = new int[MAX_PLAYERS];
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player p = players[i];
            if (p != null && !p.isPaused() && !p.isFolded() && p.getChips() > 0) {
                chipsLeft[i] = p.getChips();
            } else {
                chipsLeft[i] = 0;
            }
        }
        // sort the chipsLeft in increasing order
        Arrays.sort(chipsLeft);
        // now create a pot at each level of chips with active players set for it.
        numPots = 0;
        potLevel = 0;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            // we don't care about people who can't bet any more
            if (chipsLeft[i] == 0) {
                continue;
            }
            // if same as the previous, we don't need a separate pot
            if (i > 0 && chipsLeft[i] == chipsLeft[i - 1]) {
                continue;
            }
            pots[numPots].betLimit = chipsLeft[i];
            /* register all players who are in this pot */
            int count = 0;
            for (int j = 0; j < MAX_PLAYERS; j++) {
                Player p = players[j];
                if (p != null && !p.isPaused() && p.getChips() >= pots[numPots].betLimit) {
                    pots[numPots].players[j] = true;
                    count++;
                } else {
                    pots[numPots].players[j] = false;
                }
            }
            // if there was only one player in this pot, then we reset it and set the maximum bet
            // to be the level from the previous pot. A single player with more chips than anyone else
            // can't ever bet those extra chips.
            if (count == 1) {
                // not strictly necessary as this pot will never be used
                pots[numPots].betLimit = 0;
            } else {
                maximumBet = pots[numPots].betLimit;
                numPots++;
            }
        }
    }
    
    /* work out how many chips of bet go into each pot */
    private void makeBet(Player p, int amount) {
        LOG.info("Player {} making bet of amount {}", p.getName(), amount);
        int startLevel = potLevel + p.totalBet();
        for (int i = 0; i < numPots; i++) {
            /* ignore pots that we've already filled up */
            if (pots[i].betLimit < startLevel) {
                continue;
            }
            if (startLevel + amount <= pots[i].betLimit) {
                /* this is the final pot we need to put stuff into */
                LOG.info("Final bet of {} going into pot {}", amount, i);
                p.makeBet(i, amount);
                break;
            } else {
                int toFillPot = pots[i].betLimit - startLevel;
                p.makeBet(i, toFillPot);
                amount -= toFillPot;
                LOG.info("Filling pot {} with bet amount {} leaving {}", i, toFillPot, amount);
            }
        }      
    }
    
    public String call(Player p) {
        if (p.totalBet() == currentBet) {
            p.check();
            return " already bet " + currentBet + ", assuming check.";
        } else {
            int extra = currentBet - p.totalBet();
            if (extra >= p.getChips()) {
                p.setAllIn(true);
                extra = p.getChips();
            }
            makeBet(p, extra);
            p.setCheckedCalled(true);
            return " put in " + extra + " to call.";
        }
    }
    
    public String raise(Player p, int amount ) {
        if (amount + potLevel > maximumBet) {
            amount = maximumBet - potLevel;
            p.setAllIn(true);
        }
        int extra = amount - p.totalBet();
        if (extra == p.getChips()) {
            p.setAllIn(true);
            extra = p.getChips();
        }
        makeBet(p, extra);
        p.setCheckedCalled(true);
        currentBet = amount;
        return " put in " + extra + " to raise to " + amount + ".";
    }
    
    // below here getters/setters
    
    public void setPlayers(Player[] players) {
        this.players = players;
    }
    
    public int getDealer() {
        return dealer;
    }
    
    public void setDealer(int dealer) {
        this.dealer = dealer;
    }
    
    public int getNextToBet() {
        return nextToBet;
    }
    
    public void setNextToBet(int nextToBet) {
        this.nextToBet = nextToBet;
    }
    
    public int getCurrentBet() {
        return currentBet;
    }
    
    public void setCurrentBet(int currentBet) {
        this.currentBet = currentBet;
    }
    
    public int getBigBlind() {
        return bigBlind;
    }
    
    public void setBigBlind(int bigBlind) {
        this.bigBlind = bigBlind;
    }
    
    public int getSmallBlind() {
        return smallBlind;
    }
    
    public void setSmallBlind(int smallBlind) {
        this.smallBlind = smallBlind;
    }
    
    public Pot[] getPots() {
        return pots;
    }
    
    public void setPots(Pot[] pots) {
        this.pots = pots;
    }
    
    public int getLastRaise() {
        return lastRaise;
    }
    
    public void setLastRaise(int lastRaise) {
        this.lastRaise = lastRaise;
    }
    
    public Card[] getCards() {
        return cards;
    }
    
    public void setCards(Card[] cards) {
        this.cards = cards;
    }
    
    public TableState getState() {
        return state;
    }
    
    public void setState(TableState state) {
        this.state = state;
    }
    
    public int getNumPots() {
        return numPots;
    }

    public void setNumPots(int numPots) {
        this.numPots = numPots;
    }

    public int getPotLevel() {
        return potLevel;
    }

    public void setPotLevel(int potLevel) {
        this.potLevel = potLevel;
    }

    public int getMaximumBet() {
        return maximumBet;
    }

    public void setMaximumBet(int maximumBet) {
        this.maximumBet = maximumBet;
    }

    public int getMinimumBuyin() {
        return minimumBuyin;
    }

    public void setMinimumBuyin(int minimumBuyin) {
        this.minimumBuyin = minimumBuyin;
    }

    class Pot {
        int pot;
        /* maximum bet that can go into this pot, determined by still-in player with fewest chips */
        int betLimit;
        /* true for players who are involved in this pot */
        boolean[] players = new boolean[MAX_PLAYERS];
        
        
        public int getPot() {
            return pot;
        }
        public void setPot(int pot) {
            this.pot = pot;
        }
        public int getBetLimit() {
            return betLimit;
        }
        public void setBetLimit(int betLimit) {
            this.betLimit = betLimit;
        }
        public boolean[] getPlayers() {
            return players;
        }
        public void setPlayers(boolean[] players) {
            this.players = players;
        }
        
        
    }
}
