package com.somdiproy.smartcode.service;

import com.somdiproy.smartcode.dto.AnalysisResponse;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AnalysisStorageService {
    
    private final Map<String, AnalysisResponse> storage = new ConcurrentHashMap<>();
    
    public void storeAnalysis(String analysisId, AnalysisResponse response) {
        storage.put(analysisId, response);
    }
    
    public AnalysisResponse getAnalysis(String analysisId) {
        return storage.get(analysisId);
    }
    
    public void removeAnalysis(String analysisId) {
        storage.remove(analysisId);
    }
}