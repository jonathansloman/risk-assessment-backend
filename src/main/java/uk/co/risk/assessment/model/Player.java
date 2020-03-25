package uk.co.risk.assessment.model;

/**
 * All public details of a player.
 *
 */
public class Player {
    String name;
    int chips;
    int buyIns;
    int bet;
    boolean checkedCalled;
    boolean folded;
    boolean paused;
    
    public Player(String name) {
        this.name = name;
    }
    
    public Player() {
        
    }

    public void makeBet(int amount) {
        bet = amount;
        chips -= amount;
    }
    
    public void buyIn() {
        buyIns++;
        chips += Table.BUYIN;
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

    public int getBet() {
        return bet;
    }

    public void setBet(int bet) {
        this.bet = bet;
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
    
    
}
