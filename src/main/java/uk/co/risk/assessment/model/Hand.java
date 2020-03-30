package uk.co.risk.assessment.model;

import java.util.Arrays;
import java.util.Collections;

public class Hand {
    private Card[] cards = new Card[7];
    private Card[] bestCards = new Card[5];
    HandType handType = null;
    Integer[] ordinals = new Integer[5];
    
    public Hand(Card[] playerCards, Card[] tableCards) {
        System.arraycopy(playerCards, 0, cards, 0, 2);
        System.arraycopy(tableCards, 0, cards, 2, 5);
        calculateBestHand();
    }
    
    // this constructor used to create dummy hand values for testing while working out best hand
    public Hand(HandType handType, Integer[] ordinals) {
        this.handType = handType;
        this.ordinals = ordinals;
    }
    
    private void calculateBestHand() {
        // need to iterate through all possible 5 card combinations of 7 available cards and work out the best hand
        // we iterate through the cards we don't choose..
        Card[] testCards = new Card[5];
        
        for (int skip1 = 0; skip1 < 6; skip1++) {
            for (int skip2 = skip1 + 1; skip2 < 7; skip2++) {
                for (int i = 0, j = 0; i < 7; i++) {
                    if (i != skip1 && i != skip2) {
                        testCards[j++] = cards[i];
                    }
                }
                Hand testHand = analyseHand(testCards);
                if (handType == null) {
                    handType = testHand.getHandType();
                    ordinals = testHand.getOrdinals();
                    bestCards = testCards;
                } else {
                    if (betterThan(testHand) < 0) {
                        handType = testHand.getHandType();
                        ordinals = testHand.getOrdinals();
                        bestCards = testCards;
                    }
                }
            }
        }
    }
    
    private Hand analyseHand(Card[] testCards) {
        HandType testHandType;
        // we use Integer not int here so we can do a reverse array sort
        Integer[] testHandOrdinals = new Integer[5];
        
        int straight = testStraight(testCards);
        boolean flush = testFlush(testCards);
        
        if (straight > 0 && flush) {
            if (straight == 14) {
                testHandType = HandType.ROYALFLUSH;
            } else {
                testHandType = HandType.STRAIGHTFLUSH;
                testHandOrdinals[0] = straight;
            }
        } else {
            int[] frequencies = frequency(testCards);
            if (testFourOfAKind(frequencies, testHandOrdinals)) {
                testHandType = HandType.FOUROFAKIND;
            } else if (testFullHouse(frequencies, testHandOrdinals)) {
                testHandType = HandType.FULLHOUSE;
            } else if (flush) {
                testHandType = HandType.FLUSH;
                // ordinals need to be card values of flush in reverse order
                for (int i = 0; i < 5; i++) {
                    testHandOrdinals[i] = testCards[i].getValue();
                }
                Arrays.sort(testHandOrdinals, Collections.reverseOrder());
            } else if (straight > 0) {
                testHandType = HandType.STRAIGHT;
                testHandOrdinals[0] = straight;
            } else if (testThreeOfAKind(frequencies, testHandOrdinals)) {
                testHandType = HandType.THREEOFAKIND;
            } else if (testTwoPair(frequencies, testHandOrdinals)) {
                testHandType = HandType.TWOPAIR;
            } else if (testPair(frequencies, testHandOrdinals)) {
                testHandType = HandType.PAIR;
            } else {
                testHandType = HandType.HIGHCARD;
                // ordinals need to be card values in reverse order
                for (int i = 0; i < 5; i++) {
                    testHandOrdinals[i] = testCards[i].getValue();
                }
                Arrays.sort(testHandOrdinals, Collections.reverseOrder());
            }
        }
        return new Hand(testHandType, testHandOrdinals);
    }
    
    private boolean testFlush(Card[] testCards) {
        Suit suit = testCards[0].getSuit();
        for (int i = 1; i < 5; i++) {
            if (!testCards[i].getSuit().equals(suit)) {
                return false;
            }
        }
        return true;
    }
    
    /* return value is ordinal of highest card in straight */
    private int testStraight(Card[] testCards) {
        int highest = 0;
        for (int i = 0; i < 5; i++) {
            if (testCards[i].getValue() > highest) {
                highest = testCards[i].getValue();
            }
        }
        boolean foundHighStraight = true;
        for (int sequence = 1; sequence < 5 && foundHighStraight; sequence++) {
            boolean foundSequence = false;
            for (int i = 0; i < 5; i++) {
                if (testCards[i].getValue() == highest - sequence) {
                    foundSequence = true;
                    break;
                }
            }
            if (!foundSequence) {
                foundHighStraight = false;
            }
        }
        if (!foundHighStraight && highest == 14) {
            boolean foundLowStraight = true;
            // need to check for low straight with ace at bottom
            for (int sequence = 2; sequence <= 5 && foundLowStraight; sequence--) {
                boolean foundSequence = false;
                for (int i = 0; i < 5; i++) {
                    if (testCards[i].getValue() == sequence) {
                        foundSequence = true;
                        break;
                    }
                }
                if (!foundSequence) {
                    foundLowStraight = false;
                }
            }
            if (foundLowStraight) {
                return 5;
            } else {
                return -1;
            }
        }
        if (foundHighStraight) {
            return highest;
        } else {
            return -1;
        }
    }
    
    /* frequency chart of cards in hand. Note card values start of 2 so we use offset into array */
    private int[] frequency(Card[] testCards) {
        int[] frequencies = new int[13];
        for (int i = 0; i < 5; i++) {
            frequencies[testCards[i].getValue() - 2]++;
        }
        return frequencies;
    }
    
    private boolean testFourOfAKind(int[] frequencies, Integer[] testHandOrdinals) {
        for (int i = 0; i < 13; i++) {
            if (frequencies[i] == 4) {
                testHandOrdinals[0] = i + 2;
                // look for kicker
                for (int j = 0; j < 13; j++) {
                    if (frequencies[j] == 1) {
                        testHandOrdinals[1] = j + 2;
                    }
                }
                return true;
            }
        }
        return false;
    }
    
    private boolean testFullHouse(int[] frequencies, Integer[] testHandOrdinals) {
        for (int i = 0; i < 13; i++) {
            if (frequencies[i] == 3) {
                for (int j = 0; j < 13; j++) {
                    if (frequencies[j] == 2) {
                        testHandOrdinals[0] = i + 2;
                        testHandOrdinals[1] = j + 2;
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }
    
    private boolean testThreeOfAKind(int[] frequencies, Integer[] testHandOrdinals) {
        for (int i = 0; i < 13; i++) {
            if (frequencies[i] == 3) {
                testHandOrdinals[0] = i + 2;
                // fill in kickers
                int kickers = 1;
                for (int j = 12; j >= 0; j--) {
                    if (frequencies[j] == 1) {
                        testHandOrdinals[kickers++] = j + 2;
                    }
                }
                return true;
            }
        }
        return false;
    }
    
    private boolean testTwoPair(int[] frequencies, Integer[] testHandOrdinals) {
        // search from top down to get highest value pair first
        for (int i = 12; i >= 0; i--) {
            if (frequencies[i] == 2) {
                for (int j = i - 1; j >= 0; j--) {
                    if (frequencies[j] == 2) {
                        testHandOrdinals[0] = i + 2;
                        testHandOrdinals[1] = j + 2;
                        // find kicker
                        for (int k = 0; k < 13; k++) {
                            if (frequencies[k] == 1) {
                                testHandOrdinals[2] = k + 2;
                            }
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean testPair(int[] frequencies, Integer[] testHandOrdinals) {
        for (int i = 0; i < 13; i++) {
            if (frequencies[i] == 2) {
                testHandOrdinals[0] = i + 2;
                
                // find kickers, highest first
                int kickers = 1;
                for (int j = 12; j >= 0; j--) {
                    if (frequencies[j] == 1) {
                        testHandOrdinals[kickers++] = j + 2;
                    }
                }
                return true;
            }          
        }
        return false;
    }
    
    /* 1 - better. 0 - same. -1 - worse */
    public int betterThan(Hand hand) {
        if (handType.getStrength() > hand.getHandType().getStrength()) {
            return 1;
        } else if (handType.getStrength() == hand.getHandType().getStrength()) {
            for (int i = 0; i < 5; i++) {
                if (ordinals[i] == null) {
                    return 0;
                }
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
    
    public Integer[] getOrdinals() {
        return ordinals;
    }
    
    public void setOrdinals(Integer[] ordinals) {
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
