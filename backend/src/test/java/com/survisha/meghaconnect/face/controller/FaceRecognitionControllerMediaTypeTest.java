package com.survisha.meghaconnect.face.controller;

import com.survisha.meghaconnect.exception.GlobalExceptionHandler;
import com.survisha.meghaconnect.face.dto.FaceResponses;
import com.survisha.meghaconnect.face.service.FaceRecognitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FaceRecognitionControllerMediaTypeTest {
    @Mock private FaceRecognitionService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        FaceRecognitionController controller = new FaceRecognitionController(service, Runnable::run);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void acceptsCanonicalJsonRequest() throws Exception {
        when(service.search(any(), eq(false))).thenReturn(FaceResponses.Search.builder()
                .success(true).matched(false).message("No matching visitor found.").build());

        MvcResult asyncResult = mockMvc.perform(post("/api/v1/face-recognition/search")
                        .principal(authentication())
                        .contentType("application/json")
                        .content("{\"photo\":\"/9j/AA==\",\"includeMatchedPhoto\":false}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(asyncResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void rejectsTextPlainWithStructured415() throws Exception {
        mockMvc.perform(post("/api/v1/face-recognition/search")
                        .principal(authentication())
                        .contentType("text/plain;charset=UTF-8")
                        .content("/9j/AA=="))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"))
                .andExpect(jsonPath("$.message").value("The request format is not supported. Use application/json."))
                .andExpect(jsonPath("$.path").value("/api/v1/face-recognition/search"));
    }

    @Test
    void rejectsBlankPhoto() throws Exception {
        mockMvc.perform(post("/api/v1/face-recognition/search")
                        .principal(authentication())
                        .contentType("application/json")
                        .content("{\"photo\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken("operator", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_DATA_ENTRY_OPERATOR")));
    }
}
