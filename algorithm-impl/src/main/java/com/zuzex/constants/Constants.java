package com.zuzex.constants;

import com.zuzex.utils.PathUtils;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.SystemUtils;

import java.util.regex.Pattern;

@UtilityClass
public class Constants {
    public static final boolean IS_WINDOWS = SystemUtils.OS_NAME.startsWith("Windows");
    public static final String ACTUAL_DATE_FILE_FORMAT = "csv";
    public static final String PATH_DELIMITER = IS_WINDOWS ? "\\" : "/";
    public static final Pattern SPLIT_PATTERN = Pattern.compile(IS_WINDOWS ? "\\\\" : "/");
    public static final String UNZIP_PREFIX = PATH_DELIMITER + "unzip" + PATH_DELIMITER;
    public static final String CURRENT_PATH = PathUtils.removeLastPackageFromPath(System.getProperty("user.dir"));
}
