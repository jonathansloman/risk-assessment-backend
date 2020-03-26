package uk.co.risk.assessment.model;

public class Card {
    int value;
    Suit suit;
    
    public Card(Suit suit, int value) {
        this.suit = suit;
        this.value = value;
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
