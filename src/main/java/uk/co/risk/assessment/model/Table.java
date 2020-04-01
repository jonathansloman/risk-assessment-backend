package uk.co.risk.assessment.model;

import java.util.Arrays;

/**
 * All public details of a table.
 *
 */
public class Table { 
    public final static int MAX_PLAYERS = 10;
    public final static int BUYIN = 500;
    
    Player players[] = new Player[MAX_PLAYERS];
    int dealer = -1;
    int nextToBet = -1;
    int currentBet;
    int lastRaise;
    int bigBlind = 10;
    int smallBlind = 5;
    Card[] cards = new Card[5];
    TableState state = TableState.PREDEAL;
    int currentPot = 0;
    int numPots = 0;
    Pot[] pots  = new Pot[MAX_PLAYERS];
    
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
    
    // returns the id of the only remaining non-foldedplayer, or -1 if 2 or more people sitll in
    public int checkAllFolded() {
        int result = -1;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player p = players[i];
            if (p != null && !p.isPaused() && !p.isFolded()) {
                if (result == -1) {
                    result = i;
                } else {
                    return -1;
                }
            }
        }
        return result;
    }

    public void nextHand(int leftover) {
        cards = new Card[5];
        state = TableState.PREFLOP;
        pots[0].pot = leftover;
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
        makeBet(smallBlindIndex, smallBlind);
        makeBet(bigBlindIndex, bigBlind);
        
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
    private void makeBet(int player, int amount) {
        players[player].makeBet(0, amount);
    }
    
    private int findNextPlayer(int start) {
        int index = start + 1;
        while (start != index && players[index] == null || players[index].isPaused() || players[index].isFolded()) {
            index = (index + 1) % MAX_PLAYERS;
        }
        return index;
    }
    
    /* sit player at first available place */
    public void sitPlayer(Player p) {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] == null) {
                players[i] = p;
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
        currentBet = 0;
        lastRaise = 10; 
        nextToBet = findNextPlayer(dealer);
    }
    
    public void foldPlayer() {
        Player p = players[nextToBet];
        betsToPots(p);
        p.setFolded(true);
    }
    
    public String finishHand(int winner) {
  //      players[winner].addChips(mainPot);
        nextDealer();
        return players[winner].getName() + " won " + /* mainPot + */ " chips. Dealer is now " + players[dealer].getName();
    }
    
    /* for each hand, we set up the possible required pots in advanced */
    public void initialisePots() {
        int[] chipsLeft = new int[MAX_PLAYERS];
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player p = players[i];
            if (p != null && !p.isPaused() && !p.isFolded() && p.getChips() > 0 ) {
                chipsLeft[i] = p.getChips();
            } else {
                chipsLeft[i] = 0;
            }
        }
        // sort the chipsLeft in increasing order
        Arrays.sort(chipsLeft);
        // now create a pot at each level of chips with active players set for it.
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
            for (int j = 0; j < MAX_PLAYERS; j++) {
                if (players[j].getChips() >= pots[numPots].betLimit) {
                    pots[numPots].players[j] = true;
                } else {
                    pots[numPots].players[j] = false;
                }
            }
            numPots++;
        }
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

    class Pot {
        int pot;
        /* maximum bet that can go into this pot, determined by still-in player with fewest chips */
        int betLimit;
        /* true for players who are involved in this pot */
        boolean[] players = new boolean[MAX_PLAYERS];
    }
}
