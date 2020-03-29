package uk.co.risk.assessment.model;

/**
 * All public details of a player.
 *
 */
public class Player {
    String name;
    int chips;
    int buyIns;
    int bet = 0;;
    boolean checkedCalled = false;
    boolean folded = false;
    boolean paused = false;
    
    public Player(String name) {
        this.name = name;
    }
    
    public Player() {
        
    }
    
    public void makeBet(int amount) {
        bet += amount;
        chips -= amount;
    }
    
    public void addChips(int amount) {
        chips += amount;
    }
    
    public void buyIn() {
        buyIns++;
        chips += Table.BUYIN;
    }
    
    public String call(int currentBet) {
        if (bet == currentBet) {
            check();
            return " already bet " + currentBet + ", assuming check.";
        } else {
            int extra = currentBet - bet;
            makeBet(extra);
            checkedCalled = true;
            return " put in " + extra + " to call.";
        }
    }
    
    public void check() {
        checkedCalled = true;
    }
    
    public String raise(int currentBet, int raise) {
        int extraTable = raise - currentBet;
        int extraPlayer = raise - bet;
        makeBet(extraPlayer);
        checkedCalled = true;
        return " put in " + extraPlayer + " to raise by " + extraTable + " to " + raise;
    }
    
    public void resetForNextHand() {
        checkedCalled = false;
        folded = false;
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
