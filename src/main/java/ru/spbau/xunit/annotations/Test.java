package ru.spbau.xunit.annotations;

import javax.annotation.Nonnull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for marking methods as tests.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Test {
    String ACTIVE = "not_ignored";
    String NO_EXCEPTIONS = "no_exceptions";

    /**
     * A parameter for the reason to ignore the test. The test is run only if the value
     * is default.
     * @return ACTIVE if the test is not ignored. Reason to ignore otherwise.
     */
    String ignoreReason() default ACTIVE;

    /**
     * A parameter for the expected exception. In case the value has been changed, the
     * test is considered successful if this exception is thrown. Otherwise the test
     * is considered to be successful if no exception is throw.
     * @return the name of the expected exception or NO_EXCEPTIONS if no exceptions are
     * expected.
     */
    String expected() default NO_EXCEPTIONS;
}
