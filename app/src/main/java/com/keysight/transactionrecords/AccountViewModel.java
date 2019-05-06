package com.keysight.transactionrecords;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

public class AccountViewModel extends AndroidViewModel
{
    private AccountRepository mRepository;

    private LiveData<List<Account>> mAllElements;

    public AccountViewModel(Application application)
    {
        super(application);
        mRepository = new AccountRepository(application);
        mAllElements = mRepository.getAllElements();
    }

    LiveData<List<Account>> getAllElements()
    {
        return mAllElements;
    }

    public void insert(Account account)
    {
        mRepository.insert(account);
    }

    public void insertElements(List<Account> accounts)
    {
        mRepository.insertElements(accounts);
    }

    public void deleteAllElements()
    {
        mRepository.deleteAllElements();
    }
}
