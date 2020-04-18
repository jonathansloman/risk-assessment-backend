package uk.co.risk.assessment.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Card {
    private static final Logger LOG = LoggerFactory.getLogger(Card.class);
    
    int value;
    Suit suit;
    
    public Card(Suit suit, int value) {
        this.suit = suit;
        this.value = value;
    }
    
    // used for card-setting
    public Card(char suit, char value) {
        switch (suit) {
            case 'S':
                this.suit = Suit.SPADES;
                break;
            case 'D':
                this.suit = Suit.DIAMONDS;
                break;
            case 'H':
                this.suit = Suit.HEARTS;
                break;
            case 'C':
                this.suit = Suit.CLUBS;
                break;
            default:
                LOG.warn("Invalid card suit {}, defaulting to spades", suit);
                this.suit = Suit.SPADES;
        }
        switch (value) {
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                this.value = (int) (value - '0');
                break;
            case '0': // use 0 or T for ten to make life easier.
            case 'T':
                this.value = 10;
                break;
            case 'J':
                this.value = 11;
                break;
            case 'Q':
                this.value = 12;
                break;
            case 'K':
                this.value = 13;
                break;
            case 'A':
                this.value = 14;
                break;
            default:
                LOG.warn("Invalid card value {}, defaulting to 2", value);
                this.value = 2;
        }
        
    }
    
    @Override
    public String toString() {
        return "Card [value=" + value + ", suit=" + suit + "]";
    }
    
    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    public Suit getSuit() {
        return suit;
    }
    
    public void setSuit(Suit suit) {
        this.suit = suit;
    }
    
}
