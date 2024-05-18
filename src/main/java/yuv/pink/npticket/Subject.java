package yuv.pink.npticket;

public class Subject {
    public long accountID;
    public String onlineID;
    public byte[] region;
    public String domain;
    public String serviceID;
    public NPDate dob;
    public int status;
    public long duration;

    public Subject(long accountID, String onlineID, byte[] region, String domain, String serviceID, NPDate dob, int status, long duration) {
        this.accountID = accountID;
        this.onlineID = onlineID;
        this.region = region;
        this.domain = domain;
        this.serviceID = serviceID;
        this.dob = dob;
        this.status = status;
        this.duration = duration;
    }
}
