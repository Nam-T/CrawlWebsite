import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;

public class CrawlWeb {
    private final static String QUEUE_NAME = "covid-19";
    private HashSet<String> links;

    public CrawlWeb() {
        links = new HashSet<String>();
    }

    public void getPageLinks(String URL) {
        if (!links.contains(URL)) {
            try {
                if (links.add(URL)) {
                    System.out.println(URL);
                }

                Document document = Jsoup.connect(URL).get();
                Elements linksOnPage = document.select("a[href]");

                for (Element page : linksOnPage) {
                    getPageLinks(page.attr("abs:href"));
                }
            } catch (IOException e) {
                System.err.println("For '" + URL + "': " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.err.println("For '" + URL + "': " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println(" [*] Waiting for messages.");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            new CrawlWeb().getPageLinks(message);


        };
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });



    }
}
