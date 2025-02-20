package com.liyunx.groot.util;

/**
 * <p><b>from https://github.com/leangen/geantyref/</b></p>
 *
 * Indicates that invalid data has been encountered during annotation creation.
 * Similar to {@link java.lang.annotation.AnnotationFormatError} but meant to be handled by the user.
 */
public class AnnotationFormatException extends Exception {

    private static final long serialVersionUID = -2680103741623459660L;

    AnnotationFormatException() {
        super();
    }

    AnnotationFormatException(String message) {
        super(message);
    }
}
