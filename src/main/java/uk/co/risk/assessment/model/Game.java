package uk.co.risk.assessment.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulating class for whole game including private information.
 *
 */
public class Game {
    private static final Logger LOG = LoggerFactory.getLogger(Game.class);

    Table table;
    List<Card> deck;
    Card[][] hands = new Card[Table.MAX_PLAYERS][2];
    Random random;
    
    public Game() {
        table = new Table();
        random = new Random();
    }
    
    private void shuffle() {
        deck = new LinkedList<>();
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

    public void deal() {
        shuffle();
        table.nextHand(0);
        for (int i = 0; i < Table.MAX_PLAYERS; i++) {
            if (table.getPlayers()[i] != null) {
                hands[i][0] = dealCard();
                hands[i][1] = dealCard();
                LOG.info("Dealt hand: {}, {} to player {}", hands[i][0], hands[i][1], table.getPlayers()[i].getName());
            }
        }
    }
    
    public Card[] getCardsFor(String thisPlayer) {
        int location = table.locatePlayer(thisPlayer);
        LOG.info("Got location {} for player {}", location, thisPlayer);
        if (location != -1) {
            return hands[location];
        }
        return null;
    }
    
    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public List<Card> getDeck() {
        return deck;
    }

    public void setDeck(List<Card> deck) {
        this.deck = deck;
    }

    public Card[][] getHands() {
        return hands;
    }

    public void setHands(Card[][] hands) {
        this.hands = hands;
    }
    
    
}
