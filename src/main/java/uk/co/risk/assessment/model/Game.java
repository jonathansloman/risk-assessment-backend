package uk.co.risk.assessment.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Encapsulating class for whole game including private information.
 *
 */
public class Game {
    Table table;
    List<Card> deck = new LinkedList<>();
    Card[][] hands = new Card[Table.MAX_PLAYERS][2];
    Random random;
    
    public Game() {
        table = new Table();
        random = new Random();
    }
    
    private void shuffle() {
        for (Suit suit : Suit.values()) {
            for (int value = 1; value <= 13; value++) {
                deck.add(new Card(suit, value));
            }
        }
    }
    
    public Card dealCard() {
        int choice = random.nextInt(deck.size());
        Card card = deck.get(choice);
        deck.remove(choice);
        return card;
    }
}
