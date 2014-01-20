package walker;

import action.Action;
import info.Area;
import info.Box;
import info.Card;
import info.Deck;
import info.FairyBattleInfo;
import info.Floor;
import info.PFBGood;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

public class Info {
	// login info
	public static String LoginId = "";
	public static String LoginPw = "";

	// sleep count
	public static int sleepCount = 0;

	// user info
	public String username = "";
	public long gold = 0;
	public int friendshipPoint;
	public int gachaTicket;
	public int gachaPoint;
	public int ap = 0;
	public int bc = 0;
	public int apMax = 0;
	public int bcMax = 0;
	public int exp = 0;
	public int gather = 0;
	public int lv = 0;
	public int cardMax = 0;
	public String guildId = "";
	public String userId = "";
	public int ticket = 0;
	public String week = "";
	public int pointToAdd = 0;
	public int apUp = 0;
	public int bcUp = 0;
	public boolean isCardFull = false;

	// 舔妖
	public int fairyHit = 0;

	// 尾妖
	public int fairyKill = 0;

	// 妖精礼物，距离上次收礼物时间大于1分钟才收
	public long lastReceiveRewardTime = 0;

	// 优先进行妖精战
	public static boolean FairyBattleFirst = true;

	// 允许BC不够时依旧跑图
	public static boolean AllowBCInsuffient = false;

	// 升级自动加点
	public enum PointType {
		BC(0), AP(1);

		int type;

		private PointType(int t) {
			this.type = t;
		}

		public static boolean isBC(PointType t) {
			return PointType.BC.equals(t);
		}
	}

	public static PointType AutoAddPointType;

	// 是否跑日常图
	public static boolean DailyMap = false;

	// 日常图编号
	public static Set<Integer> DailyMapNo = new HashSet<>();

	// 表图跑最低cost图
	public static boolean MinAPOnly = true;

	// 里图跑最低cost图
	public static boolean MinAPOnlyInner = true;

	// 优先跑里图
	public static boolean InnerMapFirst = false;

	// 只跑里图
	public static boolean InnerMapOnly = false;

	// 第一张里图编号
	public static int firstInnerMapNo = 0;

	// 自动每隔5分钟刷新地图列表
	public static boolean AutoRefreshMap = false;
	public double lastUpdateTime = 0;

	// 是否自动收箱子
	public static boolean AutoReceiveBox = false;

	// 最大卖卡金额
	public static int MaxSellCardMoney = 0;

	// 当自己妖精未打死时不继续探索
	public static boolean StopExploreWhenFairyAlive = true;
	public boolean ownFairyKilled = true;

	// 优先打剩余时间少的妖精
	public static boolean LessTimeFairyFirst = false;

	// 自动卖卡
	public static boolean AutoSellCard = false;

	// 登录时自动消耗伴点
	public static boolean DropFriendshipPoint = false;

	/*
	 * 吃药相关
	 */
	// 自动吃AP药
	public static boolean AutoUseAp = false;
	// 自动吃BC药
	public static boolean AutoUseBc = false;

	// 自动吃AP药类型
	public static AutoUseType AutoApType = AutoUseType.NONE;
	// 自动吃BC药类型
	public static AutoUseType AutoBcType = AutoUseType.NONE;
	// 低于x吃AP药
	public static int AutoApLow = -1;
	// 低于x吃BC药
	public static int AutoBcLow = -1;
	// 保留x全AP药
	public static int AutoApFullLow = Integer.MAX_VALUE;
	// 保留x全BC药
	public static int AutoBcFullLow = Integer.MAX_VALUE;

	// 自动吃药类型
	public enum AutoUseType {
		NONE, HALF_ONLY, FULL_ONLY, ALL
	}

	public int fullBc = 0;
	public int fullAp = 0;
	public int halfBc = 0;
	public int halfAp = 0;
	public int halfBcToday = 0;
	public int halfApToday = 0;
	public String toUse = "";

	// 调试开关
	public static boolean Debug = false;

	// card list
	public ArrayList<Card> cardList;
	public static HashSet<Integer> CanBeSold = new HashSet<Integer>();
	public String toSell = "";

	// decks
	public static Deck fairyDeck = new Deck();
	public static Deck guildFairyDeck = new Deck();
	public static Deck rareFairyDeck = new Deck();
	public static Deck koDeck = new Deck();
	public static long killFairyHp = Long.MIN_VALUE;

	// area
	public TreeMap<Integer, Area> area = new TreeMap<>();
	public TreeMap<Integer, Area> areaInner = new TreeMap<>();

	// floor
	public TreeMap<Integer, Floor> floor = new TreeMap<>();
	public Floor front;
	public boolean AllClear = false;

	// fairy
	public FairyBattleInfo fairy;
	public boolean NoFairy = false;
	public Queue<FairyBattleInfo> LatestFairyList = new LinkedList<FairyBattleInfo>();
	// 由于系统是倒序排列的（最新的在第一个），因此需采用FILO的堆栈
	public Stack<PFBGood> PFBGoodList;

	// explore
	public String ExploreResult = "";
	public String ExploreProgress = "";
	public String ExploreGold = "";
	public String ExploreExp = "";

	// box list
	public ArrayList<Box> boxList;
	public String toGet = "";

	// 跑固定图
	public static boolean OneMapOnly = false;
	public static int OneMapId = 0;
	public static int OneMapCost = 0;

	// timeout
	public static enum TimeoutEntry {
		login;
	}

	private Hashtable<TimeoutEntry, Long> timeout;

	public long GetTimeout(TimeoutEntry te) {
		return System.currentTimeMillis() - timeout.get(te);
	}

	public void SetTimeoutByEntry(TimeoutEntry te) {
		timeout.put(te, System.currentTimeMillis());
	}

	public void SetTimeoutByAction(Action act) {
		switch (act) {
		case LOGIN:
			this.SetTimeoutByEntry(TimeoutEntry.login);
		default:
			break;
		}
	}

	public ArrayList<TimeoutEntry> CheckTimeout() {
		ArrayList<TimeoutEntry> te = new ArrayList<TimeoutEntry>();
		// restart at 1:00~1:59AM if user already alive for more than a hour
		if (Calendar.getInstance().get(Calendar.HOUR_OF_DAY) == 1) {
			if (GetTimeout(TimeoutEntry.login) > 3600000) {
				te.add(TimeoutEntry.login);
			}
		}
		return te;
	}

	// event
	public static enum EventType {
		notLoggedIn, cookieOutOfDate, needFloorInfo, innerMapJump, areaComplete, fairyAppear, fairyTransform, fairyReward, fairyCanBattle, fairyBattleWin, fairyBattleLose, fairyBattleEnd, cardFull, privateFairyAppear, guildTopRetry, guildBattle, guildTop, ticketFull, needAPBCInfo, levelUp, PFBGood, recvPFBGood, gotoFloor
	}

	public Stack<EventType> events;

	public Info() {
		cardList = new ArrayList<Card>();
		front = new Floor();
		PFBGoodList = new Stack<PFBGood>();
		events = new Stack<EventType>();
		events.push(EventType.notLoggedIn);

		timeout = new Hashtable<TimeoutEntry, Long>();
		timeout.put(TimeoutEntry.login, (long) 0);

		fairy = new FairyBattleInfo();
	}
}
