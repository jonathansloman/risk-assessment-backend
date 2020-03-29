package uk.co.risk.assessment.model;

import java.util.ArrayList;
import java.util.List;

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
    int mainPot;
    Card[] cards = new Card[5];
    TableState state = TableState.PREDEAL;
    List<SidePot> sidePots = new ArrayList<>();
    
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
            Player p = players[i];
            if (p != null && !p.isPaused() && p.getChips() >= bigBlind) {
                result++;
            }
        }
        return result;
    }

    public void nextHand(int leftover) {
        cards = new Card[5];
        state = TableState.PREFLOP;
        mainPot = leftover;
        sidePots = new ArrayList<>();
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
        if (countPlayers() > 1) {
            do {
                dealer++;
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
    
    private void makeBet(int player, int amount) {
        players[player].makeBet(amount);
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
    
    public void endBettingRound() {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player p = players[i];
            if (p != null && !p.isPaused() && !p.isFolded()) {
               p.setCheckedCalled(false);
               mainPot += p.getBet();
               // TODO handle sidepots

               p.setBet(0);
            }
        }
        currentBet = 0;
        lastRaise = 10; 
        nextToBet = findNextPlayer(dealer);
    }
    
    public String finishHand() {
        int winner = dealer; // TODO HACK for now
        players[winner].addChips(mainPot);
        nextDealer();
        return players[winner].getName() + " won " + mainPot + " chips. Dealer is now " + players[dealer].getName();
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

    public int getMainPot() {
        return mainPot;
    }



    public void setMainPot(int mainPot) {
        this.mainPot = mainPot;
    }



    public List<SidePot> getSidePots() {
        return sidePots;
    }



    public void setSidePots(List<SidePot> sidePots) {
        this.sidePots = sidePots;
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



    class SidePot {
        int pot;
        List<Integer> players = new ArrayList<>();
    }
}
