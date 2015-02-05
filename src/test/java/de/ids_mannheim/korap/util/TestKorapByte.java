package de.ids_mannheim.korap.util;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import static de.ids_mannheim.korap.util.KorapByte.*;
import de.ids_mannheim.korap.util.QueryException;
import java.nio.ByteBuffer;

/**
 * @author diewald
 */
public class TestKorapByte {

    @Test
    public void testConversion() {
        assertEquals(4, byte2int(int2byte(4)));
        assertEquals(
            byte2int(ByteBuffer.allocate(4).putInt(4).array()),
            byte2int(int2byte(4))
        );

        assertEquals(
            byte2int(ByteBuffer.allocate(4).putInt(99999).array()),
            byte2int(int2byte(99999))
        );

        assertEquals(128, byte2int(int2byte(128)));
        assertEquals(1024, byte2int(int2byte(1024)));
        assertEquals(66_666, byte2int(int2byte(66_666)));
        assertEquals(66_666, byte2int(int2byte(66_666)), 0);
    };
};
