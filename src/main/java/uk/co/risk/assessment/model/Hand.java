package uk.co.risk.assessment.model;

public class Hand {
    private Card[] cards = new Card[7];
    private Card[] bestCards = new Card[5];
    HandType handType = null;
    int[] ordinals = new int[5];
    
    public Hand(Card[] playerCards, Card[] tableCards) {
        System.arraycopy(playerCards, 0, cards, 0, 2);
        System.arraycopy(tableCards, 0, cards, 2, 5);
        calculateBestHand();
    }
    
    private void calculateBestHand() {
        // need to iterate through all possible 5 card combinations of 7 available cards and work out the best hand
        // we iterate through the cards we don't choose..
        Card[] testHand = new Card[5];
        HandType testHandType;
        int[] testHandOrdinals = new int[5];
        for (int skip1 = 0; skip1 < 6; skip1++) {
            for (int skip2 = skip1 + 1; skip2 < 7; skip2++) {
                for (int i = 0, j = 0; i < 7; i++) {
                    if (i != skip1 && i != skip2) {
                        testHand[j++] = cards[i];
                    }
                }
                int straight = testStraight(testHand);
                boolean flush = testFlush(testHand);
                
                if (straight > 0 && flush) {
                    if (straight == 14) {
                        testHandType = HandType.ROYALFLUSH;
                    } else {
                        testHandType = HandType.STRAIGHTFLUSH;
                        testHandOrdinals[0] = straight;
                    }
                } else {
                    boolean fourOfAKind = testFourOfAKind(testHand, testHandOrdinals);
                    if (fourOfAKind) {
                        testHandType = HandType.FOUROFAKIND;
                    }

                }
            }
        }
    }
    
    private boolean testFlush(Card[] testHand) {
        Suit suit = testHand[0].getSuit();
        for (int i = 1; i < 5; i++) {
            if (!testHand[i].getSuit().equals(suit)) {
                return false;
            }
        }
        return true;
    }
    
    /* return value is ordinal of highest card in straight */
    private int testStraight(Card[] testHand) {
        int lowest = 99;
        for (int i = 0; i < 5; i++) {
            if (testHand[i].getValue() < lowest) {
                lowest = testHand[i].getValue();
            }
        }
        boolean foundLowStraight = true;
        for (int sequence = 1; sequence <= 5 && foundLowStraight; sequence++) {
            boolean foundSequence = false;
            for (int i = 0; i < 5; i++) {
                if (testHand[i].getValue() == lowest + sequence) {
                    foundSequence = true;
                    break;
                }
            }
            if (!foundSequence) {
                foundLowStraight = false;
            }
        }
        if (!foundLowStraight && lowest == 1) {
            boolean foundHighStraight = true;
            // need to check for high straight with ace at top.
            for (int sequence = 13; sequence >= 10 && foundHighStraight; sequence--) {
                boolean foundSequence = false;
                for (int i = 0; i < 5; i++) {
                    if (testHand[i].getValue() == sequence) {
                        foundSequence = true;
                        break;
                    }
                }
                if (!foundSequence) {
                    foundHighStraight = false;
                }
            }
            if (foundHighStraight) {
                return 14;
            } else {
                return -1;
            }
        }
        if (foundLowStraight) {
            return lowest + 4;
        } else {
            return -1;
        }
    }

    private boolean testFourOfAKind(Card[] testHand, int[] testHandOrdinals) {
        // either the first card is part of the four of a kind, or the first card is the one which isn't
        int count = 0;
        int other = -1;
        for (int i = 1; i < 5; i++) {
            if (testHand[i].getValue() == testHand[0].getValue()) {
                count++;
            } else {
                other = testHand[i].getValue();
            }
        }
        if (count == 4) {
            testHandOrdinals[0] = testHand[0].getValue();
            testHandOrdinals[1] = other;
            for (int i = 2; i < 5; i++) {
                testHandOrdinals[i] = 0;
            }
            return true;
        }
        count = 0; 
        for (int i = 2; i < 5; i++) {
            if (testHand[i].getValue() == testHand[1].getValue()) {
                count++;
            } 
        }
        if (count == 4) {
            testHandOrdinals[0] = testHand[1].getValue();
            testHandOrdinals[1] = testHand[0].getValue();
            for (int i = 2; i < 5; i++) {
                testHandOrdinals[i] = 0;
            }
            return true;
        }
        return false;
    }
    
    /* 1 - better. 0 - same. -1 - worse */
    public int betterThan(Hand hand) {
        if (handType.getStrength() > hand.getHandType().getStrength()) {
            return 1;
        } else if (handType.getStrength() == hand.getHandType().getStrength()) {
            for (int i = 0; i < 5; i++) {
                if (ordinals[i] > hand.getOrdinals()[i]) {
                    return 1;
                } else if (ordinals[i] < hand.getOrdinals()[i]) {
                    return -1;
                }
            }
            return 0;
        } else {
            return -1;
        }
    }
    
    public Card[] getCards() {
        return cards;
    }
    
    public void setCards(Card[] cards) {
        this.cards = cards;
    }
    
    public Card[] getBestCards() {
        return bestCards;
    }
    
    public void setBestCards(Card[] bestCards) {
        this.bestCards = bestCards;
    }
    
    public HandType getHandType() {
        return handType;
    }
    
    public void setHandType(HandType handType) {
        this.handType = handType;
    }
    
    public int[] getOrdinals() {
        return ordinals;
    }
    
    public void setOrdinals(int[] ordinals) {
        this.ordinals = ordinals;
    }
    
    enum HandType {
        HIGHCARD(0, "High Card"), PAIR(1, "Pair"), TWOPAIR(2, "Two Pair"),
        THREEOFAKIND(3, "Three of a Kind"), STRAIGHT(4, "Straight"), FLUSH(5, "Flush"),
        FULLHOUSE(6, "Full House"), FOUROFAKIND(7, "Four of a Kind"),
        STRAIGHTFLUSH(8, "Straight Flush"), ROYALFLUSH(9, "Royal Flush");
        
        private final int strength;
        private final String description;
        
        HandType(int strength, String description) {
            this.description = description;
            this.strength = strength;
        }
        
        int getStrength() {
            return strength;
        }
        
        String getDescription() {
            return description;
        }
    }
}
