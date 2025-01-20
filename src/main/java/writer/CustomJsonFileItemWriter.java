package writer;

import com.arpan.demo_batch.exception.NotificationException;
import com.arpan.demo_batch.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.JsonObjectMarshaller;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.core.io.WritableResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

@Slf4j
public class CustomJsonFileItemWriter<T> extends JsonFileItemWriter<T> {
//    private final RetryTemplate retryTemplate;

    public CustomJsonFileItemWriter(WritableResource resource, JsonObjectMarshaller<T> jsonObjectMarshaller) {
        super(resource, jsonObjectMarshaller);
    }

    @Override
    public void write(Chunk<? extends T> items) throws Exception {
        System.out.println("<<<<<<<<<<<<<<Writing items: " + items); // Debugging log
        for (T item : items) {
            sendNotification((Transaction) item);
        }
        super.write(items);
    }

    /*@Override
    public void write(Chunk<? extends T> items) throws Exception {
        System.out.println("<<<<<<<<<<<<<<Writing items: " + items); // Debugging log
        for (T item : items) {
            retryTemplate.execute(context -> {
                try {
                    sendNotification((Transaction) item);
                } catch (NotificationException ex) {
                    log.error("Retrying notification for userId: {}", ((Transaction) item).getUserId());
                    throw ex; // Re-throw to trigger retry
                }
                return null; // Required return value for RetryTemplate
            });
        }
        super.write(items);
    }*/

    private void sendNotification(Transaction transaction) throws NotificationException {
        if (transaction.getEmail() == null) {
            log.error("Not able to send notification to userId: {}", transaction.getUserId());
            throw new NotificationException("Not able to send notification to userId: " + transaction.getUserId() + " with no email");
        } else {
            log.info(">>>>>>>> Published notification to user: {}", transaction.getUserId());
        }

    }
}
