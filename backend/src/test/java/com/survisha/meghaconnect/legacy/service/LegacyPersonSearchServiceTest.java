package com.survisha.meghaconnect.legacy.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.legacy.dto.LegacyPersonSearchRequest;
import com.survisha.meghaconnect.legacy.repository.*;
import com.survisha.meghaconnect.service.AuditLogService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class LegacyPersonSearchServiceTest {
    @Test void rejectsEmptySearchWithoutQueryingTheDatabase(){
        LegacyPersonSearchService service=new LegacyPersonSearchService(mock(LegacyPersonIndexRepository.class),mock(LegacyDatasetRecordRepository.class),mock(LegacyDatasetDefinitionRepository.class),new ObjectMapper(),mock(AuditLogService.class));
        assertThrows(IllegalArgumentException.class,()->service.search(new LegacyPersonSearchRequest(),"admin"));
    }
}
