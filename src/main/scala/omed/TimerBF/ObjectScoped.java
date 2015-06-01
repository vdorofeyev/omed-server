package omed.TimerBF;

import com.google.inject.ScopeAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created with IntelliJ IDEA.
 * User: naryshkinaa
 * Date: 28.03.14
 * Time: 14:44
 * To change this template use File | Settings | File Templates.
 */
@Target({ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ScopeAnnotation
public @interface ObjectScoped {}
