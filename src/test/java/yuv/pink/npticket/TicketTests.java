package yuv.pink.npticket;

import org.junit.jupiter.api.Test;

import java.io.*;

public class TicketTests {
    @Test
    public final void PkgReadFiles() throws Exception, BrokenTicketException {
        final File inputFile = new File("ticket2.bin");
        final File outputFile = new File("ticket3.bin");

        InputStream inputStream = new FileInputStream(inputFile);
        byte[] ticketData = inputStream.readAllBytes();

        NPTicket tick = new NPTicket();
        tick.parse(ticketData, null, false);

        tick.writeTo(new FileOutputStream(outputFile), new Cipher(0x42424242));
        System.out.println("a");
    }
}
