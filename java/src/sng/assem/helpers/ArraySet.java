package sng.assem.helpers;

import java.util.Arrays;
import java.util.Comparator;

// E has to implement Comparable for this to work
// This class is for sets which are created and then not changed
public class ArraySet<E> 
{

	static Integer syncObj = 0;
	Comparable[] mObjList;
	int mNumKeys = 0;
	boolean mSorted = false;
	
	ArraySet(int size)
	{
		mObjList = new Comparable[size];
		for (int i = 0; i < size; i++)
		{
			mObjList[i] = null;
		}
	}
	void clear()
	{
		if (mObjList == null) return;
		for (int i = 0; i < mObjList.length; i++)
		{
			mObjList[i] = null;
		}
		mObjList = null;
		mNumKeys = 0;
	}
	int numKeys()
	{
		synchronized(syncObj)
		{
			return mNumKeys;
		}
	}
	boolean contains(E obj) throws Exception
	{
		boolean ret = false;
		synchronized(syncObj)
		{
			if (find(obj) >= 0)
			{
				ret = true;	
			}
		}
		return ret;
	}

	void add (E val) throws Exception
	{
		synchronized(syncObj)
		{
			if (mSorted)
			{
				if (contains(val)) return;
			}
			mSorted = false;
			if (mNumKeys >= mObjList.length)
			{
				Comparable[] newList = new Comparable[(int)Math.floor(1.25*mObjList.length)];
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
			mObjList[mNumKeys] = (Comparable)val;
			mNumKeys++;
		}
	}
	int find (E obj) throws Exception
	{
		if (!mSorted)
		{
			throw(new Exception("Find on unsorted ArraySet!"));
		}
		int ret = Arrays.binarySearch(mObjList, (Comparable)obj, new ObjCmp());
		return ret;
	}

	void sort() throws Exception
	{
		Arrays.sort(mObjList, new ObjCmp());
		int i = 0;
		for (i = 0; i < mObjList.length ; i++) 
		{
			if (mObjList[i] == null) break;
		}
		if (i != mNumKeys)
		{
			throw(new Exception("key count off after sort:" + getClass().getName()));
		}
		mSorted = true;
	}
	int size()
	{
		return mNumKeys;
	}
	
	class ObjCmp implements Comparator<Comparable>
	{
		public int compare(Comparable a, Comparable b)
		{
			if (a == null)
			{
				if (b == null) return 0;
				else return +1;
			}
			else if (b == null)
			{
				return -1;
			}
			else return a.compareTo(b);
		}
	}
}
