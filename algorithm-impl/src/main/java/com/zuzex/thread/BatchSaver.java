package com.zuzex.thread;

import com.zuzex.data.algorithm.ArchiveData;
import com.zuzex.service.DataService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@AllArgsConstructor
public class BatchSaver implements Runnable {
    private final AtomicBoolean end;
    private final DataService dataService;
    private final LinkedBlockingQueue<ArchiveData> archiveBarrier;
    private final List<ArchiveData> drainList = new ArrayList<>(10 * 3 / 2);

    @Override
    public void run() {
        try {
            while (!end.get()) {
                TimeUnit.SECONDS.sleep(2);
                if (archiveBarrier.size() > 20) {
                    this.save();
                    drainList.clear();
                }
            }
            // сохраняем, если что осталось
            if (!archiveBarrier.isEmpty()) {
                this.save();
            }
        } catch (final InterruptedException ex) {
            log.error("Error: ", ex);
        }
    }

    private void save() {
        archiveBarrier.drainTo(drainList);
        this.dataService.addArchives(drainList);
        log.info("Save in db '{}'", drainList.size());
    }
}
