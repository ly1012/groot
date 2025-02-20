package com.liyunx.groot;

import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.functions.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ApplicationConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationConfigTest.class);

    @Test
    public void testFunctions() {
        // given
        List<Function> functions = new ArrayList<>();
        functions.add(new Function() {
            @Override
            public String getName() {
                return "longSum";
            }

            @Override
            public Long execute(ContextWrapper contextWrapper, List<Object> parameters) {
                long first = Long.parseLong(String.valueOf(parameters.get(0)));
                long second = Long.parseLong(String.valueOf(parameters.get(1)));
                return first + second;
            }
        });
        // when
        ApplicationConfig.setFunctions(functions);
        functions = ApplicationConfig.getFunctions();
        // then
        assertThat(functions.size()).isEqualTo(1);
        Function function = functions.get(0);
        assertThat(function.getName()).isEqualTo("longSum");
        assertThat(function.execute("100", "200")).isEqualTo(300L);
    }

}
