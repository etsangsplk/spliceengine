package com.splicemachine.si.impl.txn;

import com.splicemachine.si.api.txn.ChildStatementDuration;
import com.splicemachine.si.api.txn.TransactionStatus;
import com.splicemachine.si.api.txn.Txn;

/**
 * Created by jleach on 12/20/16.
 */
public class SimpleTxnImpl implements Txn {
    private long txnId;
    private long parentTxnId;
    private long commitTimestamp;
    private int nodeId;
    private int regionId;
    private long duration;
    private long[] rolledBackChildIds;
    private ChildStatementDuration childStatementDuration;
    private long hlcTimestamp;
    private String userId;
    private String statementId;
    private boolean persisted = false;


    @Override
    public long getTxnId() {
        return txnId;
    }

    @Override
    public long getParentTxnId() {
        return parentTxnId;
    }

    @Override
    public long getCommitTimestamp() {
        return commitTimestamp;
    }

    @Override
    public int getNodeId() {
        return nodeId;
    }

    @Override
    public int getRegionId() {
        return regionId;
    }

    @Override
    public long getDuration() {
        return duration;
    }

    @Override
    public long[] getRolledBackChildIds() {
        return rolledBackChildIds;
    }

    @Override
    public ChildStatementDuration getChildStatementDuration() {
        return childStatementDuration;
    }

    @Override
    public long getHLCTimestamp() {
        return hlcTimestamp;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public String getStatementId() {
        return statementId;
    }

    @Override
    public TransactionStatus getTransactionStatus() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setTxnId(long txnId) {
        this.txnId = txnId;
    }

    @Override
    public void setParentTxnId(long parentTxnId) {
        this.parentTxnId = parentTxnId;
    }

    @Override
    public void setCommitTimestamp(long commitTimestamp) {
        this.commitTimestamp = commitTimestamp;
    }

    @Override
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    @Override
    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Override
    public void setRolledBackChildIds(long[] rolledBackChildIds) {
        this.rolledBackChildIds = rolledBackChildIds;
    }

    @Override
    public void setChildStatementDuration(ChildStatementDuration childStatementDuration) {
        this.childStatementDuration = childStatementDuration;
    }

    @Override
    public void setHLCTimestamp(long hlcTimestamp) {
        this.hlcTimestamp = hlcTimestamp;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public void setStatementId(String statementId) {
        this.statementId = statementId;
    }

    @Override
    public void persist() {
        this.persisted = true;
    }

    @Override
    public int compareTo(Txn o) {
        return Long.compare(txnId,o.getTxnId()); // Is this right? JL
    }
}