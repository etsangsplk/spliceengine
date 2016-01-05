package com.splicemachine.pipeline.callbuffer;

import com.carrotsearch.hppc.ObjectArrayList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.splicemachine.encoding.Encoding;
import com.splicemachine.hbase.KVPair;
import com.splicemachine.hbase.regioninfocache.RegionCache;
import com.splicemachine.si.impl.ActiveWriteTxn;
import com.splicemachine.metrics.Metrics;
import com.splicemachine.hbase.RegionCacheComparator;
import com.splicemachine.pipeline.api.BufferConfiguration;
import com.splicemachine.pipeline.api.WriteConfiguration;
import com.splicemachine.pipeline.api.WriteCoordinatorStatus;
import com.splicemachine.pipeline.api.Writer;
import com.splicemachine.pipeline.impl.BulkWrites;
import com.splicemachine.pipeline.impl.WriteCoordinator;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.ServerName;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.util.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import javax.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Assert;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Scott Fines
 *         Created on: 9/24/13
 */
public class PipingCallBufferTest{
	/*
	public static final String FOO_SERVERNAME="example.org,1234,1212121212";

    @Before
    public void setUp() throws Exception {

    }


		@Test
		public void testAddDataFlushThenAddMoreDataSameRegion() throws Exception {
				byte[] tableName = Bytes.toBytes("test");
				RegionCache regionCache = mock(RegionCache.class);

				//is BYTES_COMPARATOR correct here? it might put start in the incorrect location
				SortedSet<Pair<HRegionInfo,ServerName>> regions = getFakeRegions(tableName,5,5);
				when(regionCache.getRegions(tableName)).thenReturn(regions);

				final NavigableMap<byte[],List<BulkWrites>> outputs = Maps.newTreeMap(Bytes.BYTES_COMPARATOR);
				Writer fakedWriter = getFakeWriter(outputs);

				WriteConfiguration config = mock(WriteConfiguration.class);
				when(config.getMetricFactory()).thenReturn(Metrics.noOpMetricFactory());
				BufferConfiguration bufferConfig = new BufferConfiguration() {
						@Override public long getMaxHeapSize() { return 100; }
						@Override public int getMaxEntries() { return 100; }
						@Override public int getMaxFlushesPerRegion() { return 2; }
						@Override public void writeRejected() {  } };
				PipingCallBuffer buffer = new PipingCallBuffer(tableName,new ActiveWriteTxn(1l,1l),fakedWriter,regionCache,
				WriteCoordinator.noOpFlushHook,config,bufferConfig);

				byte[] column = Encoding.encode("value");
				ObjectArrayList<KVPair> added = ObjectArrayList.newInstanceWithCapacity(3);
				for(int i=2;i<4;i++){
						byte[] key = Encoding.encode(i);
						KVPair pair = new KVPair(key, column);
						buffer.add(pair);
						Assert.assertEquals("Buffer flushed prematurely!",0,outputs.size());
						added.add(pair);
				}
				//flush data, and make sure it all went to a single buffer, and that the BulkWrite is correct
				buffer.flushBuffer();

				//assert that the buffer didn't flush yet
				Assert.assertEquals("Buffer flushed too many regions!",1,outputs.size());
				List<BulkWrites> writes = outputs.get(Encoding.encode(2));
				Assert.assertEquals("Incorrect number of flushes!",1,writes.size());
				BulkWrites write = writes.get(0);
				Assert.assertEquals("Incorrect number of rows in the buffer",added.size(),write.getKVPairSize());
				Assert.assertEquals("Incorrect rows added to buffer!",added,write.getAllCombinedKeyValuePairs());

				outputs.clear();
				added.clear();
				for(int i=4;i<7;i++){
						byte[] key = Encoding.encode(i);
						KVPair pair = new KVPair(key, column);
						buffer.add(pair);
						Assert.assertEquals("Buffer flushed prematurely!",0,outputs.size());
						added.add(pair);
				}
				buffer.flushBuffer();

				//assert that the buffer didn't flush yet
				Assert.assertEquals("Buffer flushed too many regions!",1,outputs.size());
				writes = outputs.get(Encoding.encode(2));
				Assert.assertEquals("Incorrect number of flushes!",1,writes.size());
				write = writes.get(0);
				Assert.assertEquals("Incorrect number of rows in the buffer",added.size(),write.getKVPairSize());
				Assert.assertEquals("Incorrect rows added to buffer!",added,write.getAllCombinedKeyValuePairs());
		}

		@Test
		public void testAddDataFlushThenAddMoreDataDifferentRegions() throws Exception {
				byte[] tableName = Bytes.toBytes("test");
				RegionCache regionCache = mock(RegionCache.class);

				//is BYTES_COMPARATOR correct here? it might put start in the incorrect location
				SortedSet<Pair<HRegionInfo,ServerName>> regions = getFakeRegions(tableName,5,5);
				when(regionCache.getRegions(tableName)).thenReturn(regions);

				final NavigableMap<byte[],List<BulkWrites>> outputs = Maps.newTreeMap(Bytes.BYTES_COMPARATOR);
				Writer fakedWriter = getFakeWriter(outputs);

				WriteConfiguration config = mock(WriteConfiguration.class);
				when(config.getMetricFactory()).thenReturn(Metrics.noOpMetricFactory());
				BufferConfiguration bufferConfig = new BufferConfiguration() {
						@Override public long getMaxHeapSize() { return 100; }
						@Override public int getMaxEntries() { return 100; }
						@Override public int getMaxFlushesPerRegion() { return 2; }
						@Override public void writeRejected() {  } };
				PipingCallBuffer buffer = new PipingCallBuffer(tableName,new ActiveWriteTxn(1l,1l),fakedWriter,regionCache,
				WriteCoordinator.noOpFlushHook,config,bufferConfig);

				byte[] column = Encoding.encode("value");
				ObjectArrayList<KVPair> added = ObjectArrayList.newInstanceWithCapacity(3);
				for(int i=2;i<5;i++){
						byte[] key = Encoding.encode(i);
						KVPair pair = new KVPair(key, column);
						buffer.add(pair);
						Assert.assertEquals("Buffer flushed prematurely!",0,outputs.size());
						added.add(pair);
				}
				//flush data, and make sure it all went to a single buffer, and that the BulkWrite is correct
				buffer.flushBuffer();

				//assert that the buffer didn't flush yet
				Assert.assertEquals("Buffer flushed too many regions!",1,outputs.size());
				List<BulkWrites> writes = outputs.get(Encoding.encode(2));
				Assert.assertEquals("Incorrect number of flushes!",1,writes.size());
				BulkWrites write = writes.get(0);
				Assert.assertEquals("Incorrect number of rows in the buffer",added.size(),write.getKVPairSize());
				Assert.assertEquals("Incorrect rows added to buffer!",added,write.getAllCombinedKeyValuePairs());

				outputs.clear();
				added.clear();
				for(int i=7;i<9;i++){
						byte[] key = Encoding.encode(i);
						KVPair pair = new KVPair(key, column);
						buffer.add(pair);
						Assert.assertEquals("Buffer flushed prematurely!",0,outputs.size());
						added.add(pair);
				}
				buffer.flushBuffer();

				//assert that the buffer didn't flush yet
				Assert.assertEquals("Buffer flushed too many regions!",1,outputs.size());
				writes = outputs.get(Encoding.encode(7));
				Assert.assertEquals("Incorrect number of flushes!",1,writes.size());
				write = writes.get(0);
				Assert.assertEquals("Incorrect number of rows in the buffer",added.size(),write.getKVPairSize());
				Assert.assertEquals("Incorrect rows added to buffer!",added,write.getAllCombinedKeyValuePairs());
		}

		@Test
		public void testAddingUntilEntryLimitForcesFlush() throws Exception {
				byte[] tableName = Bytes.toBytes("test");
				RegionCache regionCache = mock(RegionCache.class);
				SortedSet<Pair<HRegionInfo,ServerName>> regions = getFakeRegions(tableName,5,1);
				when(regionCache.getRegions(tableName)).thenReturn(regions);

				final NavigableMap<byte[],List<BulkWrites>> outputs = Maps.newTreeMap(Bytes.BYTES_COMPARATOR);
				Writer fakedWriter = getFakeWriter(outputs);

				WriteConfiguration config = mock(WriteConfiguration.class);
				when(config.getMetricFactory()).thenReturn(Metrics.noOpMetricFactory());
				BufferConfiguration bufferConfig = new BufferConfiguration() {
						@Override public long getMaxHeapSize() { return 100; }
						@Override public int getMaxEntries() { return 0; }
						@Override public int getMaxFlushesPerRegion() { return 2; }
						@Override public void writeRejected() {  } };
				PipingCallBuffer buffer = new PipingCallBuffer(tableName,new ActiveWriteTxn(1l,1l),fakedWriter,regionCache,
				WriteCoordinator.noOpFlushHook,config,bufferConfig);

				//add an entry at the middle of the region
				byte[] key = Encoding.encode(1);
				KVPair pair = new KVPair(key,Encoding.encode("value"));
				buffer.add(pair);
				//assert that the buffer didn't flush yet
				Assert.assertEquals("Buffer flushed too many regions!",1,outputs.size());

				Assert.assertArrayEquals("Incorrect buffer flush location!",HConstants.EMPTY_START_ROW, outputs.floorKey(key));
				outputs.clear();
				buffer.flushBuffer();
				Assert.assertEquals("Buffer flushed more entries than it should!", 0, outputs.size());
		}

		protected SortedSet<Pair<HRegionInfo,ServerName>> getFakeRegions(byte[] tableName, int numRegions,final int regionSpace) {
				SortedSet<Pair<HRegionInfo,ServerName>> regions = new TreeSet<Pair<HRegionInfo,ServerName>>(new RegionCacheComparator());
				HRegionInfo info = new HRegionInfo(tableName, HConstants.EMPTY_START_ROW, Encoding.encode(2));
				regions.add(Pair.newPair(info,new ServerName(FOO_SERVERNAME)));
				for(int i=0,regionStart = 2;i<numRegions;i++,regionStart+=regionSpace){
						info = new HRegionInfo(tableName,Encoding.encode(regionStart),Encoding.encode(regionStart+regionSpace));
						regions.add(Pair.newPair(info, new ServerName(FOO_SERVERNAME)));
				}
				return regions;
		}
		@Test
		public void testAddingUntilHeapSizeForcesFlush() throws Exception {
				byte[] tableName = Bytes.toBytes("test");
				RegionCache regionCache = mock(RegionCache.class);

				//is BYTES_COMPARATOR correct here? it might put start in the incorrect location
				SortedSet<Pair<HRegionInfo,ServerName>> regions = getFakeRegions(tableName,5,1);
				when(regionCache.getRegions(tableName)).thenReturn(regions);

				final NavigableMap<byte[],List<BulkWrites>> outputs = Maps.newTreeMap(Bytes.BYTES_COMPARATOR);
				Writer fakedWriter = getFakeWriter(outputs);

				WriteConfiguration config = mock(WriteConfiguration.class);
				when(config.getMetricFactory()).thenReturn(Metrics.noOpMetricFactory());
				BufferConfiguration bufferConfig = new BufferConfiguration() {
						@Override public long getMaxHeapSize() { return 5; }
						@Override public int getMaxEntries() { return 10; }
						@Override public int getMaxFlushesPerRegion() { return 2; }
						@Override public void writeRejected() {  } };
				PipingCallBuffer buffer = new PipingCallBuffer(tableName,new ActiveWriteTxn(1l,1l),fakedWriter,regionCache,
				WriteCoordinator.noOpFlushHook,config,bufferConfig);

				//add some entries at the borders of the region
				byte[] key = Encoding.encode(2);
				KVPair pair = new KVPair(key,Encoding.encode("value"));
				buffer.add(pair);
				//assert that the buffer didn't flush yet
				Assert.assertEquals("Buffer flushed too many regions!",1,outputs.size());

				Assert.assertArrayEquals("Incorrect buffer flush region", key, outputs.floorKey(key));
				outputs.clear();
				buffer.flushBuffer();
				Assert.assertEquals("Buffer flushed more entries than it should!", 0, outputs.size());
		}

		@Test
		public void testAddingRowOnBoundaryOnlyAddedToOneRegion() throws Exception {
				byte[] tableName = Bytes.toBytes("test");
				RegionCache regionCache = mock(RegionCache.class);

				//is BYTES_COMPARATOR correct here? it might put start in the incorrect location
				SortedSet<Pair<HRegionInfo,ServerName>> regions = new TreeSet<Pair<HRegionInfo,ServerName>>(new RegionCacheComparator());
				for(int i=0;i<5;i++){
						HRegionInfo info = new HRegionInfo(tableName,Encoding.encode(i),Encoding.encode(i+1));
						regions.add(Pair.newPair(info,new ServerName(FOO_SERVERNAME)));
				}
				when(regionCache.getRegions(tableName)).thenReturn(regions);

				final NavigableMap<byte[],List<BulkWrites>> outputs = Maps.newTreeMap(Bytes.BYTES_COMPARATOR);
				Writer fakedWriter = getFakeWriter(outputs);

				WriteConfiguration config = mock(WriteConfiguration.class);
				when(config.getMetricFactory()).thenReturn(Metrics.noOpMetricFactory());
				BufferConfiguration bufferConfig = new BufferConfiguration() {
						@Override public long getMaxHeapSize() { return 100; }
						@Override public int getMaxEntries() { return 10; }
						@Override public int getMaxFlushesPerRegion() { return 2; }
						@Override public void writeRejected() {  } };
				PipingCallBuffer buffer = new PipingCallBuffer(tableName,new ActiveWriteTxn(1l,1l),fakedWriter,regionCache,
				WriteCoordinator.noOpFlushHook,config,bufferConfig);

				//add some entries at the borders of the region
				byte[] key = Encoding.encode(1);
				KVPair pair = new KVPair(key,Encoding.encode("value"));
				buffer.add(pair);
				//assert that the buffer didn't flush yet
				Assert.assertEquals("Buffer flushed prematurely!", 0, outputs.size());

				//now forcibly flush it
				buffer.flushBuffer();

				Assert.assertEquals("Buffer flushed too many regions!", 1, outputs.size());
				Assert.assertArrayEquals("Incorrect buffer flush region", key, outputs.floorKey(key));

				outputs.clear();
				buffer.flushBuffer();
				Assert.assertEquals("Buffer flushed more entries than it should!", 0, outputs.size());
		}
		protected Writer getFakeWriter(final SortedMap<byte[], List<BulkWrites>> outputs) throws ExecutionException {
				Writer fakedWriter = mock(Writer.class);
				when(fakedWriter.write(any(byte[].class),any(BulkWrites.class),any(WriteConfiguration.class)))
								.thenAnswer(new Answer<Future<Void>>() {
										@Override
										public Future<Void> answer(InvocationOnMock invocationOnMock) throws Throwable {
												Object[] arguments = invocationOnMock.getArguments();
												BulkWrites write = (BulkWrites) arguments[1];
												byte[] region = write.getRegionKey();
												List<BulkWrites> writes = outputs.get(region);
												if(writes==null){
														writes = Lists.newArrayList();
														outputs.put(region,writes);
												}
												writes.add(write);
												Future<Void> future = mock(Future.class);
												when(future.get()).thenReturn(null);
												return future;
										}
								});
				return fakedWriter;
		}
/*
		@Test
    public void testRebuildingRegionToBufferMap() throws Exception {
        byte[] tableName = Bytes.toBytes("testTable");
        FakedHBaseRegionCache regionCache = new FakedHBaseRegionCache(tableName);
        Monitor monitor = new Monitor(SpliceConstants.writeBufferSize,SpliceConstants.maxBufferEntries,SpliceConstants.numRetries,SpliceConstants.pause,SpliceConstants.maxFlushesPerRegion);

				WriteConfiguration config = mock(WriteConfiguration.class);
				when(config.getMetricFactory()).thenReturn(Metrics.noOpMetricFactory());
<<<<<<< HEAD:splice_machine/src/test/java/com/splicemachine/hbase/writer/PipingWriteBufferTest.java
        PipingWriteBuffer buffer = new PipingWriteBuffer(tableName, new ActiveWriteTxn(100,100), null, null, regionCache, null, config, monitor);
=======
        PipingCallBuffer buffer = new PipingCallBuffer(tableName, "100", null, regionCache, null, config, monitor);
>>>>>>> 96452a4... DB-1862 Write Pipeline Changes:splice_machine/src/test/java/com/splicemachine/pipeline/callbuffer/PipingCallBufferTest.java

        KVPair kv = new KVPair(Bytes.toBytes("aaaa"), Bytes.toBytes("1"));
        buffer.add(kv);

        kv = new KVPair(Bytes.toBytes("bbbc"), Bytes.toBytes("2"));
        buffer.add(kv);

        kv = new KVPair(Bytes.toBytes("dddd"), Bytes.toBytes("3"));
        buffer.add(kv);

        int [] r1 ={2, 1};
        TreeMap<Integer, Integer> map = buffer.getRegionToBufferCount();
        Set<Integer> set = map.keySet();
        Iterator<Integer> iterator = set.iterator();
        int i = 0;
        while(iterator.hasNext()) {
            int count = map.get(iterator.next()).intValue();
            Assert.assertEquals(count, r1[i]);
            ++i;
        }

        // force rebuilding region to buffer map
        buffer.setBuildBuffer();

        // fake table split
        regionCache.reload();

        //add another row and this should trigger a rebuild of region to buffer map
        kv = new KVPair(Bytes.toBytes("eeee"), Bytes.toBytes("4"));
        buffer.add(kv);

        int[] r2 = {1, 1, 2};
        map = buffer.getRegionToBufferCount();
        set = map.keySet();
        iterator = set.iterator();
        i = 0;
        while(iterator.hasNext()) {
            int count = map.get(iterator.next()).intValue();
            Assert.assertEquals(count, r2[i]);
            ++i;
        }
    }
*/
	/*
    @After
    public void tearDown() throws Exception {

    }

    public class FakedHBaseRegionCache implements RegionCache {
				private final byte[] tableName;
        private final ConcurrentHashMap<Integer,SortedSet<Pair<HRegionInfo, ServerName>>> regionCache;

        public  FakedHBaseRegionCache (byte[] tableName) {
            this.regionCache = new  ConcurrentHashMap<Integer,SortedSet<Pair<HRegionInfo, ServerName>>>();
						this.tableName = tableName;
            //populate the cache with HRegionInfo
            populateCache();
        }

        private void populateCache() {

            // Populate the cache with two regions
            Integer key = Bytes.mapKey(tableName);
            SortedSet<Pair<HRegionInfo, ServerName>> infos =  new ConcurrentSkipListSet<Pair<HRegionInfo, ServerName>>(new RegionCacheComparator());
            HRegionInfo info = new HRegionInfo(tableName, null, Bytes.toBytes("cccc"));
            infos.add(Pair.newPair(info, new ServerName(FOO_SERVERNAME)));
            info = new HRegionInfo(tableName, Bytes.toBytes("cccd"), null);
            infos.add(Pair.newPair(info,new ServerName(FOO_SERVERNAME)));
            regionCache.put(key, infos);
        }

        public void reload() {

            // Populate the cache with two regions
            Integer key = Bytes.mapKey(tableName);
            SortedSet<Pair<HRegionInfo,ServerName>> infos =  regionCache.get(key);
            infos.clear();
            HRegionInfo info = new HRegionInfo(tableName, null, Bytes.toBytes("bbba"));
            infos.add(Pair.newPair(info, new ServerName(FOO_SERVERNAME)));

            info = new HRegionInfo(tableName, Bytes.toBytes("bbbb"), Bytes.toBytes("cccc"));
            infos.add(Pair.newPair(info, new ServerName(FOO_SERVERNAME)));

            info = new HRegionInfo(tableName, Bytes.toBytes("cccd"), null);
            infos.add(Pair.newPair(info, new ServerName(FOO_SERVERNAME)));

            regionCache.put(key, infos);
        }

        @Override
        public long getUpdateTimestamp() {
            return 0;
        }

        @Override
        public void start() {
        }

        @Override
        public void shutdown() {
        }

        @Override
        public long size(){
            return regionCache.size();
        }

        @Override
        public void invalidate(byte[] tableName){
        }

        @Override
        public SortedSet<Pair<HRegionInfo, ServerName>> getRegions(byte[] tableName) throws ExecutionException {
            return regionCache.get(Bytes.mapKey(tableName));
        }

        @Override
        public void registerJMX(MBeanServer mbs) throws MalformedObjectNameException, NotCompliantMBeanException, InstanceAlreadyExistsException, MBeanRegistrationException {
        }

        @Override
        public SortedSet<Pair<HRegionInfo, ServerName>> getRegionsInRange(byte[] tableName, byte[] startRow, byte[] stopRow) throws ExecutionException {
            throw new UnsupportedOperationException("IMPLEMENT");
        }
    }

    private static class Monitor implements WriteCoordinatorStatus,BufferConfiguration{
        private volatile long maxHeapSize;
        private volatile int maxEntries;
        private volatile int maxRetries;
        private volatile int maxFlushesPerRegion;

        private AtomicInteger outstandingBuffers = new AtomicInteger(0);
        private volatile long pauseTime;
        private AtomicLong writesRejected = new AtomicLong(0l);

        private Monitor(long maxHeapSize, int maxEntries, int maxRetries,long pauseTime,int maxFlushesPerRegion) {
            this.maxHeapSize = maxHeapSize;
            this.maxEntries = maxEntries;
            this.maxRetries = maxRetries;
            this.pauseTime = pauseTime;
            this.maxFlushesPerRegion = maxFlushesPerRegion;
        }

        @Override public long getMaxBufferHeapSize() { return maxHeapSize; }
        @Override public void setMaxBufferHeapSize(long newMaxHeapSize) { this.maxHeapSize = newMaxHeapSize; }
        @Override public int getMaxBufferEntries() { return maxEntries; }
        @Override public void setMaxBufferEntries(int newMaxBufferEntries) { this.maxEntries = newMaxBufferEntries; }
        @Override public int getOutstandingCallBuffers() { return outstandingBuffers.get(); }
        @Override public int getMaximumRetries() { return maxRetries; }
        @Override public void setMaximumRetries(int newMaxRetries) { this.maxRetries = newMaxRetries; }
        @Override public long getPauseTime() { return pauseTime; }
        @Override public void setPauseTime(long newPauseTimeMs) { this.pauseTime = newPauseTimeMs; }
        @Override public long getMaxHeapSize() { return maxHeapSize; }
        @Override public int getMaxEntries() { return maxEntries; }
        @Override public int getMaxFlushesPerRegion() { return maxFlushesPerRegion; }
        @Override public void setMaxFlushesPerRegion(int newMaxFlushesPerRegion) { this.maxFlushesPerRegion = newMaxFlushesPerRegion; }

        @Override
        public long getSynchronousFlushCount() {
            return writesRejected.get();
        }

        @Override
        public void writeRejected() {
            this.writesRejected.incrementAndGet();
        }
    }
    */
}


