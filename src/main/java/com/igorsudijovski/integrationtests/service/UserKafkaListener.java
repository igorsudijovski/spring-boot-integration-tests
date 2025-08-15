package com.igorsudijovski.integrationtests.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igorsudijovski.integrationtests.client.model.UserDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserKafkaListener {

    private final ObjectMapper objectMapper;
    private final UserService userService;

    public UserKafkaListener(ObjectMapper objectMapper, UserService userService) {
        this.objectMapper = objectMapper;
        this.userService = userService;
    }

    @KafkaListener(topics = "user-address-updates", groupId = "address")
    public void listenUserTopic(String message) throws Exception {
        UserDto userDto = objectMapper.readValue(message, UserDto.class);
        userService.updateAddressFromKafka(userDto);
    }
}

