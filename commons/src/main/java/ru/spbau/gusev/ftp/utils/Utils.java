package ru.spbau.gusev.ftp.utils;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;

/**
 * A auxiliary class with methods for performing nio operations.
 */
public class Utils {
    public static void writeStringToBuffer(@Nonnull String str,
                                              @Nonnull ByteBuffer buffer) {
        buffer.putInt(str.length());
        for (char c: str.toCharArray()) {
            buffer.putChar(c);
        }
    }
}
