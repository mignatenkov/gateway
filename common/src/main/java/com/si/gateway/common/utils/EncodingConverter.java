package com.si.gateway.common.utils;

import com.si.gateway.common.properties_reader.PropertiesReaderSingleton;

import java.nio.charset.Charset;
import java.util.List;

public class EncodingConverter {

    public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static String convertFromISOtoUTF8(String input) {
        if (PropertiesReaderSingleton.getInstance().getConfig().getBoolean("system.aliases.enabled")) {
            for (String aliasPair : (List<String>) PropertiesReaderSingleton.getInstance().getConfig().getList("system.aliases.list")) {
                if (aliasPair.substring(0, aliasPair.indexOf(":")).equalsIgnoreCase(input)) {
                    byte ptext[] = aliasPair.substring(aliasPair.indexOf(":") + 1).getBytes(ISO_8859_1);
                    String value = new String(ptext, UTF_8);
                    return value;
                }
            }
        }
        return input;
    }

}
