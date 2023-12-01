package org.corgi.consumer.sourcedownload.service.sqsconsumer;

import com.amazon.sqs.javamessaging.SQSConnection;
import com.amazon.sqs.javamessaging.SQSSession;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import org.corgi.consumer.sourcedownload.service.download.DownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;

@Component
public class SourceDownloadConsumer {

    private final static String QUEUE_NAME = "source_download";

    @Autowired
    private Session sqsSession;

    @Autowired
    private SQSConnection sqsConnection;

    @Autowired
    private DownloadService sourceDownloadService;

    private static final Logger logger = LoggerFactory.getLogger(SourceDownloadConsumer.class);

    class SourceDownloadMessageListener implements MessageListener {
        @Override
        public void onMessage(Message message) {
            try {
                // Cast the received message as TextMessage and print the text to screen.
                System.out.println("Received: " + ((TextMessage) message).getText());

                String ossKey = ((TextMessage) message).getText();
                sourceDownloadService.execFromS3(ossKey, message.getJMSMessageID());
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    @PostConstruct
    public void start() throws JMSException {
        Queue queue = sqsSession.createQueue(QUEUE_NAME);
        MessageConsumer consumer = sqsSession.createConsumer(queue);
        consumer.setMessageListener(new SourceDownloadMessageListener());
        sqsConnection.start();
    }
}
