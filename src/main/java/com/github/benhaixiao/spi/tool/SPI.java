package com.github.benhaixiao.spi.tool;

import java.lang.annotation.*;

/**
 * @author xiaobenhai
 * Date: 2016/11/23
 * Time: 12:50
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {

    String value() default "";

}
