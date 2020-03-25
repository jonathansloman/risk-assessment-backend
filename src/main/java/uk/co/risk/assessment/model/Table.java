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
    int bigBlind = 10;
    int smallBlind = 5;
    int mainPot;
    Card[] cards = new Card[5];
    TableState state;
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

    public void nextHand(int leftover) {
        cards = new Card[5];
        state = TableState.PREDEAL;
        mainPot = leftover;
        sidePots = new ArrayList<>();
        int smallBlindIndex = findNextPlayer(dealer);
        int bigBlindIndex = findNextPlayer(smallBlindIndex);
        makeBet(smallBlindIndex, smallBlind);
        makeBet(bigBlindIndex, bigBlind);
        mainPot += bigBlind + smallBlind;
        if (countPlayers() > 1) {
            do {
                dealer++;
            } while (players[dealer] == null);
        }   
    }
    
    private void makeBet(int player, int amount) {
        players[player].makeBet(amount);
        mainPot += amount;
    }
    
    private int findNextPlayer(int start) {
        int index = start + 1;
        while (players[index] == null) {
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



    public int getBuyIn() {
        return buyIn;
    }



    public void setBuyIn(int buyIn) {
        this.buyIn = buyIn;
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

    class SidePot {
        int pot;
        List<Integer> players = new ArrayList<>();
    }
}
