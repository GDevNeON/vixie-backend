package com.neong.vixie.controllers.wallet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neong.vixie.helpers.api.GlobalExceptionHandler;
import com.neong.vixie.models.db.User;
import com.neong.vixie.services.wallet.AdRewardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdRewardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdRewardService adRewardService;

    @InjectMocks
    private AdRewardController adRewardController;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(adRewardController)
                .setCustomArgumentResolvers(new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                        return org.springframework.security.core.userdetails.UserDetails.class.isAssignableFrom(parameter.getParameterType());
                    }
                    @Override
                    public Object resolveArgument(org.springframework.core.MethodParameter parameter, org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                                  org.springframework.web.context.request.NativeWebRequest webRequest, org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        return User.builder().id("user_1").role(com.neong.vixie.models.constant.Role.ROLE_USER).build();
                    }
                })
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    private void mockAuthentication(String userId) {
        User user = User.builder().id(userId).role(com.neong.vixie.models.constant.Role.ROLE_USER).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );
    }

    @Test
    void claimAdReward_success_returnsNewBalance() throws Exception {
        User user = User.builder().id("user_1").role(com.neong.vixie.models.constant.Role.ROLE_USER).build();
        
        when(adRewardService.rewardAdWatch("user_1")).thenReturn(51);

        mockMvc.perform(post("/api/coins/ad-reward")
                        .principal(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(51));
    }

    @Test
    void getAdRemaining_success_returnsRemaining() throws Exception {
        User user = User.builder().id("user_1").role(com.neong.vixie.models.constant.Role.ROLE_USER).build();
        
        when(adRewardService.getRemainingAdWatches("user_1")).thenReturn(5);

        mockMvc.perform(get("/api/coins/ad-remaining")
                        .principal(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remaining").value(5));
    }
}
