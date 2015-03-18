/**
 *
 * @author Ryan J. Marcotte
 */

package edu.utdallas.robotchess.game;

import java.util.ArrayList;
import java.util.Collections;

public class King extends ChessPiece
{
    public King(Square location)
    {
        super(location);
    }
    
    public King(Square location, int id)
    {
        super(location, id); 
    }

    public King(Square location, int id, Team team, 
                boolean active, boolean hasNotMoved)
    {
        super(location, id, team, active, hasNotMoved);
    }

    protected ChessPiece copyPiece()
    {
        ChessPiece copiedPiece = new King(getLocation(), getID(), getTeam(),
                                          isActive(), hasNotMoved());
        return copiedPiece;
    }

    protected ArrayList<Square> generateMoveLocations()
    {
        ArrayList<Square> moveList = new ArrayList<>();

        for (int i = 0; i < 8; i++)
            addMovesInDirection(moveList, i, 1);

        Collections.sort(moveList);

        return moveList;
    }
}