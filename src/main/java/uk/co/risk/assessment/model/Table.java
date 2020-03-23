package uk.co.risk.assessment.model;

import java.util.ArrayList;
import java.util.List;

public class Table {
    public final static int MAX_PLAYERS = 10;
    Player players[] = new Player[MAX_PLAYERS];
    
    int dealer = -1;
    int nextToBet = -1;
    int currentBet;
    int bigBlind = 10;
    int smallBlind = 5;
    int buyIn = 500;
    int mainPot;
    List<SidePot> sidePots = new ArrayList<>();
    int[] deck = new int[52];
    
    public Player[] getPlayers() {
        return players;
    }



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



    public int[] getDeck() {
        return deck;
    }



    public void setDeck(int[] deck) {
        this.deck = deck;
    }



    class SidePot {
        int pot;
        List<Integer> players = new ArrayList<>();
    }
}
