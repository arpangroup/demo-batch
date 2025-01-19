package writer;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.JsonObjectMarshaller;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.core.io.WritableResource;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

public class CustomJsonFileItemWriter<T> extends JsonFileItemWriter<T> {

    public CustomJsonFileItemWriter(WritableResource resource, JsonObjectMarshaller<T> jsonObjectMarshaller) {
        super(resource, jsonObjectMarshaller);
    }

    @Override
    public void write(Chunk<? extends T> items) throws Exception {
        System.out.println("<<<<<<<<<<<<<<Writing items: " + items); // Debugging log
        super.write(items);
    }
}
