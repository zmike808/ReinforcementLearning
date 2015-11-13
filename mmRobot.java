package ReinforcementLearning;

import java.awt.*; 
import java.awt.geom.*; 
import java.io.*;

import ReinforcementLearning.RobotAction;
import ReinforcementLearning.QLearning;
import ReinforcementLearning.LUQTable;
import ReinforcementLearning.RobotState;
import ReinforcementLearning.Enemy;

import robocode.*; 
 
import robocode.AdvancedRobot; 

public class mmRobot extends AdvancedRobot 
{
	private static int winningRound;
	private static int losingRound;
	public static final double PI = Math.PI;
	private Enemy enemy;
	private LUQTable qtable;
	private QLearning learner; 
    private double reward = 0.0; 
	private double firePower; 
	private int direction = 1; 
	private int isHitWall = 0; 
	private int isHitByBullet = 0; 
	 
	  public void run() 
	  { 
	    qtable = new LUQTable(); 
	    learner = new QLearning(qtable); 
	    enemy = new Enemy(); 
	    enemy.distance = 100000; 
	 
	    setColors(Color.green, Color.white, Color.green); 
	    setAdjustGunForRobotTurn(true); 
	    setAdjustRadarForGunTurn(true); 
	    turnRadarRightRadians(2 * PI); 
	    while (true) 
	    { 
	      robotMovement(); 
	      firePower = 400/enemy.distance; 
	      if (firePower > 3) 
	        firePower = 3; 
	      radarMovement(); 
	      gunMovement(); 
	      if (getGunHeat() == 0) { 
	        setFire(firePower); 
	      } 
	      execute(); 
	    } 
	  } 
	 
	  void doMovement() 
	  { 
	    if (getTime()%20 == 0) 
	    { 
	      direction *= -1;		//reverse direction 
	      setAhead(direction*300);	//move in that direction 
	    } 
	    setTurnRightRadians(enemy.bearing + (PI/2)); //every turn move to circle strafe the enemy 
	  } 
	 
	  private void robotMovement() 
	  { 
	    int state = getState(); 
	    int action = learner.selectAction(state, getTime()); 
	    out.println("RobotAction selected: " + action); 
	    learner.learn(state, action, reward); 
	    reward = 0.0; 
	    isHitWall = 0; 
	    isHitByBullet = 0; 
	 
	    switch (action) 
	    { 
	      case RobotAction.Ahead: 
	        setAhead(RobotAction.RobotMoveDistance); 
	        break; 
	      case RobotAction.Back: 
	        setBack(RobotAction.RobotMoveDistance); 
	        break; 
	      case RobotAction.TurnLeft: 
	        setTurnLeft(RobotAction.RobotTurnDegree); 
	        break; 
	      case RobotAction.TurnRight: 
	        setTurnRight(RobotAction.RobotTurnDegree); 
	        break; 
	    } 
	  } 
	 
	  private int getState() 
	  { 
	    int heading = RobotState.getHeading(getHeading()); 
	    int enemyDistance = RobotState.getEnemyDistance(enemy.distance); 
	    int enemyBearing = RobotState.getEnemyBearing(enemy.bearing); 
	    out.println("Stste(" + heading + ", " + enemyDistance + ", " + enemyBearing + ", " + isHitWall + ", " + isHitByBullet + ")"); 
	    int state = RobotState.mapping[heading][enemyDistance][enemyBearing][isHitWall][isHitByBullet]; 
	    return state; 
	  } 
	 
	  private void radarMovement() 
	  { 
	    double radarOffset; 
	    if (getTime() - enemy.ctime > 4) { //if we haven't seen anybody for a bit.... 
	      radarOffset = 4*PI;				//rotate the radar to find a enemy 
	    } else { 
	 
	      radarOffset = getRadarHeadingRadians() - (Math.PI/2 - Math.atan2(enemy.y - getY(),enemy.x - getX())); 
	      radarOffset = NormaliseBearing(radarOffset); 
	      if (radarOffset < 0) 
	        radarOffset -= PI/10; 
	      else 
	        radarOffset += PI/10; 
	    } 
	    setTurnRadarLeftRadians(radarOffset); 
	  } 
	 
	  private void gunMovement() 
	  { 
	    long time; 
	    long nextTime; 
	    Point2D.Double p; 
	    p = new Point2D.Double(enemy.x, enemy.y); 
	    for (int i = 0; i < 20; i++) 
	    { 
	      nextTime = (int)Math.round((getrange(getX(),getY(),p.x,p.y)/(20-(3*firePower)))); 
	      time = getTime() + nextTime - 10; 
	      p = enemy.guessPosition(time); 
	    } 
	    double gunOffset = getGunHeadingRadians() - (Math.PI/2 - Math.atan2(p.y - getY(),p.x -  getX())); 
	    setTurnGunLeftRadians(NormaliseBearing(gunOffset)); 
	  } 
	 
	  double NormaliseBearing(double ang) { 
	    if (ang > PI) 
	      ang -= 2*PI; 
	    if (ang< -PI) 
	      ang += 2*PI; 
	    return ang; 
	  } 
	 
	  double NormaliseHeading(double ang) { 
	    if (ang > 2*PI) 
	      ang -= 2*PI; 
	    if (ang < 0) 
	      ang += 2*PI; 
	    return ang; 
	  } 
	 
	  public double getrange( double x1,double y1, double x2,double y2 ) 
	  { 
	    double xo = x2-x1; 
	    double yo = y2-y1; 
	    double h = Math.sqrt( xo*xo + yo*yo ); 
	    return h; 
	  } 
	 
	  public double absbearing( double x1,double y1, double x2,double y2 ) 
	  { 
	    double xo = x2-x1; 
	    double yo = y2-y1; 
	    double h = getrange( x1,y1, x2,y2 ); 
	    if( xo > 0 && yo > 0 ) 
	    { 
	      return Math.asin( xo / h ); 
	    } 
	    if( xo > 0 && yo < 0 ) 
	    { 
	      return Math.PI - Math.asin( xo / h ); 
	    } 
	    if( xo < 0 && yo< 0 ) 
	    { 
	      return Math.PI + Math.asin( -xo / h ); 
	    } 
	    if( xo < 0 && yo > 0 ) 
	    { 
	      return 2.0*Math.PI - Math.asin( -xo / h ); 
	    } 
	    return 0; 
	  } 
	 
	  public void onBulletHit(BulletHitEvent e) 
	  { 
	    if (enemy.name == e.getName()) 
	    { 
	      double change = e.getBullet().getPower() * 9; 
	      out.println("Bullet Hit: " + change); 
	      reward += change; 
	    } 
	  } 
	 
	  public void onBulletMissed(BulletMissedEvent e) 
	  { 
	    double change = -e.getBullet().getPower(); 
	    out.println("Bullet Missed: " + change); 
	    reward += change; 
	  } 
	 
	  public void onHitByBullet(HitByBulletEvent e) 
	  { 
	    if (enemy.name == e.getName()) 
	    { 
	      double power = e.getBullet().getPower(); 
	      double change = -(4 * power + 2 * (power - 1)); 
	      out.println("Hit By Bullet: " + change); 
	      reward += change; 
	    } 
	    isHitByBullet = 1; 
	  } 
	 
	  public void onHitRobot(HitRobotEvent e) 
	  { 
	    if (enemy.name == e.getName()) 
	    { 
	      double change = -6.0; 
	      out.println("Hit Robot: " + change); 
	      reward += change; 
	    } 
	  } 
	 
	  public void onHitWall(HitWallEvent e) 
	  { 
	     
	    double change = -(Math.abs(getVelocity()) * 0.5 - 1); 
	    out.println("Hit Wall: " + change); 
	    reward += change; 
	    isHitWall = 1; 
	  } 
	 
	  public void onScannedRobot(ScannedRobotEvent e) 
	  { 
	    if ((e.getDistance() < enemy.distance)||(enemy.name == e.getName())) 
	    { 
	      //the next line gets the absolute bearing to the point where the bot is 
	      double absbearing_rad = (getHeadingRadians()+e.getBearingRadians())%(2*PI); 
	      //this section sets all the information about our enemy 
	      enemy.name = e.getName(); 
	      double h = NormaliseBearing(e.getHeadingRadians() - enemy.head); 
	      h = h/(getTime() - enemy.ctime); 
	      enemy.changehead = h; 
	      enemy.x = getX()+Math.sin(absbearing_rad)*e.getDistance(); //works out the x coordinate of where the enemy is 
	      enemy.y = getY()+Math.cos(absbearing_rad)*e.getDistance(); //works out the y coordinate of where the enemy is 
	      enemy.bearing = e.getBearingRadians(); 
	      enemy.head = e.getHeadingRadians(); 
	      enemy.ctime = getTime();				//game time at which this scan was produced 
	      enemy.speed = e.getVelocity(); 
	      enemy.distance = e.getDistance(); 
	      enemy.energy = e.getEnergy(); 
	    } 
	  } 
	 
	  public void onRobotDeath(RobotDeathEvent e) 
	  { 
	 
	    if (e.getName() == enemy.name) 
	      enemy.distance = 10000; 
	  }   

	  public void onWin(WinEvent event) 
	  { 
		 winningRound++; 
		 
		 PrintStream w = null; 
		    try 
		    { 
		      w = new PrintStream(new RobocodeFileOutputStream("/home/lili/workspace/EECE592/RL/src/RL/survival.xlsx", true)); 
		      w.println(winningRound + " " + losingRound + " " + qtable.totalValue()); 
		      if (w.checkError()) 
		        System.out.println("Could not save the data!"); 
		      w.close(); 
		    } 
		    catch (IOException e) 
		    { 
		      System.out.println("IOException trying to write: " + e); 
		    } 
		    finally 
		    { 
		      try 
		      { 
		        if (w != null) 
		          w.close(); 
		      } 
		      catch (Exception e) 
		      { 
		        System.out.println("Exception trying to close witer: " + e); 
		      } 
		    } 
		  }  

	  public void onDeath(DeathEvent event) 
	  { 
	     losingRound++; 
	     PrintStream w = null; 
		    try 
		    { 
		      w = new PrintStream(new RobocodeFileOutputStream("/home/lili/workspace/EECE592/RL/src/RL/survival.xlsx", true)); 
		      w.println(winningRound + " " + losingRound + " " + qtable.totalValue()); 
		      if (w.checkError()) 
		        System.out.println("Could not save the data!"); 
		      w.close(); 
		    } 
		    catch (IOException e) 
		    { 
		      System.out.println("IOException trying to write: " + e); 
		    } 
		    finally 
		    { 
		      try 
		      { 
		        if (w != null) 
		          w.close(); 
		      } 
		      catch (Exception e) 
		      { 
		        System.out.println("Exception trying to close witer: " + e); 
		      } 
		    } 
	  } 
}
