package chess.engine;

import java.util.logging.*;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests the functionality of an InteriorSquare, including generation
 * @author Ryan J. Marcotte
 */
public class InteriorSquareTest {
    private static final Logger logger = ChessLogger.getInstance().logger;

    @Test
    public void testGenerateInteriorSquareAt() {
        logger.log(Level.WARNING, "Begin testGenerateInteriorSquareAt() - InteriorSquareTest");

        Square sq1 = InteriorSquare.generateInteriorSquareAt(8);
        Square sq2 = InteriorSquare.generateInteriorSquareAt(42);

        assertEquals(sq1.getNumericalLocation(), 8);
        assertEquals(sq2.getNumericalLocation(), 42);

        logger.log(Level.WARNING, "End testGenerateInteriorSquareAt() - InteriorSquareTest");
    }
}
