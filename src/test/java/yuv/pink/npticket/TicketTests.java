package yuv.pink.npticket;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;

public class TicketTests {
    @Test
    public final void PkgReadFiles() throws Exception, BrokenTicketException {
        final File outputFile = new File("ticket3.bin");
        byte[] ticketData = Files.readAllBytes(new File("ticket2.bin").toPath());

        NPTicket tick = new NPTicket();
        tick.parse(ticketData, null, false);

        tick.writeTo(Files.newOutputStream(outputFile.toPath()), new Cipher(0x42424242));
        System.out.println("a");
    }
}
