package com.somdiproy.smartcode.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SuggestionsDeserializer extends JsonDeserializer<List<Suggestion>> {
    @Override
    public List<Suggestion> deserialize(JsonParser p, DeserializationContext ctxt) 
            throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        List<Suggestion> suggestions = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        
        if (node.isArray()) {
            for (JsonNode item : node) {
                if (item.isTextual()) {
                    // Handle string suggestions (backward compatibility)
                    Suggestion suggestion = new Suggestion();
                    suggestion.setDescription(item.asText());
                    suggestion.setTitle("Improvement Suggestion");
                    suggestion.setCategory("General");
                    suggestion.setImpact("MEDIUM");
                    suggestions.add(suggestion);
                } else if (item.isObject()) {
                    // Handle object suggestions
                    Suggestion suggestion = new Suggestion();
                    
                    // Map fields from the Lambda response format
                    if (item.has("type")) {
                        suggestion.setCategory(item.get("type").asText());
                    }
                    if (item.has("description")) {
                        suggestion.setDescription(item.get("description").asText());
                    }
                    if (item.has("recommendation")) {
                        suggestion.setImplementation(item.get("recommendation").asText());
                    }
                    if (item.has("title")) {
                        suggestion.setTitle(item.get("title").asText());
                    } else {
                        // Generate title from type if not provided
                        suggestion.setTitle(suggestion.getCategory() + " Improvement");
                    }
                    if (item.has("impact")) {
                        suggestion.setImpact(item.get("impact").asText());
                    } else {
                        suggestion.setImpact("MEDIUM");
                    }
                    
                    suggestions.add(suggestion);
                }
            }
        }
        return suggestions;
    }
}