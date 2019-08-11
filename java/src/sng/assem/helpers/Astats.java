package sng.assem.helpers;

import java.util.TreeMap;
// Thread safe global stats tracker
// Intended for gathering stats from disparate functions and threads
/// Not really used, however
public class Astats 
{
	static TreeMap<String,Integer> mStats = null;
	
	public Astats()
	{
	}
	public static void init()
	{
		if (mStats == null)
		{
			mStats = new TreeMap<String,Integer>();	
		}
	}
	public static void set(String key, int val)
	{
		synchronized(mStats)
		{
			mStats.put(key,val);
		}
	}	
	public static int get(String key)
	{
		synchronized(mStats)
		{
			return mStats.get(key);	
		}
	}
	public static void inc(String key,int amt)
	{
		synchronized(mStats)
		{
			if (!mStats.containsKey(key))
			{
				set(key,0);	
			}
			mStats.put(key,get(key) + amt);
		}
	}
}
