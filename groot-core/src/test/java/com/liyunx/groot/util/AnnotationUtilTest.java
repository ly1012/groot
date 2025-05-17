package com.liyunx.groot.util;

import org.testng.annotations.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotationUtilTest {

    @Test
    public void testHasAnnotation() {
        assert AnnotationUtil.hasAnnotation(Annotation1.class, Annotation1.class);
        assert AnnotationUtil.hasAnnotation(Annotation2.class, Annotation1.class);
        assert AnnotationUtil.hasAnnotation(Annotation3.class, Annotation1.class);
        assert !AnnotationUtil.hasAnnotation(Annotation1.class, Annotation2.class);
    }

    @Test
    public void testNewInstance() throws AnnotationFormatException {
        Map<String, Object> values = Map.of("value", "test", "ignore", true);
        Annotation1 annotation1 = AnnotationUtil.newInstance(Annotation1.class, values);
        assertThat(annotation1.value()).isEqualTo("test");
        assertThat(annotation1.ignore()).isTrue();
    }

}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@interface Annotation1 {

    String value() default "miss";

    boolean ignore() default false;

}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Annotation1
@interface Annotation2 {

}

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Annotation2
@interface Annotation3 {

}


