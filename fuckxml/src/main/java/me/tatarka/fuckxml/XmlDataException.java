package me.tatarka.fuckxml;

/**
 * Created by evan on 6/15/15.
 */
public class XmlDataException extends RuntimeException {
    public XmlDataException() {
        super();
    }

    public XmlDataException(String message) {
        super(message);
    }

    public XmlDataException(Throwable cause) {
        super(cause);
    }

    public XmlDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
