package uk.co.risk.assessment.model;

/**
 * All public details of a player.
 *
 */
public class Player {
    String name;
    int chips;
    int buyIns;
    // we need an array of bets because of side-pots. 0 is the main pot.
    int currentPot = 0;
    int[] bets = new int[Table.MAX_PLAYERS];
    boolean checkedCalled = false;
    boolean folded = false;
    boolean paused = false;
    boolean allIn = false;
    
    public Player(String name) {
        this.name = name;
    }
    
    public Player() {
        
    }
    
    public void makeBet(int pot, int amount) {
        bets[pot] += amount;
        chips -= amount;
    }
    
    public void addChips(int amount) {
        chips += amount;
    }
    
    public void buyIn() {
        buyIns++;
        chips += Table.BUYIN;
    }
    
    public void check() {
        checkedCalled = true;
    }
    
    public void resetForNextHand() {
        checkedCalled = false;
        folded = false;
        allIn = false;
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getChips() {
        return chips;
    }

    public void setChips(int chips) {
        this.chips = chips;
    }

    public int getBuyIns() {
        return buyIns;
    }

    public void setBuyIns(int buyIns) {
        this.buyIns = buyIns;
    }

    public int[] getBets() {
        return bets;
    }

    public void setBets(int[] bets) {
        this.bets = bets;
    }
    
    public void setBet(int pot, int amount) {
        this.bets[pot] = amount;
    }
    
    /* total amount we've bet so far */
    public int totalBet() {
        int total = 0;
        for (int i = 0; i < Table.MAX_PLAYERS; i++) {
            total += bets[i];
        }
        return total;
    }

    public boolean isCheckedCalled() {
        return checkedCalled;
    }

    public void setCheckedCalled(boolean checkedCalled) {
        this.checkedCalled = checkedCalled;
    }

    public boolean isFolded() {
        return folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isAllIn() {
        return allIn;
    }

    public void setAllIn(boolean allIn) {
        this.allIn = allIn;
    }
}
