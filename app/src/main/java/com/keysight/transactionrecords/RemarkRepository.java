package com.keysight.transactionrecords;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

public class RemarkRepository
{
    private RemarkDao dao;
    private LiveData<List<Remark>> allElements;

    RemarkRepository(Application application)
    {
        TransactionRecordsRoomDatabase db = TransactionRecordsRoomDatabase.getDatabase(application);
        dao = db.remarkDao();
        allElements = dao.getAllElements();
    }

    LiveData<List<Remark>> getAllElements()
    {
        return allElements;
    }

    public void deleteAllElements()
    {
        new deleteAllElementsAsyncTask(dao).execute();
    }

    public void insert(Remark remark)
    {
        new insertAsyncTask(dao).execute(remark);
    }

    public void insertElements(List<Remark> remarks)
    {
        new insertElementsAsyncTask(dao).execute(remarks);
    }

    private static class insertElementsAsyncTask extends AsyncTask<List<Remark>, Void, Void>
    {
        private RemarkDao mAsyncTaskDao;

        insertElementsAsyncTask(RemarkDao dao)
        {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final List<Remark>... params)
        {
            for (Remark remark : params[0])
            {
                mAsyncTaskDao.insert(remark);
            }
            return null;
        }
    }

    private static class deleteAllElementsAsyncTask extends AsyncTask<Remark, Void, Void>
    {
        private RemarkDao mAsyncTaskDao;

        deleteAllElementsAsyncTask(RemarkDao dao)
        {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Remark... params)
        {
            mAsyncTaskDao.deleteAll();
            return null;
        }
    }

    private static class insertAsyncTask extends AsyncTask<Remark, Void, Void>
    {
        private RemarkDao mAsyncTaskDao;

        insertAsyncTask(RemarkDao dao)
        {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Remark... params)
        {
            mAsyncTaskDao.insert(params[0]);
            return null;
        }
    }
}
