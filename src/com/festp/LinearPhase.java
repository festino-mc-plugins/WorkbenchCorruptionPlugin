package com.festp;

import java.util.EnumMap;

public class LinearPhase implements Phase {
	EnumMap<PhaseFeature, Boolean> features;
	public long duration;
	public double startSpeedMultiplier;
	public double endSpeedMultiplier;
	
	public LinearPhase(PhaseFeature[] features, long duration, double startMultiplier, double endMultiplier)
	{
		this.duration = duration;
		this.startSpeedMultiplier = startMultiplier;
		this.endSpeedMultiplier = endMultiplier;
		this.features = new EnumMap<>(PhaseFeature.class);
		for (PhaseFeature pf : features)
			this.features.put(pf, true);
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

	@Override
	public EnumMap<PhaseFeature, Boolean> getFeatures() {
		return features;
	}
}
