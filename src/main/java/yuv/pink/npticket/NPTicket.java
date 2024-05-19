package yuv.pink.npticket;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NPTicket {
    public byte majorVersion;
    public byte minorVersion;
    public byte packetID;
    public boolean partialPacket;
    public boolean jumboPacket;
    public byte[] serial;
    public int issuerID;
    public long issuedDate;
    public long notOnOrAfterDate;
    public Subject subject;
    public byte[] cookie;
    public List<Entitlement> entitlements;
    public List<Role> roles;
    public String platform;
    public byte[] consoleID;
    public boolean dontHaveRSASignature;
    public boolean isVerified;

    public boolean isExpired() {
        return notOnOrAfterDate < System.currentTimeMillis();
    }

    public void writeTo(OutputStream ticketBody, Cipher cipher) throws IOException {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(byteOutStream);
        int version = ((short)(majorVersion) << 8) + (short)minorVersion;

        // Header

        int data0 = (this.majorVersion << 4) | (this.packetID & 15);
        dataOutputStream.writeByte(data0);
        dataOutputStream.writeByte(this.minorVersion);
        dataOutputStream.writeByte(0);
        byte data3 = 4;
        dataOutputStream.writeByte(data3);

        // Placeholder
        writeTag(dataOutputStream,  0, 0);
        writeTag(dataOutputStream, 0x3000, 0);

        // Serial
        writeTag(dataOutputStream,8, serial.length);
        dataOutputStream.write(serial);

        // Issuer
        writeTag(dataOutputStream, 1, 4);
        dataOutputStream.writeInt(issuerID);

        // IssuedDate
        writeTag(dataOutputStream, 7, 8);
        dataOutputStream.writeLong(issuedDate);

        // NotOnOrAfterDate
        writeTag(dataOutputStream, 7, 8);
        dataOutputStream.writeLong(notOnOrAfterDate);

        // AccountID
        writeTag(dataOutputStream, 2, 8);
        dataOutputStream.writeLong(this.subject.accountID);

        // OnlineID
        writeTag(dataOutputStream, 4, 32);
        writeString(dataOutputStream, this.subject.onlineID, 32);

        // Region
        writeTag(dataOutputStream, 8, 4);
        dataOutputStream.write(this.subject.region);

        // Domain
        if(version >= 257) {
            writeTag(dataOutputStream, 4, 4);
            writeString(dataOutputStream, this.subject.domain, 4);
        }

        // ServiceID
        writeTag(dataOutputStream, 8, 24);
        writeString(dataOutputStream, this.subject.serviceID, 24);

        // DateOfBirth
        if(version >= 768) {
            writeTag(dataOutputStream, 0x3011, 4);
            this.subject.dob.write(dataOutputStream);
        }

        // Status
        writeTag(dataOutputStream, 1, 4);
        dataOutputStream.writeInt(this.subject.status);

        // Duration
        if((this.subject.status&255) >= 128) {
            writeTag(dataOutputStream, 2, 8);
            dataOutputStream.writeLong(this.subject.duration);
        }

        // Cookie
        if(this.cookie != null) {
            writeTag(dataOutputStream, 8, 4);
            dataOutputStream.write(this.cookie);
        }

        // Entitlements
        if(version >= 768) {
            ByteArrayOutputStream entitlementsBody = new ByteArrayOutputStream();
            DataOutputStream entitlementsBodyData = new DataOutputStream(entitlementsBody);
            if(this.entitlements != null) {
                for (Entitlement entitlement : this.entitlements) {
                    String eid = entitlement.entitlementID.substring(entitlement.entitlementID.lastIndexOf("-") + 1);
                    boolean haveExpiry = entitlement.expiredDate != 0;

                    byte header = 0;
                    if (entitlement.type == 1) {
                        header |= 1 << 6;
                    }
                    if (haveExpiry) {
                        header |= 1 << 5;
                    }
                    header |= (byte) (eid.length() & 31);
                    entitlementsBodyData.write(header);

                    writeString(entitlementsBodyData, eid, eid.length());
                    entitlementsBodyData.writeLong(entitlement.createdDate);

                    if (haveExpiry) {
                        entitlementsBodyData.writeLong(entitlement.expiredDate);
                    }

                    if (entitlement.type == 1) {
                        entitlementsBodyData.writeInt(entitlement.remainingCount);
                        entitlementsBodyData.writeInt(entitlement.consumedCount);
                    }
                }
            }
            writeTag(dataOutputStream, 0x3010, entitlementsBody.size());
            dataOutputStream.write(entitlementsBody.toByteArray());
        }

        if(version >= 512) {
            ByteArrayOutputStream rolesBody = new ByteArrayOutputStream();
            DataOutputStream rolesBodyData = new DataOutputStream(rolesBody);
            if(this.roles != null) {
            for (Role role : this.roles) {
                writeTag(rolesBodyData, 0x3004, 8+role.domain.length()+1+4);
                writeTag(rolesBodyData, 0x1, 4);
                rolesBodyData.writeInt(role.id);
                writeTag(rolesBodyData, 0x4, role.domain.length()+1);
                writeString(rolesBodyData, role.domain, role.domain.length()+1);
            }
            }
            writeTag(dataOutputStream, 0, rolesBody.size());
            dataOutputStream.write(rolesBody.toByteArray());
        }

        if(version >= 818) {
            writeTag(dataOutputStream, 0x8, 8);
            writeString(dataOutputStream, this.platform, 8);
        }

        writeTag(dataOutputStream, 0x8, 64);
        dataOutputStream.write(this.consoleID);

        // RSA Signing
        if (!dontHaveRSASignature) {
            byte[] signedData = byteOutStream.toByteArray();
            // Cipher ID
            writeTag(dataOutputStream, (short) 0x3012, 200);
            dataOutputStream.write(new byte[]{0x41, 0, 0, 8});
            dataOutputStream.writeInt(cipher.id);

            dataOutputStream.write(cipher.signRSA(Arrays.copyOfRange(signedData, 16, signedData.length)));
        }

        // put size
        byte[] ticketBodyBytes = byteOutStream.toByteArray();
        ByteBuffer.wrap(ticketBodyBytes, 14, 2).putShort((short) (ticketBodyBytes.length - 16));

        // put total size
        ByteBuffer.wrap(ticketBodyBytes, 8, 2).putShort((short) ((ticketBodyBytes.length - 10) + 16 + cipher.size()));

        ticketBody.write(ticketBodyBytes);

        // Second Signature
        ByteArrayOutputStream byteOutStream2 = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream2 = new DataOutputStream(byteOutStream2);
        writeTag(dataOutputStream2, 0x3002, 12+cipher.size());
        writeTag(dataOutputStream2, 0x8, 4);
        dataOutputStream2.writeInt(cipher.id);
        writeTag(dataOutputStream2, 0x8, cipher.size());
        byte[] sig2 = cipher.sign(ticketBodyBytes);
        dataOutputStream2.write(sig2);

        ticketBody.write(byteOutStream2.toByteArray());
    }

    public void parse(byte[] data, Cipher cipher, boolean verify) throws IOException, BrokenTicketException {
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(data);
        DataInputStream dataInputStream = new DataInputStream(byteInStream);

        byte data0 = dataInputStream.readByte();
        majorVersion = (byte) ((data0 & 240) >> 4);
        packetID = (byte) (data0 & 15);
        minorVersion = dataInputStream.readByte();
        dataInputStream.readByte();
        byte data3 = dataInputStream.readByte();
        partialPacket = (data3 & 0x02) != 0;
        jumboPacket = (data3 & 0x01) != 0;
        int version = ((short)(majorVersion) << 8) + (short)minorVersion;

        checkTag(dataInputStream, 0, -1);
        int ticketBodySize = checkTag(dataInputStream, 0x3000, -1);

        if(verify) {
            byte[] signedData = Arrays.copyOfRange(data, 0, ticketBodySize+16);
            byte[] signatureData = Arrays.copyOfRange(data, ticketBodySize+16, data.length);
            ByteArrayInputStream sigInStream = new ByteArrayInputStream(signatureData);
            DataInputStream dataSigInStream = new DataInputStream(sigInStream);
            checkTag(dataSigInStream, 0x3002, 12+cipher.size());
            checkTag(dataSigInStream, 8, 4);
            int cipherId = dataSigInStream.readInt();
            if(cipherId != cipher.id) {
                throw new BrokenTicketException("wrong cipher id");
            }

            checkTag(dataSigInStream, 8, cipher.size());
            byte[] signatureBytes = new byte[cipher.size()];
            dataSigInStream.readFully(signatureBytes);
            if(!cipher.verify(signedData, signatureBytes)) {
                throw new BrokenTicketException("signature invalid");
            }
            this.isVerified = true;
        }

        byte[] ticketBody = Arrays.copyOfRange(data, 16, ticketBodySize+16);
        byteInStream = new ByteArrayInputStream(ticketBody);
        dataInputStream = new DataInputStream(byteInStream);

        // Serial
        checkTag(dataInputStream, 0x8, 20);
        serial = new byte[20];
        dataInputStream.readFully(serial);

        // Issuer
        checkTag(dataInputStream, 0x1, 4);
        issuerID = dataInputStream.readInt();

        // IssuedDate
        checkTag(dataInputStream, 0x7, 8);
        issuedDate = dataInputStream.readLong();

        // NotOnOrAfterDate
        checkTag(dataInputStream, 0x7, 8);
        notOnOrAfterDate = dataInputStream.readLong();

        // AccountID
        checkTag(dataInputStream, 0x2, 8);
        long accountID = dataInputStream.readLong();

        // OnlineID
        checkTag(dataInputStream, 0x4, 32);
        byte[] onlineIDBytes = new byte[32];
        dataInputStream.readFully(onlineIDBytes);
        String onlineID = new String(onlineIDBytes, "UTF-8").trim();

        // Region
        checkTag(dataInputStream, 0x8, 4);
        byte[] regionBytes = new byte[4];
        dataInputStream.readFully(regionBytes);

        String domain = null;
        if (version >= 257) {
            // Domain
            checkTag(dataInputStream, 0x4, 4);
            byte[] domainBytes = new byte[4];
            dataInputStream.readFully(domainBytes);
            domain = new String(domainBytes, "UTF-8").trim();
        }

        // ServiceID
        checkTag(dataInputStream, 0x8, 24);
        byte[] serviceIDBytes = new byte[24];
        dataInputStream.readFully(serviceIDBytes);
        String serviceID = new String(serviceIDBytes, "UTF-8").trim();

        // Dob
        NPDate dob = null;
        if (version >= 768) {
            checkTag(dataInputStream, 0x3011, 4);
            dob = NPDate.read(dataInputStream);
        }

        // Status
        checkTag(dataInputStream, 0x1, 4);
        int status = dataInputStream.readInt();

        // Duration
        long duration = 0;
        if ((status & 255) >= 128) {
            checkTag(dataInputStream, 0x2, 8);
            duration = dataInputStream.readLong();
        }

        this.subject = new Subject(
                accountID,
                onlineID,
                regionBytes,
                domain,
                serviceID,
                dob,
                status,
                duration
        );

        // Cookie
        try {
            checkTag(dataInputStream, 0x8, 4);
            byte[] cookieBytes = new byte[4];
            dataInputStream.readFully(cookieBytes);
            cookie = cookieBytes;
        } catch (BrokenTicketException ignored) {}

        // Entitlements
        if (version >= 768) {
            this.entitlements = new ArrayList<>();
            int entitlementsSize = checkTag(dataInputStream, 0x3010, -1);
            byte[] entitlementsData = new byte[entitlementsSize];
            dataInputStream.readFully(entitlementsData);
            int pe = 0;
            while (pe < entitlementsData.length) {
                Entitlement entitlement = new Entitlement();
                byte header = entitlementsData[pe];
                pe++;
                int eidSize = header & 31;
                if (eidSize == 0) {
                    eidSize = 32;
                }

                byte[] eidBytes = new byte[eidSize];
                System.arraycopy(entitlementsData, pe, eidBytes, 0, eidSize);
                String entitlementID = new String(eidBytes, "UTF-8");
                if (eidSize > 8) {
                    entitlement.entitlementID = entitlementID;
                } else {
                    entitlement.entitlementID = this.subject.serviceID + "-" + entitlementID;
                }
                pe += eidSize;
                DataInputStream din = new DataInputStream(new ByteArrayInputStream(entitlementsData, pe, entitlementsData.length-pe));


                entitlement.createdDate = din.readLong();
                pe += 8;
                if ((header & 32) != 0) {
                    entitlement.expiredDate = din.readLong();
                    pe += 8;
                }

                if ((header & 64) != 0) {
                    entitlement.type = 1;
                    entitlement.remainingCount = din.readInt();
                    entitlement.consumedCount = din.readInt();
                    pe += 8;
                } else {
                    entitlement.type = 0;
                }

                this.entitlements.add(entitlement);
            }
        } else {
            throw new IllegalStateException("Old style entitlements :(");
        }

        // Roles
        if (version >= 512) {
            int rolesSize = checkTag(dataInputStream, 0x0, -1);
            int pp = 0;
            while (pp < rolesSize) {
                checkTag(dataInputStream, 0x0, -1);
                checkTag(dataInputStream, 0x1, 4);
                pp += 8;
                int id = dataInputStream.readInt();
                int domainLength = checkTag(dataInputStream, 0x1, -1);
                pp += 8;
                byte[] domainBytes = new byte[domainLength];
                dataInputStream.readFully(domainBytes);
                pp += domainLength;
                String roleDomain = new String(domainBytes, "UTF-8").trim();
                Role role = new Role(id, roleDomain);
                this.roles.add(role);
            }
        }

        // Platform
        if (version >= 818) {
            checkTag(dataInputStream, 0x8, 8);
            byte[] platformBytes = new byte[8];
            dataInputStream.readFully(platformBytes);
            platform = new String(platformBytes, "UTF-8").trim();
        }

        // ConsoleID
        checkTag(dataInputStream, 0x8, 64);
        byte[] consoleIDBytes = new byte[64];
        dataInputStream.readFully(consoleIDBytes);
        consoleID = consoleIDBytes;

        try {
            checkTag(dataInputStream, 0x3012, 200);
            dataInputStream.readInt();
            int cipherID = dataInputStream.readInt();
            if(verify && cipherID != cipher.id) {
                throw new BrokenTicketException("wrong cipherID");
            }
            byte[] signatureData = new byte[200];
            dataInputStream.readFully(signatureData);
            if(verify && !cipher.verifyRSA(ticketBody, signatureData)) {
                throw new BrokenTicketException("invalid rsa signature");
            }
        } catch(IOException ignored) {
            this.dontHaveRSASignature = true;
        }
    }

    private void writeTag(DataOutputStream dataOutputStream, int tag, int length) throws IOException {
        dataOutputStream.writeShort(tag);
        if(tag == 0 ||tag == 0x3000 || tag == 0x3010) {
            dataOutputStream.writeShort(0);
        }
        dataOutputStream.writeShort(length);
    }

    private void writeString(DataOutputStream dataOutputStream, String str, int length) throws IOException {
        dataOutputStream.writeBytes(str);
        byte[] b = new byte[length - str.length()];
        dataOutputStream.write(b);
    }

    private int checkTag(DataInputStream dataInputStream, int expectedTag, int expectedSize) throws IOException, BrokenTicketException {
        dataInputStream.mark(6);
        short tag = dataInputStream.readShort();
        if(tag == 0 ||tag == 0x3000 || tag == 0x3010) {
            dataInputStream.readShort();
        }
        short size = dataInputStream.readShort();
        if(tag != expectedTag) {
            dataInputStream.reset();
            throw new BrokenTicketException("unexpected tag");
        }
        if(expectedSize != -1 && size != expectedSize) {
            throw new BrokenTicketException("unexpected size");
        }
        return size;
    }
}

