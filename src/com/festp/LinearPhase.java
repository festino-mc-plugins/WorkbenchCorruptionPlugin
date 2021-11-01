package com.festp;

public class LinearPhase implements Phase {
	public long duration;
	public double startSpeedMultiplier;
	public double endSpeedMultiplier;
	
	public LinearPhase(long duration, double startMultiplier, double endMultiplier)
	{
		this.duration = duration;
		this.startSpeedMultiplier = startMultiplier;
		this.endSpeedMultiplier = endMultiplier;
	}
	
	public long getDuration()
	{
		return duration;
	}
	
	public double getSpeedMultiplier(long phaseTime)
	{
		double k = phaseTime / duration;
		return startSpeedMultiplier * (1 - k) + endSpeedMultiplier * k;
	}
}
