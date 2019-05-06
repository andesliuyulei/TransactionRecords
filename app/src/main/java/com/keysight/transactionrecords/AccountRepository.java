package com.keysight.transactionrecords;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

public class AccountRepository
{
    private AccountDao dao;
    private LiveData<List<Account>> allElements;

    AccountRepository(Application application)
    {
        TransactionRecordsRoomDatabase db = TransactionRecordsRoomDatabase.getDatabase(application);
        dao = db.accountDao();
        allElements = dao.getAllElements();
    }

    LiveData<List<Account>> getAllElements()
    {
        return allElements;
    }

    public void deleteAllElements()
    {
        new deleteAllElementsAsyncTask(dao).execute();
    }

    public void insert(Account account)
    {
        new insertAsyncTask(dao).execute(account);
    }

    public void insertElements(List<Account> accounts)
    {
        new insertElementsAsyncTask(dao).execute(accounts);
    }

    private static class insertElementsAsyncTask extends AsyncTask<List<Account>, Void, Void>
    {
        private AccountDao mAsyncTaskDao;

        insertElementsAsyncTask(AccountDao dao)
        {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final List<Account>... params)
        {
            for (Account account : params[0])
            {
                mAsyncTaskDao.insert(account);
            }
            return null;
        }
    }

    private static class deleteAllElementsAsyncTask extends AsyncTask<Account, Void, Void>
    {
        private AccountDao mAsyncTaskDao;

        deleteAllElementsAsyncTask(AccountDao dao)
        {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Account... params)
        {
            mAsyncTaskDao.deleteAll();
            return null;
        }
    }

    private static class insertAsyncTask extends AsyncTask<Account, Void, Void>
    {
        private AccountDao mAsyncTaskDao;

        insertAsyncTask(AccountDao dao)
        {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Account... params)
        {
            mAsyncTaskDao.insert(params[0]);
            return null;
        }
    }
}
