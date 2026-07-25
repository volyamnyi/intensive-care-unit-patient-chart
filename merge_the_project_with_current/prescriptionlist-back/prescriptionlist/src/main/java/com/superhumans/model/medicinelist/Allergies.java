package com.superhumans.model.medicinelist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString
public class Allergies {
    Integer documentId;
    List<String> allergiesList;

    @SneakyThrows
    public void setAllergiesList(String xml) {
        if (xml == null || xml.isBlank()) {
            this.allergiesList = new ArrayList<>();
            return;
        }

        XmlMapper xmlMapper = new XmlMapper();
        JsonNode root = xmlMapper.readTree(xml);

        JsonNode itemsNode = root.path("item");

        this.allergiesList = StreamSupport.stream(
                        itemsNode.isArray()
                                ? itemsNode.spliterator()
                                : List.of(itemsNode).spliterator(),
                        false
                )
                .map(item -> item.path("dictInfo").path("Name").asText(null))
                .filter(Objects::nonNull)
                .toList();
    }

}
