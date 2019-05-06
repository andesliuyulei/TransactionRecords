package com.keysight.transactionrecords;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "account_table")
public class Account
{
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "account_name")
    private String accountName;

    public Account(@NonNull String accountName)
    {
        this.accountName = accountName;
    }

    public String getAccountName()
    {
        return this.accountName;
    }

    public void setAccountName(String accountName)
    {
        this.accountName = accountName;
    }
}
