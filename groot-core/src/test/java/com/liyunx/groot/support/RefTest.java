package com.liyunx.groot.support;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RefTest {

    @Test
    public void testEquals() {
        Ref<String> r1 = Ref.ref("100");
        Ref<String> r2 = Ref.ref("100");
        Ref<String> r3  = Ref.ref("200");
        Ref<Integer> r4 = Ref.ref(100);

        assertThat(r1).isEqualTo(r2);
        assertThat(r1).isNotEqualTo(r3);
        assertThat(r1).isNotEqualTo(r4);
    }
}
