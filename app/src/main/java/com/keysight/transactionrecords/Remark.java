package com.keysight.transactionrecords;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

@Entity(tableName = "remark_table")
public class Remark
{
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "remark")
    private String remark;

    public Remark(@NonNull String remark)
    {
        this.remark = remark;
    }

    public String getRemark()
    {
        return this.remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
