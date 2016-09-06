/*
 * Apache Derby is a subproject of the Apache DB project, and is licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use these files
 * except in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * Splice Machine, Inc. has modified this file.
 *
 * All Splice Machine modifications are Copyright 2012 - 2016 Splice Machine, Inc.,
 * and are licensed to you under the License; you may not use this file except in
 * compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 */

package com.splicemachine.db.jdbc;

import java.sql.SQLException;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import com.splicemachine.db.client.net.NetLogWriter;
import com.splicemachine.db.client.am.LogWriter;
import com.splicemachine.db.client.am.SqlException;


/**
 * <p>
 * This is Derby's network XADataSource for use with JDBC3.0 and JDBC2.0.
 * </p>
 * An XADataSource is a factory for XAConnection objects.  It represents a
 * RM in a DTP environment.  An object that implements the XADataSource
 * interface is typically registered with a JNDI service provider.   	
 * <P>
 * ClientXADataSource automatically supports the correct JDBC specification version
 * for the Java Virtual Machine's environment.
 * <UL>
 * <LI> JDBC 3.0 - Java 2 - JDK 1.4, J2SE 5.0
 * <LI> JDBC 2.0 - Java 2 - JDK 1.2,1.3
 * </UL>
 *
 * <P>ClientXADataSource is serializable and referenceable.</p>
 *
 * <P>See ClientDataSource for DataSource properties.</p>
 */
public class ClientXADataSource extends ClientDataSource implements XADataSource {
    public static final String className__ = "com.splicemachine.db.jdbc.ClientXADataSource";

    // following serialVersionUID was generated by the JDK's serialver program
    // verify it everytime that ClientXADataSource is modified
    private static final long serialVersionUID = 7057075094707674880L;

    public ClientXADataSource() {
    }

    public XAConnection getXAConnection() throws SQLException {
        NetLogWriter dncLogWriter = null;
        try {
            updateDataSourceValues(
                    tokenizeAttributes(getConnectionAttributes(), null));
            dncLogWriter = (NetLogWriter)
                    super.computeDncLogWriterForNewConnection("_xads");
            return getXAConnectionX(
                    dncLogWriter, this, getUser(), getPassword());
        } catch (SqlException se) {
            // The method below may throw an exception.
            handleConnectionException(dncLogWriter, se);
            // If the exception wasn't handled so far, re-throw it.
            throw se.getSQLException();
        }
    }

    public XAConnection getXAConnection(String user, String password) throws SQLException {
        NetLogWriter dncLogWriter = null;
        try
        {
            updateDataSourceValues(
                    tokenizeAttributes(getConnectionAttributes(), null));
            dncLogWriter = (NetLogWriter)
                    super.computeDncLogWriterForNewConnection("_xads");
            return getXAConnectionX(dncLogWriter, this, user, password);
        }
        catch ( SqlException se )
        {
            // The method below may throw an exception.
            handleConnectionException(dncLogWriter, se);
            // If the exception wasn't handled so far, re-throw it.
            throw se.getSQLException();
        }
    }    

    /**
     * Method that establishes the initial physical connection
     * using DS properties instead of CPDS properties.
     */
    private XAConnection getXAConnectionX(LogWriter dncLogWriter,
        ClientBaseDataSource ds, String user, String password)
        throws SQLException
    {
        return ClientDriver.getFactory().newClientXAConnection(ds,
                dncLogWriter, user, password);
    }   
}