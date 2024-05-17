package yuv.pink.npticket;

public class BrokenTicketException extends Throwable {
    private String message;
    BrokenTicketException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
