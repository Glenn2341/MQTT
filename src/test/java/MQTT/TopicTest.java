package MQTT;

import org.junit.Before;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TopicTest {

    private Topic topic;

    @Before
    public void setUp() {
        topic = new Topic("TestTopic");
    }

    @Test
    public void testAddData() {
        // Arrange
        String data = "Test Data";

        // Act
        topic.addData(data);

        // Assert
        List<String> expectedData = Arrays.asList(data);
        assertEquals(expectedData, topic.getData());
    }

    @Test
    public void testGetName() {
        // Act
        String name = topic.getName();

        // Assert
        assertEquals("TestTopic", name);
    }

    @Test
    public void testGetData_WithData() throws Exception {
        // Arrange
        String data = "Test Data";
        topic.addData(data);

        // Act
        byte[] result = topic.getData();

        // Assert
        assertNotNull(result);
        // TODO: Add additional assertions for the generated update message if needed
    }

    @Test
    public void testGetData_WithoutData() throws Exception {
        // Act
        byte[] result = topic.getData();

        // Assert
        assertNull(result);
    }

    @Test
    public void testClearData() {
        // Arrange
        topic.addData("Test Data");

        // Act
        topic.clearData();

        // Assert
        List<String> expectedData = Arrays.asList();
        assertEquals(expectedData, topic.getData());
    }
}
