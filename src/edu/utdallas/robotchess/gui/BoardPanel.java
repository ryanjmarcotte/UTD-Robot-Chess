package edu.utdallas.robotchess.gui;

import java.util.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import edu.utdallas.robotchess.manager.*;
import edu.utdallas.robotchess.game.*;
import edu.utdallas.robotchess.engine.*;

public class BoardPanel extends JPanel
{
    final String[] imageNames = {"green-pawn",
                                 "green-rook",
                                 "green-knight",
                                 "green-bishop",
                                 "green-queen",
                                 "green-king",
                                 "orange-pawn",
                                 "orange-rook",
                                 "orange-knight",
                                 "orange-bishop",
                                 "orange-queen",
                                 "orange-king"};

    final int TOTAL_SQUARES = 64;

    private Manager manager;
    private SquarePanel squares[];
    private Map<String, ImageIcon> imageMap;

    protected BoardPanel(Manager manager)
    {
        this.manager = manager;

        int rows = manager.getBoardRowCount();
        int columns = manager.getBoardColumnCount();

        setLayout(new GridLayout(rows, columns));

        squares = new SquarePanel[TOTAL_SQUARES];

        initializeSquares(rows, columns);
        configureImages();
    }

    private void initializeSquares(int rows, int columns)
    {
        boolean whiteSquare = true;

        for (int i = 0; i < squares.length; i++) {
            Color squareColor = whiteSquare ? Color.WHITE : Color.BLACK;
            squares[i] = new SquarePanel(i, squareColor);
            squares[i].setButtonActionListener(new ButtonListener());

            squares[i].setOpaque(true);

            if (i % 8 < columns && i / 8 < rows)
                add(squares[i]);

            if (i % 8 != 7)
                whiteSquare = !whiteSquare;
        }
    }

    private void configureImages()
    {
        final String basePath = "resources/";

        imageMap = new HashMap<>();

        for (int i = 0; i < imageNames.length; i++)
            imageMap.put(imageNames[i], new ImageIcon(basePath + "" +
                                                      imageNames[i] + ".png"));

        resizeImages(MainFrame.SQUARE_SIZE, MainFrame.SQUARE_SIZE);
    }

    public void resizeImages(int scaledWidth, int scaledHeight)
    {
        for (int i = 0; i < imageNames.length; i++) {
            BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight,
                                                       BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = scaledBI.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.drawImage(imageMap.get(imageNames[i]).getImage(), 0, 0,
                        scaledWidth, scaledHeight, null);
            g.dispose();
            imageMap.put(imageNames[i], new ImageIcon(scaledBI));
        }
    }

    protected void updateDisplay()
    {
        for (int i = 0; i < squares.length; i++) {
            squares[i].clearBorder();
            squares[i].setButtonIcon(null);
        }

        ArrayList<ChessPiece> pieces = manager.getActivePieces();

        for (int i = 0; i < pieces.size(); i++) {
            ChessPiece piece = pieces.get(i);
            int location = piece.getIntLocation();
            String pieceName = piece.getName();
            Icon icon = imageMap.get(pieceName);
            squares[location].setButtonIcon(icon);
        }

        ChessPiece currentlySelectedPiece = manager.getCurrentlySelectedPiece();
        if (currentlySelectedPiece != null) {
            int currentlySelectedIndex = currentlySelectedPiece.getIntLocation();
            squares[currentlySelectedIndex].setSelectedPieceBorder();

            ArrayList<Integer> possibleMoveLocations = manager.getValidMoveLocations();
            for (int i = 0; i < possibleMoveLocations.size(); i++)
                squares[possibleMoveLocations.get(i)].setMoveLocationBorder();
        }
    }

    protected void setManager(Manager manager)
    {
        this.manager = manager;
    }

    class ButtonListener implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            SquareButton buttonPressed = (SquareButton) e.getSource();
            int buttonIndex = buttonPressed.getIndex();
            manager.handleSquareClick(buttonIndex);

            updateDisplay();

            if (manager instanceof ChessManager &&
                ((ChessManager) manager).isActiveTeamComputerControlled()) {
                (new Thread(new EngineRunner())).start();
            }
        }
    }

    class EngineRunner implements Runnable
    {
        @Override
        public void run()
        {
            ChessGame game = manager.getGame();
            AlphaBetaSearch abs = new AlphaBetaSearch();
            Move move = abs.search(game);
            manager.handleSquareClick(move.origin);
            manager.handleSquareClick(move.destination);
            updateDisplay();
        }
    }
}
