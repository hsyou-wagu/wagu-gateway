package com.hsyou.wagugateway.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.hsyou.wagugateway.service.JwtTokenProvider;
import com.hsyou.wagugateway.model.AccountDTO;
import com.hsyou.wagugateway.model.ExternalAuthProvider;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.social.google.api.plus.Person;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/auth")
public class AuthController {

//    @Autowired
//    private AccountService accountService;
    @Autowired
    private EurekaClient discoveryClient;

    private static final ObjectMapper OBJECT_MAPPER=new ObjectMapper();

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("/login")
    public String login () throws IOException {

        return jwtTokenProvider.getLoginURL();
    }


    @GetMapping("/googlecallback")
    public ResponseEntity callback(@RequestParam String code, HttpServletRequest request, HttpServletResponse response){

        try {

            Person profile = jwtTokenProvider.getProfileFromAuthServer(code);

            System.out.println(profile.getId());

            String url = discoveryClient.getNextServerFromEureka("wagu-user", false).getHomePageUrl();

            AccountDTO accountDTO = new AccountDTO().builder()
                    .email(profile.getAccountEmail())
                    .uid(profile.getId())
                    .provider(ExternalAuthProvider.GOOGLE)
                    .build();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<String>(OBJECT_MAPPER.writeValueAsString(accountDTO), headers);

            String accntResponse = restTemplate.postForObject(url + "/account", entity, String.class);

            System.out.println(accntResponse);

            AccountDTO rstAccnt = OBJECT_MAPPER.readValue(accntResponse, AccountDTO.class);

            String token = jwtTokenProvider.createJWT(rstAccnt);

            HttpHeaders responseHeaders = new HttpHeaders();
            if (rstAccnt.getName() == null) {
                responseHeaders.setLocation(new URI("http://localhost:8080/account/info/"+token));
            } else {
                responseHeaders.setLocation(new URI("http://localhost:8080/done/"+token));
            }


            return new ResponseEntity<>(responseHeaders, HttpStatus.MOVED_PERMANENTLY);
        }catch (URISyntaxException ex){
            return new ResponseEntity<>(HttpStatus.HTTP_VERSION_NOT_SUPPORTED);
        } catch (Exception ex){
            System.out.println(ex.toString());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

    }

}
