package org.uci.spacifyLib.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.uci.spacifyLib.dto.Rule;

import java.util.List;

public class SpacifyUtility {

    public static String serializeListOfRules(List<Rule> rules) throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(rules);
    }

    public static List<Rule> deserializeListOfRules(String roomRules) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(roomRules, new TypeReference<List<Rule>>(){});
    }
}
