package yuv.pink.npticket;

import java.time.Instant;

class Entitlement {
    public String entitlementID;
    public Instant createdDate;
    public Instant expiredDate;
    public int type;
    public int remainingCount;
    public int consumedCount;
    public byte[] annotation;
}
