package yuv.pink.npticket;

import java.security.*;
import java.security.interfaces.ECPublicKey;

public class Cipher {
    public final int id;
    public PublicKey publicKey;
    public PrivateKey privateKey;

    public PublicKey RSApublicKey;
    public PrivateKey RSAprivateKey;

    public Cipher(int id) {
        this.id = id;
    }

    public Cipher(int id, PrivateKey privateKey, PrivateKey RSAprivateKey) {
        this.id = id;
        this.privateKey = privateKey;
        this.RSAprivateKey = RSAprivateKey;
    }

    public Cipher(int id, PublicKey publicKey, PublicKey RSApublicKey) {
        this.id = id;
        this.publicKey = publicKey;
        this.RSApublicKey = RSApublicKey;
    }

    public byte[] sign(byte[] data) {
        if (this.privateKey == null) {
            return new byte[56];
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(data);
            return signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verify(byte[] data, byte[] signature) {
        if (this.privateKey == null) {
            return true;
        }
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] signRSA(byte[] data) {
        if (this.privateKey == null) {
            return new byte[192];
        }
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(RSAprivateKey);
            signature.update(data);
            return signature.sign();
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean verifyRSA(byte[] data, byte[] signature) {
        if (this.privateKey == null) {
            return true;
        }
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(RSApublicKey);
            sig.update(data);
            return sig.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    public int size() {
        if(publicKey == null) return 56;
        return publicKey instanceof ECPublicKey ? 56 : 64;
    }
}
