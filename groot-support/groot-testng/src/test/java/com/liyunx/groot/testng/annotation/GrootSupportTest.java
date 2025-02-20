package com.liyunx.groot.testng.annotation;

import com.liyunx.groot.support.Ref;
import com.liyunx.groot.testng.GrootTestNGTestCase;
import org.testng.annotations.Test;

import static com.liyunx.groot.DefaultVirtualRunner.repeat;
import static org.assertj.core.api.Assertions.assertThat;

public class GrootSupportTest extends GrootTestNGTestCase {

    @GrootSupport
    @Test
    public void testGrootSupport() {
        Ref<Integer> cnt = Ref.ref(0);
        repeat("dd", 3, () -> {
            cnt.value++;
        });
        assertThat(cnt.value).isEqualTo(3);
    }

}
