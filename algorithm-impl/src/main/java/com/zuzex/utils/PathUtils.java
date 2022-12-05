package com.zuzex.utils;

import lombok.experimental.UtilityClass;

import java.util.Optional;

import static com.zuzex.constants.Constants.PATH_DELIMITER;
import static com.zuzex.constants.Constants.SPLIT_PATTERN;

@UtilityClass
public class PathUtils {

    public String removeLastPackageFromPath(final String path) {
        final var sb = new StringBuilder(path.length());
        final var parts = SPLIT_PATTERN.split(path);
        final var preLastIndex = parts.length - 2;
        for (int i = 0; i < preLastIndex + 1; i++) {
            sb.append(parts[i]);
            if (i != preLastIndex) {
                sb.append(PATH_DELIMITER);
            }
        }
        return sb.toString();
    }
}
