package com.si.gateway.entry_point.soap.processor;

import com.si.gateway.common.processor.IDataProcessor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

@Slf4j
public class AmadeusBMP implements IDataProcessor {

    @Override
    public JSONObject process(String input) {
        log.debug("input: "+ input);
        JSONObject retVal = new JSONObject();
        if (input.contains("RecordLocator")) {
            String pnr = input.substring(input.indexOf("RecordLocator") + "RecordLocator".length(), input.indexOf("</RecordLocator"));
            pnr = pnr.substring(pnr.indexOf(">") + 1);
            log.debug("pnr = \"" + pnr + "\"");
            retVal.put("pnr", pnr);
        }
        return retVal;
    }
}
