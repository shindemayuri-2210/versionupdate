package com.buildtools.version;

import java.util.regex.Pattern;


/**
 * File update configuration for different file types
 */
public enum FileConfig {
    SCONSTRUCT("SConstruct", Pattern.compile("point=\\d+"), "point="),
    VERSION("VERSION", Pattern.compile("ADLMSDK_VERSION_POINT=\\d+"), "ADLMSDK_VERSION_POINT=");

    final String fileName;
    final Pattern pattern;
    final String replacementPrefix;

    FileConfig(String fileName, Pattern pattern, String replacementPrefix) {
        this.fileName = fileName;
        this.pattern = pattern;
        this.replacementPrefix = replacementPrefix;
    }
}

