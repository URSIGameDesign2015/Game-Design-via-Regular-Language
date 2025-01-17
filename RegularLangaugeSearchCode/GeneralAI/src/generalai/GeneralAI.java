package generalai;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.regex.Pattern;
import org.jdom2.*;
import org.jdom2.output.XMLOutputter;
import org.jdom2.output.Format;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GeneralAI {

    public static void main(String[] args) throws IOException {
       // p == player chooses purple block
       // a == game manger adjusted the scene due to the fact the player 
        // chose the purple block
        
        //b == player chooses blue block
       // c == game manger adjusted the scene due to the fact the player 
        // chose the blue block
        // (ac || bd) *
        // Kleene Star --> *
        // "(fp|fb)(p|b)(fp|fb)(p|b)"
        
        // only runs once ((a|b)(c|d).*?)
        
        // four turns for each player
        String a1 = "(?=(a|b)(c|d){4}";
        // a follows c and b follows d in the player's first three moves
        String a2 = "(?=(a|b)(ca|db){3}(c|d))";
        String a3 = ".*";
        
        Pattern regex = Pattern.compile(a2+a3);
        //
        String[] compAlpha = new String[] {"a", "b"};
        String[] playerAlpha = new String[] {"c", "d"};
        // a == MOVE BLUE BLOCK CLOSER
        // b == MOVE PURPLE BLOCK CLOSER
        // c == Blue BLOCK CLOSER
        // d == Purple BLOCK CLOSER
        RegLangGameState regexGame = new RegLangGameState(compAlpha, playerAlpha, regex, "");
        regexGame.turn = true;
        Document strategy = createStrategyDocument(regexGame);
        XMLOutputter XMLoutput = new XMLOutputter();
        XMLoutput.setFormat(Format.getPrettyFormat());
        try {
            XMLoutput.output(strategy, new FileWriter("strategy.xml"));
        } catch (IOException ex) {
            Logger.getLogger(GeneralAI.class.getName()).log(Level.SEVERE, null, ex);
        }
        Pattern losing = Pattern.compile("(dp|dk)d(rm|rf)");
        RegLangGameState losingGame = new RegLangGameState(compAlpha, playerAlpha, losing, "");
        losingGame.turn = true;
        Document losingStrategy = createStrategyDocument(losingGame);
        try {
            XMLoutput.output(losingStrategy, new FileWriter("losingStrategy.xml"));
        } catch (IOException ex) {
            Logger.getLogger(GeneralAI.class.getName()).log(Level.SEVERE, null, ex);
        }
        int i = 0;
        Element playerMove = null;
        Element compMove = null;
        while (!regexGame.gameOver()) {
            compMove = compTurn(regexGame, strategy, playerMove, i);
            regexGame = regexGame.doMove(new RegLangMove(compMove.getAttributeValue("move")));
            playerMove = playerTurn(regexGame, compMove);
            regexGame = regexGame.doMove(new RegLangMove(playerMove.getAttributeValue("move")));
            i++;
        }
        System.out.println("The generated string is: " + regexGame.lettersPlayed);
    }
    
    static public boolean winForCompRL(RegLangGameState game) {
        if (game.gameOver()) {
            return game.findMatch();
        }
        ArrayList<RegLangMove> moves = game.legalMoves();
        if (game.turn) {
            return existsRLWin(moves, game) != null;
        } else {
            return allRLWin(moves, game);
        }
    }
// returning all existing winning moves for CP
    static public RegLangMove existsRLWin(ArrayList<RegLangMove> moves, RegLangGameState game) {
        ArrayList<RegLangMove> existingMoves = new ArrayList();
        for (RegLangMove m : moves) {
            if (winForCompRL(game.doMove(m))) {
                existingMoves.add(m);
            }
        }
        if (!existingMoves.isEmpty()) {
            Random rand = new Random();
            return existingMoves.get(Math.abs(rand.nextInt()) % existingMoves.size());
        } else {
            return null;
        }
        
    }

    static public boolean allRLWin(ArrayList<RegLangMove> moves, RegLangGameState game) {
        for (RegLangMove m : moves) {
            if (!winForCompRL(game.doMove(m))) {
                return false;
            }
        }
        return true;
    }
    
    static public Element createTree(RegLangGameState game, ArrayList<RegLangMove> moves) {
        if (game.gameOver()) {
            return new Element("GameOver");
        }
        if (game.turn) {
            Element strategy = new Element("ComputerStrategy");
            for (RegLangMove m : moves) {
                ArrayList<RegLangMove> currMove = createList(m);
                if (existsRLWin(currMove, game) != null) {
                    Element move = new Element("Move");
                    move.setAttribute(new Attribute("move", m.nextLetter));
                    RegLangGameState newGame = game.doMove(m);
                    ArrayList<RegLangMove> legalMoves = newGame.legalMoves();
                    move.addContent(createTree(newGame, legalMoves));
                    strategy.addContent(move);
                }
            }
            if(existsRLWin(moves, game) != null)
                return strategy;
            else
                return new Element("NoWinningStrategyFound");
        }
        else {
            Element strategy = new Element("PlayerResponse");
            for (RegLangMove m : moves) {
                Element move = new Element("Move");
                move.setAttribute(new Attribute("move", m.nextLetter));
                RegLangGameState newGame = game.doMove(m);
                ArrayList<RegLangMove> legalMoves = newGame.legalMoves();
                move.addContent(createTree(newGame, legalMoves));
                strategy.addContent(move);
            }
            return strategy;
        }
    }
    
    static public Document createStrategyDocument(RegLangGameState game) {
        ArrayList<RegLangMove> legalMoves = game.legalMoves();
        Document output = new Document();
        return output.addContent(createTree(game, legalMoves));
    }

    static public ArrayList<RegLangMove> createList(RegLangMove m) {
        ArrayList move = new ArrayList(1);
        move.add(m);
        return move;
    }
    
    static public Element playerTurn(RegLangGameState game, Element compMove) throws IOException {
        ArrayList<RegLangMove> moves = game.legalMoves();
        System.out.println("Which move would you like to perform? (Enter the number of the move)");
        for (int i = 0; i < moves.size(); i++) {
            System.out.println(i + 1 + ". " + moves.get(i).nextLetter);
        }
        BufferedReader in =
                new BufferedReader(new InputStreamReader(System.in));
        String text;
        text = in.readLine();
        int answer = Integer.parseInt(text) - 1;
        RegLangMove move = moves.get(answer);
        Element playerResponse = compMove.getChild("PlayerResponse");
        List<Element> playerMoves = playerResponse.getChildren();
        for (int j = 0; j < playerMoves.size(); j++) {
            Element currMove = playerMoves.get(j);
            if (currMove.getAttributeValue("move").equals(move.nextLetter)) {
                return currMove;
            }
        }
        return null;
    }
    
    static public Element compTurn(RegLangGameState game, Document strat, Element playerMove, int i) {
        Element compStrategy = null;
        if (i == 0) {
                compStrategy = (Element) strat.getContent().get(0);
            } else {
                compStrategy = (Element) playerMove.getContent().get(0);
            }
            List<Element> moves = compStrategy.getChildren();
            Random randomGenerator = new Random();
            Element compMove = moves.get(randomGenerator.nextInt(moves.size()));
            System.out.println("The Computer played: " + compMove.getAttributeValue("move"));
            return compMove;
    }
}