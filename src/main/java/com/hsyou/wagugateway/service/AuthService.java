package com.hsyou.wagugateway.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsyou.wagugateway.model.AccountDTO;
import com.hsyou.wagugateway.model.ExternalAuthProvider;
import com.netflix.discovery.EurekaClient;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import com.netflix.ribbon.proxy.annotation.Hystrix;
import org.apache.commons.lang3.concurrent.CircuitBreakingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.hystrix.HystrixCommands;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.social.InternalServerErrorException;
import org.springframework.stereotype.Service;

import org.springframework.social.google.api.plus.Person;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

    @Autowired
    private RestTemplate restTemplate;
    private static final ObjectMapper OBJECT_MAPPER=new ObjectMapper();

    @Autowired
    private EurekaClient discoveryClient;

    @HystrixCommand(fallbackMethod = "reliable")
    public AccountDTO requestUserInfo(Person profile) throws Exception{
        String url = discoveryClient.getNextServerFromEureka("wagu-user", false).getHomePageUrl() + "/account";

        AccountDTO accountDTO = new AccountDTO().builder()
                .email(profile.getAccountEmail())
                .uid(profile.getId())
                .provider(ExternalAuthProvider.GOOGLE)
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<String>(OBJECT_MAPPER.writeValueAsString(accountDTO), headers);


        String accntResponse = restTemplate.postForObject(url, entity, String.class);

        return OBJECT_MAPPER.readValue(accntResponse, AccountDTO.class);
    }

    public AccountDTO reliable(Person profile){
        return null;
    }

}
