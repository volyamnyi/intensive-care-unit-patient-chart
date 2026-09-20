package com.superhumans.medicationsheet.migration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * {@code app.drug-interaction.*} settings for the dataset import (issue #304).
 * The runner is active only under the {@code migration} profile; when the
 * {@code import-file} property is unset the import is a no-op.
 */
@Component
@ConfigurationProperties(prefix = "app.drug-interaction")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DrugInteractionImportProperties {

    /** Path to the dataset JSON; unset = the import runner stays silent. */
    String importFile;
}
