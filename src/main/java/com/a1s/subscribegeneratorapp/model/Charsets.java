package com.a1s.subscribegeneratorapp.model;

import java.util.HashMap;

public class Charsets {
    private static final HashMap<Byte,String> charsets;

    private static final String NAME_ISO_8859_1 = "ISO-8859-1";
    private static final String NAME_GSM = "GSM";
    private static final String NAME_UCS_2 = "UCS-2";
    private static final String NAME_UTF_8 = "UTF-8";

    static {
        charsets = new HashMap<>();
        charsets.put((byte)0x03, NAME_ISO_8859_1);
        charsets.put((byte)0x01, NAME_GSM);
        charsets.put((byte)0x08, NAME_UCS_2);
        charsets.put((byte)0x04, NAME_UTF_8);

    }

    public static String getCharsetName(byte dcs) {
        return charsets.get(dcs);

    }

}
