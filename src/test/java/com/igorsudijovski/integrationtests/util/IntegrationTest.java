package com.igorsudijovski.integrationtests.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.igorsudijovski.integrationtests.client.ApiClient;
import com.igorsudijovski.integrationtests.client.api.DefaultApi;
import com.igorsudijovski.integrationtests.client.model.AddressDto;
import com.igorsudijovski.integrationtests.client.model.UserDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IntegrationTest {

    @LocalServerPort
    private int port;

    private final static PostgreSQLContainer<?> POSTRES = new PostgreSQLContainer<>("postgres:16.9")
            .withDatabaseName("demo")
            .withUsername("demo")
            .withPassword("demo");

    private final static KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

    private final static WireMockContainer WIREMOCK = new WireMockContainer(WireMockContainer.OFFICIAL_IMAGE_NAME);

    private DefaultApi defaultApi;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        POSTRES.start();
        KAFKA.start();
        WIREMOCK.withMappingFromResource("wiremock/mappings/keycloak.json")
                .withFileFromResource("keycloak.json", "wiremock/__files/keycloak.json");
        WIREMOCK.start();

        registry.add("spring.datasource.url", POSTRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTRES::getUsername);
        registry.add("spring.datasource.password", POSTRES::getPassword);

        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("spring.security.oauth2.resourceserver.jwt.jwt-set-uri", () ->
            WIREMOCK.getBaseUrl() + "/idp/realms/user/protocol/openid-connect/certs"
        );
    }

    @BeforeAll
    void setup() {
        // Start WireMock simulating Keycloak public keys endpoint
        ApiClient apiClient = new ApiClient();
        apiClient.addDefaultHeader("Authorization", "Bearer " + JwtGenerator.generateJwt(WIREMOCK.getBaseUrl()));
        apiClient.setBasePath("http://localhost:" + port);

        defaultApi = new DefaultApi(apiClient);

    }

    @AfterAll
    void teardown() {
        WIREMOCK.stop();
        KAFKA.stop();
        POSTRES.stop();
    }

    @Test
    void testCreateUserAndKafkaIntegration() throws Exception {

        // verify kafka produced message by consuming the topic directly
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "address");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        consumerProps.put("spring.json.trusted.packages", "*");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(List.of("user"));
        // Create UserDto
        AddressDto addressDto = new AddressDto();
        addressDto.setAddress("Main St");
        addressDto.setNumber("123");
        addressDto.setCity("Metropolis");
        addressDto.setPostalCode("12345");
        addressDto.setCountry("USA");

        UserDto userDto = new UserDto();
        userDto.setUsername("johndoe");
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setAddress(addressDto);

        UserDto createdUser = defaultApi.createUser(userDto).block();
        Assertions.assertNotNull(createdUser);
        Assertions.assertEquals("johndoe", createdUser.getUsername());

        // Wait briefly for Kafka listener to process any address update messages
        Thread.sleep(2000);

        UserDto retrievedUser = defaultApi.getUser("johndoe").block();
        Assertions.assertNotNull(retrievedUser);
        Assertions.assertEquals("Main St", retrievedUser.getAddress().getAddress());

        ConsumerRecords<String, String> poll = consumer.poll(Duration.ofSeconds(15));
        Assertions.assertTrue( poll.count() > 0);

        boolean found = false;
        UserDto userFromKafka = null;
        for (ConsumerRecord<String, String> r : poll) {
            if (userDto.getUsername().equals(r.key())) {
                found = true;
                userFromKafka = objectMapper.readValue(r.value(), UserDto.class);
            }
        }
        Assertions.assertTrue(found);
        Assertions.assertEquals("Main St", userFromKafka.getAddress().getAddress());
        consumer.close();

    }

    @Test
    void testCreateUserAndKafkaAndAddressUpdate() throws Exception {
        // prepare JWT
        // create user via REST
        AddressDto addressDto = new AddressDto();
        addressDto.setAddress("New street");
        addressDto.setNumber("1234");
        addressDto.setCity("Bitola");
        addressDto.setPostalCode("1000");
        addressDto.setCountry("Macedonia");

        UserDto userDto = new UserDto();
        userDto.setUsername("johndoe");
        userDto.setAddress(addressDto);


        // send an address update message to topic 'user-address-updates' simulating external change
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);
        producer.send(new ProducerRecord<>("user-address-updates", userDto.getUsername(), objectMapper.writeValueAsString(userDto)));
        producer.flush();
        producer.close();

        // wait briefly for consumer inside app to handle update
        Thread.sleep(3000);

        UserDto retrievedUser = defaultApi.getUser("johndoe").block();
        Assertions.assertNotNull(retrievedUser);
        Assertions.assertEquals("New street", retrievedUser.getAddress().getAddress());
        Assertions.assertEquals("Macedonia", retrievedUser.getAddress().getCountry());
    }
}

