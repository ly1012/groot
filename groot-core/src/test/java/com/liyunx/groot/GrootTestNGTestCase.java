package com.liyunx.groot;

public class GrootTestNGTestCase extends AbstractGrootTestNGTestCase {

    private static final Groot groot = new Groot();

    @Override
    protected Groot getGroot() {
        return groot;
    }

}
