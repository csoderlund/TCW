package sng.assem.helpers;

import java.util.Arrays;
import java.util.Comparator;

// Simple automatically growing array, less memory than Vector
// If E implements Comparable, then can sort 
// Thread safe
// CAS304 added suppress warnings
public class AutoArray<E> 
{
	static Integer syncObj = 0;
	Object[] mObjList = null;
	int mNumFilled = 0;
	
	public AutoArray()
	{
	}
	
	public void setSize(int size)
	{
		clear();
		mObjList = new Object[size];
		for (int i = 0; i < size; i++)
		{
			mObjList[i] = null;
		}
	}
	public void clear()
	{
		if (mObjList == null) return;
		for (int i = 0; i < mObjList.length; i++)
		{
			mObjList[i] = null;
		}
		mObjList = null;
		mNumFilled = 0;
	}
	public int size()
	{
		//synchronized(syncObj) CAS405 remove
		{
			return mNumFilled;
		}
	}
	@SuppressWarnings("unchecked")
	public E get(int i) throws Exception
	{
		if (i >= mNumFilled)
		{
			throw(new Exception("Get null member of AutoArray!"));
		}
		return (E)mObjList[i];
	}
	
	public void add (E val) throws Exception
	{
		//synchronized(syncObj)
		{
			if (mNumFilled >= mObjList.length)
			{
				Object[] newList = new Object[1 + (int)Math.floor(1.5*mObjList.length)];
				for (int i = 0; i < mObjList.length; i++)
				{
					newList[i] = mObjList[i];
				}
				for (int i = mObjList.length; i < newList.length; i++)
				{
					newList[i] = null;
				}
				mObjList = null;
				mObjList = newList;
			}
			mObjList[mNumFilled] = (Object)val;
			mNumFilled++;
		}
	}

	public void sort() throws Exception
	{
		Arrays.sort(mObjList, 0,mNumFilled);
	}
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void sort(Comparator cmp)
	{
		Arrays.sort(mObjList,0,mNumFilled,cmp);	
	}
}
