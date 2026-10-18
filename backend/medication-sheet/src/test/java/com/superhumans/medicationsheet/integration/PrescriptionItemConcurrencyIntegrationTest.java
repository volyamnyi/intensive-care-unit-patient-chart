package com.superhumans.medicationsheet.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superhumans.exception.BusinessException;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.repository.PrescriptionItemDayRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemRepository;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import com.superhumans.medicationsheet.service.PrescriptionItemService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** #388: concurrent day removal must preserve at least one day per item. */
@SpringBootTest(properties = "app.seed-data.enabled=false")
class PrescriptionItemConcurrencyIntegrationTest {

    @Autowired
    private PrescriptionItemService itemService;
    @Autowired
    private PrescriptionListRepository listRepository;
    @Autowired
    private PrescriptionItemRepository itemRepository;
    @Autowired
    private PrescriptionItemDayRepository dayRepository;
    @Autowired
    @Qualifier("medTransactionManager")
    private PlatformTransactionManager medTransactionManager;

    @Test
    void concurrentRemoveDayCallsKeepOneDay() throws Exception {
        UUID[] ids = createTwoDayItem();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Throwable>> futures = new ArrayList<>();
            for (UUID dayId : List.of(ids[1], ids[2])) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        itemService.removeDay(ids[0], dayId, 7L);
                        return null;
                    } catch (Throwable failure) {
                        return failure;
                    }
                }));
            }
            start.countDown();

            List<Throwable> failures = Arrays.asList(
                    futures.get(0).get(15, TimeUnit.SECONDS),
                    futures.get(1).get(15, TimeUnit.SECONDS));
            assertThat(failures).anyMatch(failure -> failure == null);
            assertThat(failures).anyMatch(failure -> failure instanceof BusinessException);

            long remaining = new TransactionTemplate(medTransactionManager).execute(status ->
                    dayRepository.findByItemIdAndDeletedFalseOrderByDayDateAsc(ids[0]).size());
            assertThat(remaining).isEqualTo(1);
        } finally {
            pool.shutdownNow();
            deleteItem(ids[0]);
        }
    }

    private UUID[] createTwoDayItem() {
        return new TransactionTemplate(medTransactionManager).execute(status -> {
            PrescriptionList list = listRepository.saveAndFlush(PrescriptionList.builder()
                    .patientId(990_388L)
                    .documentName("#388 concurrent remove")
                    .status("Saved")
                    .build());
            PrescriptionItem item = itemRepository.saveAndFlush(PrescriptionItem.builder()
                    .list(list)
                    .medicineName("#388 test item")
                    .medicineMethod("Oral")
                    .regime("once")
                    .status("Active")
                    .sortOrder(0)
                    .build());
            PrescriptionItemDay first = dayRepository.saveAndFlush(PrescriptionItemDay.builder()
                    .item(item)
                    .dayDate(LocalDate.of(2026, 10, 1))
                    .build());
            PrescriptionItemDay second = dayRepository.saveAndFlush(PrescriptionItemDay.builder()
                    .item(item)
                    .dayDate(LocalDate.of(2026, 10, 2))
                    .build());
            return new UUID[]{item.getId(), first.getId(), second.getId()};
        });
    }

    private void deleteItem(UUID itemId) {
        new TransactionTemplate(medTransactionManager).executeWithoutResult(status ->
                itemRepository.findById(itemId).ifPresent(item -> {
                    item.getDays().forEach(day -> day.setDeleted(true));
                    item.setDeleted(true);
                    itemRepository.saveAndFlush(item);
                }));
    }
}
