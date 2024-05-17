package yuv.pink.npticket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class NPDate {
    public int year;
    public int month;
    public int day;

    static NPDate read(DataInputStream dataInputStream) throws IOException {
        NPDate date = new NPDate();
        date.year = dataInputStream.readShort();
        date.month = dataInputStream.readByte();
        date.day = dataInputStream.readByte();
        return date;
    }

    public void write(DataOutputStream dataOutputStream) throws IOException {
        dataOutputStream.writeShort(year);
        dataOutputStream.writeByte(month);
        dataOutputStream.writeByte(day);
    }
}
