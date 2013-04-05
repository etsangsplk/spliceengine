package com.splicemachine.hbase;

import com.google.common.collect.Lists;
import com.splicemachine.constants.HBaseConstants;
import com.splicemachine.derby.utils.SpliceUtils;
import org.junit.Assert;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.MurmurHash;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * @author Scott Fines
 *         Created on: 3/21/13
 */
public class TableWriterTest {
    private static final Logger LOG = Logger.getLogger(TableWriterTest.class);

    private HBaseAdmin admin;
    private String tableName = "TABLEWRITER_TEST";
    private int numWrites = 10000;
    private ExecutorService executor;

    @Before
    public void setupTest() throws Exception{
        LOG.debug("Starting Hbase admin");
        admin = new HBaseAdmin(SpliceUtils.config);

        executor = Executors.newSingleThreadExecutor();

        LOG.debug("Creating hbase table");
        HTableDescriptor tableDesc = new HTableDescriptor(tableName);
        tableDesc.addCoprocessor("com.splicemachine.derby.hbase.SpliceIndexEndpoint");
        tableDesc.addFamily(new HColumnDescriptor(HBaseConstants.DEFAULT_FAMILY_BYTES));
        admin.createTable(tableDesc);
        while(!admin.isTableAvailable(tableName)){
            Thread.sleep(100);
        }

    }

    @After
    public void cleanupTest() throws Exception{
        LOG.info("Dropping table");
        admin.disableTableAsync(tableName);
        while(!admin.isTableDisabled(tableName)){
            Thread.sleep(100);
        }
        admin.deleteTable(tableName);
        executor.shutdownNow();
    }

    @Test
    public void testCanWriteTotable() throws Exception{
        TableWriter writer = TableWriter.create(SpliceUtils.config);
        writer.setCompressWrites(false);

        List<Row> rows = writeData(writer,numWrites);
        validateWrites(rows);
    }

    @Test
    public void testCanWriteToSplitTable() throws Exception{
        splitTable();
        //now attempt to write to the table
        TableWriter writer = TableWriter.create(SpliceUtils.config);
        writer.setCompressWrites(false);

        List<Row> rows = writeData(writer,numWrites);
        validateWrites(rows);
    }

    @Test
    public void canWriteWhileTableSplits() throws Exception{
        final CountDownLatch latch = new CountDownLatch(1);
        final TableWriter writer = TableWriter.create(SpliceUtils.config);
        writer.setCompressWrites(false);
        writer.setMaxBufferEntries(10);
        Future<List<Row>> future = executor.submit(new Callable<List<Row>>() {
            @Override
            public List<Row> call() throws Exception {
                List<Row> rows = writeData(writer,numWrites/2);
                latch.await();
                rows.addAll(writeData(writer,numWrites/2));
                return rows;
            }
        });

        //now that we're writing data, split the table
        Thread.sleep(100);
        splitTable();
        latch.countDown();

        List<Row> rows= future.get();
        validateWrites(rows);
    }

    private void validateWrites(List<Row> rows) throws IOException {
        LOG.info("Validating writes to table "+ tableName);
        HTableInterface table = new HTable(tableName);
        for(Row row:rows){
            Get get = new Get(row.getRow());
            get.addColumn(HBaseConstants.DEFAULT_FAMILY_BYTES,"1".getBytes());

            Assert.assertTrue("Row " + Bytes.toInt(row.getRow()) + " does not exist!", table.exists(get));
        }
    }

    private List<Row> writeData(TableWriter writer,int numToWrite) throws Exception {
        LOG.info("Writing data to table "+ tableName);
        writer.setMaxFlushesPerBuffer(1);
        CallBuffer<Mutation> buffer = writer.synchronousWriteBuffer(tableName.getBytes());
        List<Row> rows = Lists.newArrayList();
        Random random = new Random();
        for(int i=0;i<numToWrite;i++){
            Put put = new Put(Bytes.toBytes(random.nextInt(numWrites)));
            put.add(HBaseConstants.DEFAULT_FAMILY_BYTES,"1".getBytes(),Bytes.toBytes(i));
            buffer.add(put);
            rows.add(put);
        }
        buffer.flushBuffer();
        buffer.close();

        return rows;
    }

    private void splitTable() throws IOException, InterruptedException {
        LOG.info("Splitting table "+ tableName);
        //split the table
        admin.split(tableName.getBytes(),Bytes.toBytes(numWrites/2));

        boolean isSplitting=true;
        while(isSplitting){
            isSplitting=false;
            List<HRegionInfo> regions = admin.getTableRegions(tableName.getBytes());
            for(HRegionInfo region:regions){
                if(region.isSplit()||region.isOffline()){
                    isSplitting=true;
                    break;
                }
            }
            Thread.sleep(100);
        }
    }
}
