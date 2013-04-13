package com.splicemachine.si2.txn;

import com.splicemachine.constants.ITransactionManager;
import com.splicemachine.constants.ITransactionState;
import com.splicemachine.si2.si.api.ClientTransactor;
import com.splicemachine.si2.si.api.TransactionId;
import com.splicemachine.si2.si.api.Transactor;
import com.splicemachine.utils.SpliceLogUtils;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class TransactionManager implements ITransactionManager {
    static final Logger LOG = Logger.getLogger(TransactionManager.class);
    protected JtaXAResource xAResource;
    private final Transactor transactor;
    private static ThreadLocal<String> parentTransactionIdThreadLocal = new ThreadLocal<String>();

    public TransactionManager(final Transactor transactor) throws IOException {
        this.transactor = transactor;
    }

    public static void setParentTransactionId(String transactionId) {
        parentTransactionIdThreadLocal.set(transactionId);
    }

    public static String getParentTransactionId() {
        return parentTransactionIdThreadLocal.get();
    }

    public TransactionId beginTransaction(boolean allowWrites, boolean nested, boolean dependent, String parentTransactionID) throws KeeperException, InterruptedException, IOException, ExecutionException {
        SpliceLogUtils.trace(LOG, "Begin transaction");
        final String parentPerThreadLocal = parentTransactionIdThreadLocal.get();
        if (!nested && parentPerThreadLocal != null) {
            parentTransactionID = parentPerThreadLocal;
            nested = true;
            dependent = true;
        }
        if (nested) {
            final TransactionId parentTransaction = ((ClientTransactor) transactor).transactionIdFromString(parentTransactionID);
            return transactor.beginChildTransaction(parentTransaction, dependent, allowWrites, null, null);
        } else {
            return transactor.beginTransaction(true, false, false);
        }
    }

    public int prepareCommit(final ITransactionState transaction) throws KeeperException, InterruptedException, IOException {
        SpliceLogUtils.trace(LOG, "prepareCommit %s", transaction);
        return 0;
    }

    @Override
    public void prepareCommit2(Object bonus, ITransactionState transaction) throws KeeperException, InterruptedException, IOException {
        prepareCommit(transaction);
    }

    public void doCommit(final ITransactionState transaction) throws KeeperException, InterruptedException, IOException {
        SpliceLogUtils.trace(LOG, "doCommit %s", transaction);
        transactor.commit((TransactionId) transaction);
    }

    public void tryCommit(final ITransactionState transaction) throws IOException, KeeperException, InterruptedException {
        SpliceLogUtils.trace(LOG, "tryCommit %s", transaction);
        prepareCommit(transaction);
        doCommit(transaction);
    }

    public void abort(final ITransactionState transaction) throws IOException, KeeperException, InterruptedException {
        SpliceLogUtils.trace(LOG, "abort %s", transaction);
        transactor.abort((TransactionId) transaction);
    }

    public synchronized JtaXAResource getXAResource() {
        if (xAResource == null) {
            xAResource = new JtaXAResource(this);
        }
        return xAResource;
    }
}