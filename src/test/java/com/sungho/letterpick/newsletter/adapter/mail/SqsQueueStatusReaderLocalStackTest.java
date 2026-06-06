package com.sungho.letterpick.newsletter.adapter.mail;

import com.sungho.letterpick.LetterPickAwsTestConfiguration;
import com.sungho.letterpick.LetterPickTestConfiguration;
import com.sungho.letterpick.newsletter.application.required.QueueStatusSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;

import static org.assertj.core.api.Assertions.assertThat;

@Import({LetterPickTestConfiguration.class, LetterPickAwsTestConfiguration.class})
@SpringBootTest(properties = {
        "spring.cloud.aws.sqs.enabled=true",
        "letterpick.mail.sqs-listener.enabled=false",
        "letterpick.mail.receive-queue=letterpick-mail-receive-test",
        "letterpick.mail.receive-dlq=letterpick-mail-receive-test-dlq"
})
@ActiveProfiles("test")
class SqsQueueStatusReaderLocalStackTest {

    private static final String RECEIVE_QUEUE_NAME = "letterpick-mail-receive-test";
    private static final String RECEIVE_DLQ_NAME = "letterpick-mail-receive-test-dlq";

    @Autowired
    private SqsAsyncClient sqsAsyncClient;

    @Autowired
    private SqsQueueStatusReader sqsQueueStatusReader;

    @BeforeEach
    void setUp() {
        createQueue(RECEIVE_QUEUE_NAME);
        createQueue(RECEIVE_DLQ_NAME);
    }

    @Test
    void readQueueStatus_reads_localstack_queue_attributes() {
        QueueStatusSnapshot snapshot = sqsQueueStatusReader.readQueueStatus();

        assertThat(snapshot.mainQueue().availableMessageCount()).isZero();
        assertThat(snapshot.mainQueue().inFlightMessageCount()).isZero();
        assertThat(snapshot.mainQueue().delayedMessageCount()).isZero();
        assertThat(snapshot.deadLetterQueue().availableMessageCount()).isZero();
    }

    private void createQueue(String queueName) {
        sqsAsyncClient.createQueue(CreateQueueRequest.builder()
                        .queueName(queueName)
                        .build())
                .join();
    }
}
