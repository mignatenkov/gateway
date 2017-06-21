package com.si.gateway.common.processor;

import org.json.simple.JSONObject;

public interface IDataProcessor {

    JSONObject process(String input);

}
