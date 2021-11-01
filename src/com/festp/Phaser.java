package com.festp;

public class Phaser {
	private boolean enabled = false;
	double baseTickspeed = 3;
	
	int phase = 0;
	Phase[] phases = new Phase[1];

	long phaseTicks = 0;
	double tickSupply = 0;
	
	public final RandomTicker randomTicker = new RandomTicker();
	
	public Phaser(Phase[] phases)
	{
		this.phases = phases;
	}
	
	public void setPhases(Phase[] phases)
	{
		this.phases = phases;
		setPhase(0, 0);
	}
	
	public boolean setPhase(int index, double timePercent)
	{
		if (index < 0 || phases.length <= index)
			return false;
		phase = index;
		phaseTicks = (int)Math.floor(phases[phase].getDuration() * timePercent);
		return true;
	}
	
	public void tick()
	{
		double actual = getActualTickspeed();
		tickSupply += actual;
		int ticks = (int)Math.floor(tickSupply);
		tickSupply -= ticks;
		randomTicker.setRandomSectionTicks(ticks);
		if (ticks > 0)
		{
			randomTicker.tick();
		}
		
		phaseTicks++;
		if (phaseTicks > phases[phase].getDuration())
		{
			phaseTicks = 0;
			if (phase < phases.length - 1)
				phase++;
		}
		
		// TODO save state
	}
	
	public void setEnabled(boolean isEnabled)
	{
		enabled = isEnabled;
	}
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public double getBaseTickspeed()
	{
		return baseTickspeed;
	}
	
	public double getActualTickspeed()
	{
		if (!enabled)
			return 0;
		return baseTickspeed * phases[phase].getSpeedMultiplier(phaseTicks);
	}
	
	public void setBaseTickspeed(double tickspeed)
	{
		baseTickspeed = tickspeed;
	}
}
