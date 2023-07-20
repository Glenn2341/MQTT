package MQTT;

import org.junit.Test;
import static org.assertj.core.api.Assertions.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

class BrokerTest {

    @Test
    void testProcessIncomingPublishMessage() {
        // Arrange
        MQTTBroker.Broker broker = new MQTTBroker.Broker(8080);  // assume a default constructor
        Publish pubMessage = mock(Publish.class);
        when(pubMessage.getTopicName()).thenReturn("Temperature");
        when(pubMessage.getPayload()).thenReturn("20");

        // Act
        broker.processIncomingPublishMessage(pubMessage);

        // Assert
        Topic topic = broker.getTopic("Temperature");  // assume a method to get a topic
        assertNotNull(topic);
        assertEquals("20", topic.getData());
    }
}
