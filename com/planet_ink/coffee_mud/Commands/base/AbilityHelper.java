package com.planet_ink.coffee_mud.Commands.base;
import java.util.*;
import com.planet_ink.coffee_mud.utils.*;
import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;


public class AbilityHelper
{
	private AbilityHelper(){}
	protected final static int TRACK_ATTEMPTS=25;
	protected final static int TRACK_DEPTH=500;

	private static boolean levelCheck(String text, char prevChar, int lastPlace, int lvl)
	{
		int x=0;
		while(x>=0)
		{
			x=text.indexOf(">",lastPlace);
			if(x<0)	x=text.indexOf("<",lastPlace);
			if(x<0)	x=text.indexOf("=",lastPlace);
			if(x>=0)
			{
				char prev='+';
				if(x>0) prev=text.charAt(x-1);
				
				char primaryChar=text.charAt(x);
				x++;
				boolean andEqual=false;
				if(text.charAt(x)=='=')
				{
					andEqual=true;
					x++;
				}
				lastPlace=x;
					
				if(prev==prevChar)
				{
					boolean found=false;
					String cmpString="";
					while((x<text.length())&&
						  (((text.charAt(x)==' ')&&(cmpString.length()==0))
						   ||(Character.isDigit(text.charAt(x)))))
					{
						if(Character.isDigit(text.charAt(x)))
							cmpString+=text.charAt(x);
						x++;
					}
					if(cmpString.length()>0)
					{
						int cmpLevel=Util.s_int(cmpString);
						if((cmpLevel==lvl)&&(andEqual))
							found=true;
						else
						switch(primaryChar)
						{
						case '>': found=(lvl>cmpLevel); break;
						case '<': found=(lvl<cmpLevel); break;
						case '=': found=(lvl==cmpLevel); break;
						}
					}
					if(found) return true;
				}
			}
		}
		return false;
	}
	
	public static boolean zapperCheck(String text, MOB mob)
	{
		if(mob==null) return true;
		if(mob.charStats()==null) return true;
		if(text.trim().length()==0) return true;
		
		String mobClass=mob.charStats().getCurrentClass().name().toUpperCase().substring(0,3);
		String mobBaseClass=mob.charStats().getCurrentClass().baseClass().toUpperCase().substring(0,3);
		String mobRace=mob.charStats().getMyRace().racialCategory().toUpperCase().substring(0,3);
		String mobAlign=CommonStrings.shortAlignmentStr(mob.getAlignment()).toUpperCase().substring(0,3);
		String mobGender=mob.charStats().genderName().toUpperCase();
		int level=mob.envStats().level();
		int classLevel=mob.charStats().getClassLevel(mob.charStats().getCurrentClass());
		
		text=text.toUpperCase();
		
		if(mob.isASysOp(mob.location()))
		{
			if(text.toUpperCase().indexOf("+SYSOP")>=0)
			{
				return true;
			}
			if(text.toUpperCase().indexOf("-SYSOP")>=0)
			{
				return false;
			}
		}
		
		// do class first
		int x=text.indexOf("-CLAS");
		if(x>=0)
		{
			if(text.indexOf("+"+mobClass)<x)
			{
				return false;
			}
		}
		else
		{
			if(text.indexOf("-"+mobClass)>=0)
			{
				return false;
			}
		}

		// now base class
		x=text.indexOf("-BASECLAS");
		if(x>=0)
		{
			if(text.indexOf("+"+mobBaseClass)<x)
			{
				return false;
			}
		}
		else
		{
			if(text.indexOf("-"+mobBaseClass)>=0)
			{
				return false;
			}
		}

		// now race
		x=text.indexOf("-RACE");
		if(x>=0)
		{
			if(text.indexOf("+"+mobRace)<x)
			{
				return false;
			}
		}
		else
		{
			if(text.indexOf("-"+mobRace)>=0)
			{
				return false;
			}
		}

		// and now alignments
		x=text.indexOf("-ALIG");
		if(x>=0)
		{

			if(text.indexOf("+"+mobAlign)<x)
			{
				return false;
			}
		}
		else
		{
			if(text.indexOf("-"+mobAlign)>=0)
			{
				return false;
			}
		}
		
		x=text.indexOf("-GENDER");
		if(x>=0)
		{
			if(text.indexOf("+"+mobGender)<x)
			{
				return false;
			}
		}
		else
		{
			if(text.indexOf("-"+mobGender)>=0)
			{
				return false;
			}
		}
		
		x=text.indexOf("-LEVELS");
		if(x>=0)
		{
			if(!levelCheck(text,'+',x+6,level))
			{
				return false;
			}
		}
		else
		{
			if(levelCheck(text,'-',0,level))
			{
				return false;
			}
		}

		x=text.indexOf("-CLASSLEVEL");
		if(x>=0)
		{
			if(!levelCheck(text,'+',x+6,classLevel))
			{
				return false;
			}
		}
		return true;
	}

	public static boolean findTheRoom(Room location, 
									  Room destRoom, 
									  int tryCode, 
									  Vector dirVec,
									  Vector theTrail,
									  Hashtable lookedIn,
									  int depth,
									  boolean noWater)
	{
		if(lookedIn==null) return false;
		if(lookedIn.get(location)!=null) return false;
		if(depth>TRACK_DEPTH) return false;
		
		lookedIn.put(location,location);
		for(int x=0;x<dirVec.size();x++)
		{
			int i=((Integer)dirVec.elementAt(x)).intValue();
			Room nextRoom=location.getRoomInDir(i);
			Exit nextExit=location.getExitInDir(i);
			if((nextRoom!=null)
			&&(nextExit!=null)
			&&((!noWater)||(
			  (nextRoom.domainType()!=Room.DOMAIN_INDOORS_WATERSURFACE)
			&&(nextRoom.domainType()!=Room.DOMAIN_INDOORS_UNDERWATER)
			&&(nextRoom.domainType()!=Room.DOMAIN_OUTDOORS_UNDERWATER)
			&&(nextRoom.domainType()!=Room.DOMAIN_OUTDOORS_WATERSURFACE))))
			{
				if((nextRoom==destRoom)
				||(findTheRoom(nextRoom,destRoom,tryCode,dirVec,theTrail,lookedIn,depth+1,noWater)))
				{
					theTrail.addElement(nextRoom);
					return true;
				}
			}
		}
		return false;
	}
	
	public static Vector findBastardTheBestWay(Room location, 
											   Vector destRooms,
											   boolean noWater)
	{
		
		Vector trailArray[] = new Vector[TRACK_ATTEMPTS];
		Room trackArray[] = new Room[TRACK_ATTEMPTS];
		
		for(int t=0;t<TRACK_ATTEMPTS;t++)
		{
			Vector dirVec=new Vector();
			while(dirVec.size()<Directions.NUM_DIRECTIONS)
			{
				int direction=Dice.roll(1,Directions.NUM_DIRECTIONS,-1);
				for(int x=0;x<dirVec.size();x++)
					if(((Integer)dirVec.elementAt(x)).intValue()==direction)
						continue;
				dirVec.addElement(new Integer(direction));
			}
			Room roomToTry=(Room)destRooms.elementAt(Dice.roll(1,destRooms.size(),-1));
			Hashtable lookedIn=new Hashtable();
			Vector theTrail=new Vector();
			if(findTheRoom(location,roomToTry,2,dirVec,theTrail,lookedIn,0,noWater))
			{
				trailArray[t]=theTrail;
				trackArray[t]=roomToTry;
			}
		}
		int winner=-1;
		int winningTotal=Integer.MAX_VALUE;
		for(int t=0;t<TRACK_ATTEMPTS;t++)
		{
			Vector V=trailArray[t];
			Room which=trackArray[t];
			if((V!=null)&&(which!=null)&&(V.size()<winningTotal))
			{
				winningTotal=V.size();
				winner=t;
			}
		}
		
		if(winner<0) 
			return null;
		else
			return trailArray[winner];
	}
	
	public static int trackNextDirectionFromHere(Vector theTrail, 
												 Room location,
												 boolean noWater)
	{
		if((theTrail==null)||(location==null))
			return -1;
		if(location==theTrail.elementAt(0))
			return 999;

		Room nextRoom=null;
		int bestDirection=-1;
		int trailLength=Integer.MAX_VALUE;
		for(int dirs=0;dirs<Directions.NUM_DIRECTIONS;dirs++)
		{
			Room thisRoom=location.getRoomInDir(dirs);
			Exit thisExit=location.getExitInDir(dirs);
			if((thisRoom!=null)
			   &&(thisExit!=null)
			   &&((!noWater)||(
					  (thisRoom.domainType()!=Room.DOMAIN_INDOORS_WATERSURFACE)
					&&(thisRoom.domainType()!=Room.DOMAIN_INDOORS_UNDERWATER)
					&&(thisRoom.domainType()!=Room.DOMAIN_OUTDOORS_UNDERWATER)
					&&(thisRoom.domainType()!=Room.DOMAIN_OUTDOORS_WATERSURFACE))))
			{
				for(int trail=0;trail<theTrail.size();trail++)
				{
					if((theTrail.elementAt(trail)==thisRoom)
					&&(trail<trailLength))
					{
						bestDirection=dirs;
						trailLength=trail;
						nextRoom=thisRoom;
					}
				}
			}
		}
		return bestDirection;
	}
}
