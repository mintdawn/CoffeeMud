package com.planet_ink.coffee_mud.Abilities.Thief;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import com.planet_ink.coffee_mud.utils.*;
import java.util.*;

public class Thief_Sap extends ThiefSkill
{
	public Thief_Sap()
	{
		super();
		myID=this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.')+1);
		name="Sap";
		displayText="(knocked out)";
		miscText="";

		triggerStrings.addElement("SAP");

		canBeUninvoked=true;
		isAutoinvoked=false;

		quality=Ability.MALICIOUS;

		recoverEnvStats();
	}

	public Environmental newInstance()
	{
		return new Thief_Sap();
	}

	public boolean okAffect(Affect affect)
	{
		if((affected==null)||(!(affected instanceof MOB)))
			return true;

		MOB mob=(MOB)affected;

		// when this spell is on a MOBs Affected list,
		// it should consistantly prevent the mob
		// from trying to do ANYTHING except sleep
		if((affect.amISource(mob))&&(!Util.bset(affect.sourceMajor(),Affect.ACT_GENERAL)))
		{
			if((Util.bset(affect.sourceMajor(),Affect.ACT_EYES))
			||(Util.bset(affect.sourceMajor(),Affect.ACT_HANDS))
			||(Util.bset(affect.sourceMajor(),Affect.ACT_MOUTH))
			||(Util.bset(affect.sourceMajor(),Affect.ACT_EARS))
			||(Util.bset(affect.sourceMajor(),Affect.ACT_MOVE)))
			{
				if(affect.sourceMessage()!=null)
					mob.tell(mob,null,"You are way too drowsy.");
				return false;
			}
		}
		return super.okAffect(affect);
	}

	public void affectEnvStats(Environmental affected, EnvStats affectableStats)
	{
		super.affectEnvStats(affected,affectableStats);
		// when this spell is on a MOBs Affected list,
		// it should consistantly put the mob into
		// a sleeping state, so that nothing they do
		// can get them out of it.
		affectableStats.setDisposition(affectableStats.disposition()|EnvStats.IS_SLEEPING);
	}

	public void unInvoke()
	{
		// undo the affects of this spell
		if((affected==null)||(!(affected instanceof MOB)))
			return;
		MOB mob=(MOB)affected;

		super.unInvoke();

		if(!mob.amDead())
		{
			if(mob.location()!=null)
				mob.location().show(mob,null,Affect.MSG_OK_ACTION,"<S-NAME> regain(s) consciousness.");
			else
				mob.tell("You regain consciousness.");
			ExternalPlay.standIfNecessary(mob);
		}
	}



	public boolean invoke(MOB mob, Vector commands, Environmental givenTarget, boolean auto)
	{
		MOB target=this.getTarget(mob,commands,givenTarget);
		if(target==null) return false;

		if(!auto)
		{
			if(mob.isInCombat())
			{
				mob.tell("Not while you are fighting!");
				return false;
			}
		
			if(Sense.canBeSeenBy(mob,target))
			{
				mob.tell(target.name()+" is watching you way too closely.");
				return false;
			}
		
			if(mob.envStats().weight()<(target.envStats().weight()-100))
			{
				mob.tell(target.name()+" is way to big to knock out!");
				return false;
			}
		}

		// the invoke method for spells receives as
		// parameters the invoker, and the REMAINING
		// command line parameters, divided into words,
		// and added as String objects to a vector.
		if(!super.invoke(mob,commands,givenTarget,auto))
			return false;

		int levelDiff=target.envStats().level()-adjustedLevel(mob);
		if(levelDiff>0) 
			levelDiff=levelDiff*3;
		else 
			levelDiff=0;
		// now see if it worked
		boolean hit=(auto)||(CoffeeUtensils.normalizeAndRollLess(mob.adjustedAttackBonus()+target.adjustedArmor()));
		boolean success=profficiencyCheck((-levelDiff)+(-((target.charStats().getStat(CharStats.STRENGTH)-mob.charStats().getStat(CharStats.STRENGTH)))),auto)&&(hit);
		if(success)
		{
			// it worked, so build a copy of this ability,
			// and add it to the affects list of the
			// affected MOB.  Then tell everyone else
			// what happened.
			invoker=mob;
			FullMsg msg=new FullMsg(mob,target,this,Affect.MSG_DELICATE_HANDS_ACT|Affect.ACT_SOUND|Affect.MSK_MALICIOUS_MOVE|(auto?Affect.ACT_GENERAL:0),auto?"<T-NAME> hit(s) the floor!":"<S-NAME> sneak(s) up behind <T-NAMESELF> and knock(s) <T-HIM-HER> out!");
			if(mob.location().okAffect(msg))
			{
				mob.location().send(mob,msg);
				success=maliciousAffect(mob,target,3,-1);
				if(mob.getVictim()==target) mob.setVictim(null);
			}
		}
		else
			return maliciousFizzle(mob,target,"<S-NAME> sneak(s) up and attempt(s) to knock <T-NAMESELF> out, but fail(s).");

		// return whether it worked
		return success;
	}
}
