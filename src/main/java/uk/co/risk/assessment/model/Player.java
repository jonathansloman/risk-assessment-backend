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
    
    public String call(int currentBet) {
        if (totalBet() == currentBet) {
            check();
            return " already bet " + currentBet + ", assuming check.";
        } else {
            int extra = currentBet - totalBet();
            if (extra > chips) {
                
            }
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

    public int[] getBets() {
        return bets;
    }

    public void setBets(int[] bets) {
        this.bets = bets;
    }
    
    public void setBet(int pot, int amount) {
        this.bets[pot] = amount;
    }
    
    private int totalBet() {
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
    
    
}
