package com.festp;

public class Metrics {
	final int CONSIDERED_TICKS = 20 * 10;
	final double RATIO = 1.0f / 1000000000 / CONSIDERED_TICKS;
	long[] metrics = new long[CONSIDERED_TICKS];
	
	int index = 0;
	public double averageSec = 0;
	
	public void addMeasurement(long nanoDelta)
	{
		averageSec -= RATIO * metrics[index];
		metrics[index] = nanoDelta;
		averageSec += RATIO * metrics[index];
		
		index++;
		if (index >= CONSIDERED_TICKS)
			index = 0;
	}
	
	public double getAverageTickSec()
	{
		return averageSec;
	}
	
	public double recalcAverage()
	{
		averageSec = 0;
		for (int i = 0; i < metrics.length; i++)
			averageSec += RATIO * metrics[i];
		return averageSec;
	}
}
