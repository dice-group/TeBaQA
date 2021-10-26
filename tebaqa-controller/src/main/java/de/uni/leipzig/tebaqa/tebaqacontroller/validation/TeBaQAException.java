package de.uni.leipzig.tebaqa.tebaqacontroller.validation;

public class TeBaQAException extends Exception {
    private String message;
    private Throwable e;

    public TeBaQAException(String message, Throwable e) {
        super(message, e);
        this.message = message;
        this.e = e;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Throwable getE() {
        return e;
    }

    public void setE(Throwable e) {
        this.e = e;
    }
}
