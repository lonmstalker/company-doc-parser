package com.zuzex.algorithm;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

public interface ParseAlgorithm {
    void parse(final LinkedBlockingQueue<File> archives);
}
