package com.arpan.demo_batch.utils;

import org.springframework.batch.core.StepExecution;

public class LogUtils {
    public static void printLog(StepExecution stepExecution) {
        // Get skip count from StepExecution
        long skipCount = stepExecution.getSkipCount();
        long readCount = stepExecution.getReadCount();
        long writeCount = stepExecution.getWriteCount();
        long rollbackCount = stepExecution.getRollbackCount();
        long commitCount = stepExecution.getCommitCount();
        long filterCount = stepExecution.getFilterCount();
        long writeSkipCount = stepExecution.getWriteSkipCount();
        long readSkipCount = stepExecution.getReadSkipCount();
        int retryCount = stepExecution.getExecutionContext().getInt("retryCount", 0);

        // Print all StepExecution variables in a 2-column table format
        System.out.println("----------------------------------------------------");
        System.out.println("| Variable Name         | Value                   |");
        System.out.println("----------------------------------------------------");
        System.out.printf("| %-20s | %-22d |\n", "readCount", readCount);
        System.out.printf("| %-20s | %-22d |\n", "writeCount", writeCount);
        System.out.printf("| %-20s | %-22d |\n", "retryCount", retryCount);
        System.out.printf("| %-20s | %-22d |\n", "rollbackCount", rollbackCount);
        System.out.printf("| %-20s | %-22d |\n", "commitCount", commitCount);
        System.out.printf("| %-20s | %-22d |\n", "filterCount", filterCount);
        System.out.printf("| %-20s | %-22d |\n", "skipCount", skipCount);
        System.out.printf("| %-20s | %-22d |\n", "readSkipCount", readSkipCount);
        System.out.printf("| %-20s | %-22d |\n", "writeSkipCount", writeSkipCount);
        System.out.println("----------------------------------------------------");
    }
}
