package com.somdiproy.smartcode.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SuggestionsDeserializer extends JsonDeserializer<List<Suggestion>> {
    @Override
    public List<Suggestion> deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        List<Suggestion> suggestions = new ArrayList<>();
        
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    // Convert string to Suggestion object
                    Suggestion suggestion = new Suggestion();
                    suggestion.setDescription(item.asText());
                    suggestion.setTitle("Improvement Suggestion");
                    suggestion.setCategory("General");
                    suggestion.setImpact("MEDIUM");
                    suggestions.add(suggestion);
                } else if (item.isObject()) {
                    // Handle proper Suggestion objects
                    Suggestion suggestion = p.getCodec().treeToValue(item, Suggestion.class);
                    suggestions.add(suggestion);
                }
            }
        }
        return suggestions;
    }
}