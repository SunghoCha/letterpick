package com.sungho.letterpick.trending.application;

public interface TrendingMessageProcessor {

    void process(String messageBody, String queueName);
}
