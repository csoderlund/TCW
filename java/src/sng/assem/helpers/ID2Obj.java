package sng.assem.helpers;

// Lightweight map of ID's to objects, to replace e.g. TreeMap<Integer,Clone> mID2Clone
// Must set the ID range beforehand, so it can know the minimum ID and count
// the index starting from there. 
// Threadsafe
public class ID2Obj<E>
{
	static Integer syncObj = 0;
	Object[] mObjList;
	int mMinKey = -1;
	int mNumKeys = 0;
	
	public ID2Obj()
	{
	}
	public void setMinMax(int min, int max)
	{
		int size = max-min+1;
		setParams(size,min);
	}
	public void setParams(int size, int minKey)
	{
		mObjList = new Object[size];
		mMinKey = minKey;
		for (int i = 0; i < size; i++)
		{
			mObjList[i] = null;
		}
	}
	public void clear()
	{
		mMinKey = -1;
		if (mObjList == null) return;
		for (int i = 0; i < mObjList.length; i++)
		{
			mObjList[i] = null;
		}
		mObjList = null;
		mNumKeys = 0;
	}
	public int numKeys()
	{
		synchronized(syncObj)
		{
			return mNumKeys;
		}
	}
	public boolean containsKey(int key) throws Exception
	{
		synchronized(syncObj)
		{
			if (mMinKey == -1)
			{
				throw(new Exception("ID2Obj not initialized:" + getClass().getName()));
			}
			key -= mMinKey;
			return (key >= 0 && key < mObjList.length && mObjList[key] != null);
		}
	}
	public E get(int key) throws Exception
	{	
		synchronized(syncObj)
		{
			if (mMinKey == -1)
			{
				throw(new Exception("ID2Obj not initialized:" + getClass().getName()));
			}
			if (containsKey(key))
			{
				return (E)mObjList[key - mMinKey];
			}
			throw(new Exception("ID2Obj: getting unset key:" + key + ":" + getClass().getName())); 
		}
		//return null;
	}
	public void checkGrow(int key) throws Exception
	{
		synchronized(syncObj)
		{
			key -= mMinKey;
			if (key >= mObjList.length)
			{
				if (key > 1.5*mObjList.length)
				{
					throw(new Exception("Trying to grow ID2Obj array too fast! " + getClass().getName()));
				}
				Object[] newList = new Object[(int)Math.floor(1.5*mObjList.length)];
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
		}
	}
	public void put (int key, E val) throws Exception
	{
		synchronized(syncObj)
		{
			if (mMinKey == -1)
			{
				throw(new Exception("ID2Obj not initialized:" + getClass().getName()));
			}
			if (key < mMinKey)
			{
				// we have to grow the array down
				int extra = Math.min(mMinKey, 2*(mMinKey - key));
				Object[] newList = new Object[mObjList.length + extra];
				for (int i = 0; i < mObjList.length; i++)
				{
					newList[i + extra] = mObjList[i];
				}
				for (int i = 0; i < extra; i++)
				{
					newList[i] = null;
				}
				mObjList = null;
				mObjList = newList;
				mMinKey -= extra;
			}
			key -= mMinKey;
			if (key < 0)
			{
				throw(new Exception("Key " + key + " below zero?" + getClass().getName()));
			}
			if (key >= mObjList.length)
			{
				if (key > 1 + 1.5*mObjList.length)
				{
					throw(new Exception("Trying to grow ID2Obj array too fast! " + getClass().getName()));
				}
				Object[] newList = new Object[(int)Math.floor(1 + 1.5*mObjList.length)]; // in case the list is very short
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
			mObjList[key] = (Object)val;
			mNumKeys++;
		}
	}
	public int minKey() throws Exception
	{
		synchronized(syncObj)
		{
			if (mMinKey == -1)
			{
				throw(new Exception("ID2Obj not initialized:" + getClass().getName()));
			}		
			return mMinKey;
		}
	}
	public int maxFilledKey()
	{
		int ret = 0;
		for (int i = mObjList.length - 1; i >= 0; i--)
		{
			if (mObjList[i] != null)
			{
				ret = i;
				break;
			}
		}
		return ret;
	}
	public int maxKey()
	{
		synchronized(syncObj)
		{
			return mObjList.length - 1 + mMinKey;
		}
	}
	public int numSpaces()
	{
		synchronized(syncObj)
		{
			return mObjList.length;
		}		
	}	
}
