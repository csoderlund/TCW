package sng.assem.helpers;

import java.util.Arrays;
import java.util.Comparator;


// Lightweight map of strings to objects, to replace e.g. TreeMap<String,Clone> mName2Clone
// No checking for multiple insert with same string key
// After inserts, must be sorted before use
// Stores the strings and objects in parallel arrays, which get sorted in parallel
// Threadsafe
// CAS304 added suppress
public class Str2Obj<E>
{
	static Integer syncObj = 0;
	Object[] mObjList;
	String[] mStrList;
	int mNumKeys = 0;
	boolean mSorted = false;
	
	public Str2Obj()
	{
	
	}
	public void setSize( int size)
	{
		clear();
		mObjList = new Object[size];
		mStrList = new String[size];
		for (int i = 0; i < size; i++)
		{
			mObjList[i] = null;
			mStrList[i] = null;
		}
	}
	public void clear()
	{
		if (mObjList != null) 
		{
			for (int i = 0; i < mObjList.length; i++)
			{
				mObjList[i] = null;
			}
		}
		if (mStrList != null) 
		{
			for (int i = 0; i < mStrList.length; i++)
			{
				mStrList[i] = null;
			}
		}		
		mObjList = null;
		mStrList = null;
		mNumKeys = 0;
	}
	public int numKeys()
	{
		synchronized(syncObj)
		{
			return mNumKeys;
		}
	}
	public boolean containsKey(String key) throws Exception
	{
		synchronized(syncObj)
		{
			if (!mSorted)
			{
				throw(new Exception("containsKey on unsorted Str2Obj! "));
			}
			@SuppressWarnings("rawtypes")
			int idx = Arrays.binarySearch((Comparable[])mStrList, (Comparable)key, new ObjCmp());
			return (idx >= 0);
		}
	}
	@SuppressWarnings("unchecked")
	public E get(String key) throws Exception
	{	
		synchronized(syncObj)
		{
			if (!mSorted)
			{
				throw(new Exception("get on unsorted Str2Obj! "));
			}
			@SuppressWarnings("rawtypes")
			int idx = Arrays.binarySearch((Comparable[])mStrList, (Comparable)key, new ObjCmp());
			if (idx < 0) return null;
			return (E)mObjList[idx];
		}
		//return null;
	}
	public void put (String key, E val) throws Exception
	{
		synchronized(syncObj)
		{
			mSorted = false;

			if (mNumKeys >= mObjList.length)
			{
				Object[] newList = new Object[(int)Math.floor(1.25*mObjList.length)];
				String[] newStrList = new String[(int)Math.floor(1.25*mStrList.length)];
				for (int i = 0; i < mObjList.length; i++)
				{
					newList[i] = mObjList[i];
					newStrList[i] = mStrList[i];
				}
				for (int i = mObjList.length; i < newList.length; i++)
				{
					newList[i] = null;
					newStrList[i] = null;
				}
				mObjList = null;
				mStrList = null;
				mObjList = newList;
				mStrList = newStrList;
			}
			mObjList[mNumKeys] = (Object)val;
			mStrList[mNumKeys] = key;
			mNumKeys++;
		}
	}
	public void sort() throws Exception
	{
		HeapDoubleSort(mStrList,mObjList, new ObjCmp());
		int i = 0;
		for (i = 0; i < mStrList.length ; i++) 
		{
			if (mStrList[i] == null) break;
		}
		if (i != mNumKeys)
		{
			throw(new Exception("key count off after sort:" + getClass().getName()));
		}
		mSorted = true;
	}
	public int size()
	{
		return mNumKeys;
	}
	public void HeapDoubleSort(String[] a, Object[] aa, ObjCmp cmp){
        int i,f,s;
        for(i=1;i<a.length;i++){
            String e = a[i];
            Object ee = aa[i];
            s = i;
            f = (s-1)/2;
            while(s > 0 && cmp.compare(a[f], e) < 0){
                a[s] = a[f];
                aa[s] = aa[f];
                s = f;
                f = (s-1)/2;
            }
            a[s] = e;
            aa[s] = ee;
        }
        for(i=a.length-1;i>0;i--){
            String value = a[i];
            Object vv = aa[i];
            a[i] = a[0];
            aa[i] = aa[0];
            f = 0;
            if(i == 1)
                s = -1;
            else
                s = 1;
            if(i > 2 && cmp.compare(a[1], a[2]) < 0)
                s = 2;
            while(s >= 0 && cmp.compare(value, a[s]) < 0){
                a[f] = a[s];
                aa[f] = aa[s];
                f = s;
                s = 2*f+1;
                if(s+1 <= i-1 && cmp.compare(a[s], a[s+1]) < 0)
                    s = s+1;
                if(s > i-1)
                    s = -1;
            }
            a[f] = value;
            aa[f] = vv;
        }
    }	
	@SuppressWarnings("rawtypes")
	public class ObjCmp implements Comparator<Comparable>
	{
		@SuppressWarnings("unchecked")
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
