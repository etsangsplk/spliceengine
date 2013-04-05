package org.apache.derby.impl.sql.execute.operations;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import com.splicemachine.derby.test.framework.SpliceDataWatcher;
import com.splicemachine.derby.test.framework.SpliceSchemaWatcher;
import com.splicemachine.derby.test.framework.SpliceTableWatcher;
import com.splicemachine.derby.test.framework.SpliceUnitTest;
import com.splicemachine.derby.test.framework.SpliceWatcher;

/**
 * This tests basic table scans with and without projection/restriction
 */
public class TableScanOperationTest extends SpliceUnitTest {
	private static Logger LOG = Logger.getLogger(TableScanOperationTest.class);
	protected static SpliceWatcher spliceClassWatcher = new SpliceWatcher();
	public static final String CLASS_NAME = TableScanOperationTest.class.getSimpleName().toUpperCase();
	public static final String TABLE_NAME = "A";
	protected static SpliceSchemaWatcher spliceSchemaWatcher = new SpliceSchemaWatcher(CLASS_NAME);	
	protected static SpliceTableWatcher spliceTableWatcher = new SpliceTableWatcher(TABLE_NAME,CLASS_NAME,"(si varchar(40),sa character varying(40),sc varchar(40),sd int,se float)");
	
	@ClassRule 
	public static TestRule chain = RuleChain.outerRule(spliceClassWatcher)
		.around(spliceSchemaWatcher)
		.around(spliceTableWatcher)
		.around(new SpliceDataWatcher(){
			@Override
			protected void starting(Description description) {
				try {
				PreparedStatement ps = spliceClassWatcher.prepareStatement(format("insert into %s.%s (si, sa, sc,sd,se) values (?,?,?,?,?)",CLASS_NAME, TABLE_NAME));
				for (int i =0; i< 10; i++) {
					ps.setString(1, "" + i);
					ps.setString(2, "i");
					ps.setString(3, "" + i*10);
					ps.setInt(4, i);
					ps.setFloat(5,10.0f*i);
					ps.executeUpdate();
				}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				finally {
					spliceClassWatcher.closeAll();
				}
			}
			
		});
	
	@Rule public SpliceWatcher methodWatcher = new SpliceWatcher();

	@Test
	public void testSimpleTableScan() throws Exception {			
			ResultSet rs = methodWatcher.executeQuery(format("select * from %s",this.getTableReference(TABLE_NAME)));
			int i = 0;
			while (rs.next()) {
				i++;
				Assert.assertNotNull(rs.getString(1));
				Assert.assertNotNull(rs.getString(2));				
				Assert.assertNotNull(rs.getString(3));				
			}	
			Assert.assertEquals(10, i);
	}

	@Test
	public void testScanForNullEntries() throws Exception{
		ResultSet rs = methodWatcher.executeQuery(format("select si from %s where si is null",this.getTableReference(TABLE_NAME)));
		boolean hasRows = false;
		List<String> results = Lists.newArrayList();
		while(rs.next()){
			hasRows=true;
			results.add(String.format("si=%s",rs.getString(1)));
		}

		if(hasRows){
			for(String row:results){
				LOG.info(row);
			}
			Assert.fail("Rows returned! expected 0 but was "+ results.size());
		}
	}

	@Test
	public void testQualifierTableScanPreparedStatement() throws Exception {
		PreparedStatement stmt = methodWatcher.prepareStatement(format("select * from %s where si = ?", this.getTableReference(TABLE_NAME)));
		stmt.setString(1,"5");
		ResultSet rs = stmt.executeQuery();
		int i = 0;
		while (rs.next()) {
			i++;
			LOG.info("a.si="+rs.getString(1)+",b.si="+rs.getString(2)+",c.si="+rs.getString(3));
			Assert.assertNotNull(rs.getString(1));
			Assert.assertNotNull(rs.getString(2));
			Assert.assertNotNull(rs.getString(3));
		}
		Assert.assertEquals(1, i);
	}

    @Test
	public void testOrQualifiedTableScanPreparedStatement() throws Exception {
		PreparedStatement stmt = methodWatcher.prepareStatement(format("select * from %s where si = ? or si = ?",this.getTableReference(TABLE_NAME)));
		stmt.setString(1,"5");
        stmt.setString(2,"4");
		ResultSet rs = stmt.executeQuery();
		int i = 0;
		while (rs.next()) {
			i++;
			LOG.info("a.si="+rs.getString(1)+",b.si="+rs.getString(2)+",c.si="+rs.getString(3));
			Assert.assertNotNull(rs.getString(1));
			Assert.assertNotNull(rs.getString(2));
			Assert.assertNotNull(rs.getString(3));
		}
		Assert.assertEquals(2, i);
	}

	@Test
	public void testQualifierTableScan() throws Exception {
		ResultSet rs = methodWatcher.executeQuery(format("select * from %s where si = '5'",this.getTableReference(TABLE_NAME)));
		int i = 0;
		while (rs.next()) {
			i++;
			LOG.info("a.si="+rs.getString(1)+",b.si="+rs.getString(2)+",c.si="+rs.getString(3));
			Assert.assertNotNull(rs.getString(1));
			Assert.assertNotNull(rs.getString(2));
			Assert.assertNotNull(rs.getString(3));
		}
		Assert.assertEquals(1, i);
	}

	@Test
	public void testRestrictedTableScan() throws Exception{
		ResultSet rs = methodWatcher.executeQuery("select si,sc from" + this.getPaddedTableReference("A"));
		int i = 0;
		while (rs.next()) {
			i++;
			LOG.info("a.si="+rs.getString(1)+",c.si="+rs.getString(2));
			Assert.assertNotNull(rs.getString(1));
			Assert.assertNotNull(rs.getString(2));
		}
		Assert.assertEquals(10, i);
	}

	@Test
	public void testScanIntWithLessThanOperator() throws Exception{
		ResultSet rs = methodWatcher.executeQuery("select  sd from" + this.getPaddedTableReference("A") +"where sd < 5");
		List<String> results  = Lists.newArrayList();
		while(rs.next()){
			int sd = rs.getInt(1);
			Assert.assertTrue("incorrect sd returned!",sd<5);
			results.add(String.format("sd:%d",sd));
		}
		for(String result:results){
			LOG.info(result);
		}
		Assert.assertEquals("Incorrect rows returned!",5,results.size());
	}

	@Test
	public void testScanIntWithLessThanEqualOperator() throws Exception{
		ResultSet rs = methodWatcher.executeQuery("select  sd from" + this.getPaddedTableReference("A") +"where sd <= 5");
		List<String> results  = Lists.newArrayList();
		while(rs.next()){
			int sd = rs.getInt(1);
			Assert.assertTrue("incorrect sd returned!",sd<=5);
			results.add(String.format("sd:%d",sd));
		}
		for(String result:results){
			LOG.info(result);
		}
		Assert.assertEquals("Incorrect rows returned!",6,results.size());
	}

	@Test
	public void testScanIntWithGreaterThanOperator() throws Exception{
		ResultSet rs = methodWatcher.executeQuery("select  sd from" + this.getPaddedTableReference("A") +"where sd > 5");
		List<String> results  = Lists.newArrayList();
		while(rs.next()){
			int sd = rs.getInt(1);
			Assert.assertTrue("incorrect sd returned!",sd>5);
			results.add(String.format("sd:%d",sd));
		}
		for(String result:results){
			LOG.info(result);
		}
		Assert.assertEquals("Incorrect rows returned!",4,results.size());
	}

	@Test
	public void testScanIntWithGreaterThanEqualsOperator() throws Exception{
		ResultSet rs = methodWatcher.executeQuery("select  sd from" + this.getPaddedTableReference("A") +"where sd >= 5");
		List<String> results  = Lists.newArrayList();
		while(rs.next()){
			int sd = rs.getInt(1);
			Assert.assertTrue("incorrect sd returned!",sd>=5);
			results.add(String.format("sd:%d",sd));
		}
		for(String result:results){
			LOG.info(result);
		}
		Assert.assertEquals("Incorrect rows returned!",5,results.size());
	}

	@Test
	public void testScanIntWithNotEqualsOperator() throws Exception{
		ResultSet rs = methodWatcher.executeQuery("select  sd from" + this.getPaddedTableReference("A") +"where sd != 5");
		List<String> results  = Lists.newArrayList();
		while(rs.next()){
			int sd = rs.getInt(1);
			Assert.assertTrue("incorrect sd returned!",sd!=5);
			results.add(String.format("sd:%d",sd));
		}
		for(String result:results){
			LOG.info(result);
		}
		Assert.assertEquals("Incorrect rows returned!",9,results.size());
	}


	@Test
	public void testScanFloatWithLessThanOperator() throws Exception{
		float correctCompare = 50f;
		ResultSet rs = methodWatcher.executeQuery("select se from" + this.getPaddedTableReference("A") +"where se < "+correctCompare);
		List<String> results  = Lists.newArrayList();
		while(rs.next()){
			float se = rs.getFloat(1);
			Assert.assertTrue("incorrect se returned!",se<correctCompare);
			results.add(String.format("se:%f",se));
		}
		for(String result:results){
			LOG.info(result);
		}
		Assert.assertEquals("Incorrect rows returned!",5,results.size());
	}

	@Test
	public void testScanFloatWithLessThanEqualOperator() throws Exception{
		float correctCompare = 50f;
		ResultSet rs = methodWatcher.executeQuery("select  se from" + this.getPaddedTableReference("A") +"where se <= "+correctCompare);
		List<String> results  = Lists.newArrayList();
		while(rs.next()){
			float se = rs.getFloat(1);
			Assert.assertTrue("incorrect se returned!se:"+se,se<=correctCompare);
			results.add(String.format("se:%f",se));
		}
		for(String result:results){
			LOG.info(result);
		}
		Assert.assertEquals("Incorrect rows returned!",6,results.size());
	}

	@Test
	public void testScanFloatWithGreaterThanOperator() throws Exception{
		float correctCompare = 50f;
		ResultSet rs = methodWatcher.executeQuery("select  se from" + this.getPaddedTableReference("A") +"where se > "+correctCompare);
		List<String> results  = Lists.newArrayList();
		while(rs.next()){
			float se = rs.getFloat(1);
			Assert.assertTrue("incorrect se returned!",se>5);
			results.add(String.format("se:%f",se));
		}
		for(String result:results){
			LOG.info(result);
		}
		Assert.assertEquals("Incorrect rows returned!",4,results.size());
	}

	@Test
	public void testScanFloatWithGreaterThanEqualsOperator() throws Exception{
		float correctCompare = 50f;
		ResultSet rs = methodWatcher.executeQuery("select  se from" + this.getPaddedTableReference("A") +"where se >= "+correctCompare);
		List<String> results  = Lists.newArrayList();
		while(rs.next()){
			float se = rs.getFloat(1);
			Assert.assertTrue("incorrect se returned!",se>=correctCompare);
			results.add(String.format("se:%f",se));
		}
		for(String result:results){
			LOG.info(result);
		}
		Assert.assertEquals("Incorrect rows returned!",5,results.size());
	}

	@Test
	public void testScanFloatWithNotEqualsOperator() throws Exception{
		float correctCompare = 50f;
		ResultSet rs = methodWatcher.executeQuery("select  se from" + this.getPaddedTableReference("A") +"where se != "+correctCompare);
		List<String> results  = Lists.newArrayList();
		while(rs.next()){
			float se = rs.getFloat(1);
			Assert.assertTrue("incorrect se returned!",se!=correctCompare);
			results.add(String.format("se:%f",se));
		}
		for(String result:results){
			LOG.info(result);
		}
		Assert.assertEquals("Incorrect rows returned!",9,results.size());
	}
}
