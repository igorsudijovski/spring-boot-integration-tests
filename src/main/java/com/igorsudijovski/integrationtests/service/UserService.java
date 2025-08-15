package com.igorsudijovski.integrationtests.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igorsudijovski.integrationtests.client.model.UserDto;
import com.igorsudijovski.integrationtests.mapper.UserMapper;
import com.igorsudijovski.integrationtests.model.UserEntity;
import com.igorsudijovski.integrationtests.repository.UserRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper,
                       KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UserDto createUser(UserDto userDto) throws JsonProcessingException {
        UserEntity userEntity = userMapper.toEntity(userDto);
        userRepository.save(userEntity);

        // send user info as JSON to Kafka topic
        String json = objectMapper.writeValueAsString(userDto);
        kafkaTemplate.send("user", userEntity.getUsername(), json);

        return userDto;
    }

    public UserDto getUser(String username) {
        return userRepository.findById(username)
                .map(userMapper::toDto)
                .orElse(null);
    }

    @Transactional
    public void updateAddressFromKafka(UserDto userDto) {
        userRepository.findById(userDto.getUsername()).ifPresent(userEntity -> {
            userEntity.setAddress(userMapper.toEntity(userDto).getAddress());
            userRepository.save(userEntity);
        });
    }
}

