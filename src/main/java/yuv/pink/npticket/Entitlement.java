package yuv.pink.npticket;

public class Entitlement {
    public String entitlementID;
    public long createdDate;
    public long expiredDate;
    public int type;
    public int remainingCount;
    public int consumedCount;

    public Entitlement(String entitlementID, long createdDate, long expiredDate, int type, int remainingCount, int consumedCount) {
        this.entitlementID = entitlementID;
        this.createdDate = createdDate;
        this.expiredDate = expiredDate;
        this.type = type;
        this.remainingCount = remainingCount;
        this.consumedCount = consumedCount;
    }
}
