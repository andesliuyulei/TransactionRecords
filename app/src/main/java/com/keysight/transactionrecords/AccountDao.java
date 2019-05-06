package com.keysight.transactionrecords;


import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface AccountDao
{
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Account account);

    @Query("DELETE FROM account_table")
    void deleteAll();

    @Query("SELECT * from account_table ORDER BY account_name ASC")
    LiveData<List<Account>> getAllElements();
}
