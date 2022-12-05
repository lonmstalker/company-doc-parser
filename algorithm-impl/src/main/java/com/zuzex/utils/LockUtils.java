package com.zuzex.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.zuzex.data.algorithm.CompanyData;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;

@Slf4j
@UtilityClass
public class LockUtils {
    public static final ConcurrentHashMap.KeySetView<String, Boolean> LOCK_SET = ConcurrentHashMap.newKeySet();
    public static final LinkedBlockingQueue<Pair<CompanyData, JsonNode>> WAITING_QUEUE = new LinkedBlockingQueue<>();

    @SneakyThrows
    public void forceLock(final String companyId) {
        while (!LOCK_SET.add(companyId)) {
            TimeUnit.MILLISECONDS.sleep(10);
        }
    }

    public boolean tryLockCompany(final String companyId) {
        return LOCK_SET.add(companyId);
    }

    public void unlockCompany(@Nullable final String companyId) {
        if (nonNull(companyId)) {
            LOCK_SET.remove(companyId);
        }
    }

    public void nullCheckReleaseLock(@Nullable final String[] lockedChildCompanies) {
        if (nonNull(lockedChildCompanies)) {
            releaseLock(lockedChildCompanies);
        }
    }

    public void releaseLock(final String[] lockedChildCompanies) {
        for (final var lockedCompany : lockedChildCompanies) {
            unlockCompany(lockedCompany);
        }
    }

    // возвращает массив залоченных компаний, null - не получилось залочить
    @Nullable
    @SneakyThrows
    public String[] acquireLock(final CompanyData companyData, final Set<String> successTransactions, final JsonNode node) {
        int curLock = 0, curSize;

        String childName;
        List<String> children;
        Iterator<String> childrenIterator;

        final var lockedChildCompanies = new String[companyData.references().size()];
        final var referenceIterator = companyData.references().entrySet().iterator();

        while (referenceIterator.hasNext()) {
            children = referenceIterator.next().getValue();
            curSize = children.size();
            childrenIterator = children.iterator();
            while (childrenIterator.hasNext()) {
                childName = childrenIterator.next();
                if (!successTransactions.contains(childName)) {
                    if (tryLockCompany(childName)) {
                        lockedChildCompanies[curLock++] = childName;
                    } else {
                        // кладем в очередь и разлочиваем остальных
                        log.info("'{}' is locked, put in waiting", childName);
                        WAITING_QUEUE.put(Pair.of(companyData, node));
                        releaseLock(lockedChildCompanies);
                        return null;
                    }
                } else {
                    curSize--;  // уменьшаем кол-во необходимых для добавления компаний
                    childrenIterator.remove();
                    if (curSize == 0) { // если компаний нет больше, то удаляем весь список
                        referenceIterator.remove();
                    }
                }
            }

        }

        return lockedChildCompanies;
    }
}
