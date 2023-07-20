
package MQTT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class Topic {

    private String t_topicName;
    private List<String> t_data;    // an array list syncrhonized for access by multiple clients

    public Topic(String topicName) {

        t_topicName = topicName;
        t_data = Collections.synchronizedList(new ArrayList<String>());

    }

    public void addData(String data) {

        synchronized (t_data) {
            t_data.add(data);
        }

    }

    public String getName() {
        return t_topicName;
    }

    public byte[] getData() {

        byte[] data =  null;

        ArrayList<String> payloadAsString = new ArrayList<String>(t_data);

        // if there is data
        if (!t_data.isEmpty()) {

            try {
                data = Publish.generateNewBrokerUpdate(0, t_topicName, 1, payloadAsString);
            } catch (Exception e) {
                System.out.println("topic failed to generate update message " + e);
            }
        }

        return data;
    }

    public void clearData() {

        synchronized (t_data) {
            t_data.clear();
        }
    }

}
