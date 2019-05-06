package com.keysight.transactionrecords;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import java.util.List;

public class RemarkViewModel extends AndroidViewModel
{
    private RemarkRepository mRepository;

    private LiveData<List<Remark>> mAllElements;

    public RemarkViewModel(Application application)
    {
        super(application);
        mRepository = new RemarkRepository(application);
        mAllElements = mRepository.getAllElements();
    }

    LiveData<List<Remark>> getAllElements()
    {
        return mAllElements;
    }

    public void insert(Remark remark)
    {
        mRepository.insert(remark);
    }

    public void insertElements(List<Remark> remarks)
    {
        mRepository.insertElements(remarks);
    }

    public void deleteAllElements()
    {
        mRepository.deleteAllElements();
    }
}
