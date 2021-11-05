package com.festp;

import java.util.EnumMap;

public interface Phase {
	public long getDuration();
	public double getSpeedMultiplier(long phaseTime);
	public EnumMap<PhaseFeature, Boolean> getFeatures();
}
