package uk.co.risk.assessment.model;

public class Card {
    int value;
    Suit suit;
    
    public Card(Suit suit, int value) {
        this.suit = suit;
        this.value = value;
    }
}
