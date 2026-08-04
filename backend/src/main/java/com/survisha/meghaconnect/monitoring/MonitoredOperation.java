package com.survisha.meghaconnect.monitoring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MonitoredOperation {
    String value();
    Category category() default Category.BUSINESS;

    enum Category { BUSINESS, DATABASE, SCHEDULER }
}
