package com.sungho.letterpick.newsletter.adapter.mail;

import com.sungho.letterpick.newsletter.application.required.QueueStatusReadException;
import com.sungho.letterpick.newsletter.application.required.QueueStatusReader;
import com.sungho.letterpick.newsletter.application.required.QueueStatusSnapshot;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.SqsException;

import static java.util.Objects.requireNonNull;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED;
import static software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE;

@Slf4j
@Component
public class SqsQueueStatusReader implements QueueStatusReader {

    private static final List<QueueAttributeName> MAIN_QUEUE_ATTRIBUTES = List.of(
            APPROXIMATE_NUMBER_OF_MESSAGES,
            APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE,
            APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED
    );
    private static final List<QueueAttributeName> DEAD_LETTER_QUEUE_ATTRIBUTES = List.of(
            APPROXIMATE_NUMBER_OF_MESSAGES
    );

    private final SqsClient sqsClient;
    private final String receiveQueueName;
    private final String receiveDlqName;

    public SqsQueueStatusReader(
            SqsClient sqsClient,
            @Value("${letterpick.mail.receive-queue}") String receiveQueueName,
            @Value("${letterpick.mail.receive-dlq}") String receiveDlqName
    ) {
        this.sqsClient = requireNonNull(sqsClient, "sqsClient must not be null");
        this.receiveQueueName = requireText(receiveQueueName, "receiveQueueName");
        this.receiveDlqName = requireText(receiveDlqName, "receiveDlqName");
    }

    @Override
    public QueueStatusSnapshot readQueueStatus() {
        try {
            Map<QueueAttributeName, String> mainQueueAttributes = getQueueAttributes(
                    getQueueUrl(receiveQueueName),
                    MAIN_QUEUE_ATTRIBUTES
            );
            Map<QueueAttributeName, String> deadLetterQueueAttributes = getQueueAttributes(
                    getQueueUrl(receiveDlqName),
                    DEAD_LETTER_QUEUE_ATTRIBUTES
            );

            return new QueueStatusSnapshot(
                    new QueueStatusSnapshot.MainQueueSnapshot(
                            getLongAttribute(mainQueueAttributes, APPROXIMATE_NUMBER_OF_MESSAGES),
                            getLongAttribute(mainQueueAttributes, APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE),
                            getLongAttribute(mainQueueAttributes, APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED)
                    ),
                    new QueueStatusSnapshot.DeadLetterQueueSnapshot(
                            getLongAttribute(deadLetterQueueAttributes, APPROXIMATE_NUMBER_OF_MESSAGES)
                    )
            );
        } catch (QueueDoesNotExistException e) {
            log.warn("SQS queue does not exist while reading mail queue status", e);
            throw new QueueStatusReadException("SQS queue does not exist", e);
        } catch (SqsException | SdkClientException e) {
            log.warn("Failed to read SQS mail queue status", e);
            throw new QueueStatusReadException("SQS queue status unavailable", e);
        }
    }

    private String getQueueUrl(String queueName) {
        return sqsClient.getQueueUrl(GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build())
                .queueUrl();
    }

    private Map<QueueAttributeName, String> getQueueAttributes(
            String queueUrl,
            List<QueueAttributeName> attributeNames
    ) {
        return sqsClient.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(attributeNames)
                        .build())
                .attributes();
    }

    private long getLongAttribute(Map<QueueAttributeName, String> attributes, QueueAttributeName attributeName) {
        String value = attributes.get(attributeName);
        if (value == null) {
            throw new QueueStatusReadException("SQS queue attribute is missing: " + attributeName);
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new QueueStatusReadException("SQS queue attribute is not a number: " + attributeName, e);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
