package walker;

import info.BattleDB;

import java.util.List;
import walker.Info.PointType;
import action.Action;
import action.SellCard;

public class Think {
	// 吃药编码
	private static final String AP_HALF = "101";
	private static final String BC_HALF = "111";
	private static final String AP_FULL = "1";
	private static final String BC_FULL = "2";

	private static final int EXPLORE_NORMAL = 60;
	private static final int EXPLORE_URGENT = 80;
	private static final int GFL_PRI = 50;
	private static final int GFL_PRI_URGENT = 70;
	private static final int GF_PRI = 25;
	private static final int USE_PRI = 99;

	private static boolean flag = false;

	public static Action doIt(List<Action> possible) {
		Action best = Action.NOTHING;

		int score = Integer.MIN_VALUE + 208;

		for (Action action : possible) {
			switch (action) {
			case LOGIN:
				return Action.LOGIN;
			case GET_FLOOR_INFO:
				return Action.GET_FLOOR_INFO;
			case GET_FAIRY_LIST:
				// 本轮已判断过此类行为，直接跳过
				if (flag) {
					flag = false;
					break;
				}

				// BC不足不获取妖精列表
				if (Process.info.bc < Info.fairyDeck.battleCost
						|| Process.info.bc < 2) {
					flag = true;
					break;
				}

				// 本次优先探索，则下次优先妖精
				if (Info.FairyBattleFirst == true
						&& BattleDB.wantBattleFlag == false) {
					BattleDB.wantBattleFlag = true;
					flag = true;
					break;
				}

				if (Info.FairyBattleFirst == true && score < GFL_PRI_URGENT) {
					best = Action.GET_FAIRY_LIST;
					score = GFL_PRI_URGENT;
				} else if (score < GFL_PRI) {
					best = Action.GET_FAIRY_LIST;
					score = GFL_PRI;
				}
				break;
			case GOTO_FLOOR:
				if (score < GF_PRI) {
					best = Action.GOTO_FLOOR;
					score = GF_PRI;
				}
				break;
			case PRIVATE_FAIRY_BATTLE:
				if (canBattle() == true) {
					return Action.PRIVATE_FAIRY_BATTLE;
				}
				break;
			case EXPLORE:
				int p = explorePoint();
				if (p > score) {
					best = Action.EXPLORE;
					score = p;
				}
				break;
			case GUILD_BATTLE:
				Process.info.fairy.deckNo = Info.guildFairyDeck.no;
				return Action.GUILD_BATTLE;
			case GUILD_TOP:
				return Action.GUILD_TOP;
			case GET_FAIRY_REWARD:
				return Action.GET_FAIRY_REWARD;
			case PFB_GOOD:
				return Action.PFB_GOOD;
			case RECV_PFB_GOOD:
				return Action.RECV_PFB_GOOD;
			case NOTHING:
				break;
			case SELL_CARD:
				if (Info.AutoSellCard == true && SellCard.cardsToSell() == true) {
					return Action.SELL_CARD;
				}
				break;
			case LV_UP:
				if (decideUpPoint() > 0) {
					return Action.LV_UP;
				}
				break;
			case USE:
				int ptr = decideUse();
				if (ptr > score) {
					best = Action.USE;
					score = ptr;
				}
				break;
			default:
				break;
			}
		}

		return best;
	}

	/*
	 * 吃药判定
	 */
	private static int decideUse() {
		if (Info.AutoUseAp == true) {
			if (Process.info.ap < Info.AutoApLow) {
				switch (Info.AutoApType) {
				case ALL:
					if (Process.info.halfApToday > 0 && Process.info.halfAp > 0) {
						Process.info.toUse = AP_HALF;
						return USE_PRI;
					} else if (Process.info.fullAp > Info.AutoApFullLow) {
						Process.info.toUse = AP_FULL;
						return USE_PRI;
					}

					break;
				case FULL_ONLY:
					if (Process.info.fullAp > Info.AutoApFullLow) {
						Process.info.toUse = AP_FULL;
						return USE_PRI;
					}

					break;
				case HALF_ONLY:
					if (Process.info.halfApToday > 0 && Process.info.halfAp > 0) {
						Process.info.toUse = AP_HALF;
						return USE_PRI;
					}

					break;
				default:
					break;
				}
			}
		}

		if (Info.AutoUseBc == true) {
			if (Process.info.bc < Info.AutoBcLow) {
				switch (Info.AutoBcType) {
				case ALL:
					if (Process.info.halfBcToday > 0 && Process.info.halfBc > 0) {
						Process.info.toUse = BC_HALF;
						return USE_PRI;
					} else if (Process.info.fullBc > Info.AutoBcFullLow) {
						Process.info.toUse = BC_FULL;
						return USE_PRI;
					}

					break;
				case FULL_ONLY:
					if (Process.info.fullBc > Info.AutoBcFullLow) {
						Process.info.toUse = BC_FULL;
						return USE_PRI;
					}

					break;
				case HALF_ONLY:
					if (Process.info.halfBcToday > 0 && Process.info.halfBc > 0) {
						Process.info.toUse = BC_HALF;
						return USE_PRI;
					}

					break;
				default:
					break;
				}
			}
		}

		return Integer.MIN_VALUE;
	}

	private static boolean canBattle() {
		// 其他地方已配置过卡组，此处不再配置
		switch (Process.info.fairy.fairyType) {
		case 0:// 外敌
			return true;
		case 4:// 妖精（他人）
		case 5:// 妖精（他人、觉醒）
		case 6:// 妖精（自己）
		case 7:// 妖精（自己，觉醒）
			if (Process.info.bc < Info.fairyDeck.battleCost
					|| Process.info.bc < 2) {
				return false;
			}

			break;
		default:
			return false;
		}

		return true;
	}

	private static int decideUpPoint() {
		if (PointType.isBC(Info.AutoAddPointType)) {
			Process.info.apUp = 0;
			Process.info.bcUp = Process.info.pointToAdd;

			return Process.info.bcUp;
		} else {
			Process.info.apUp = Process.info.pointToAdd;
			Process.info.bcUp = 0;

			return Process.info.apUp;
		}
	}

	private static int explorePoint() {
		// AP不足不跑图
		if (Process.info.ap < Process.info.front.cost) {
			return Integer.MIN_VALUE;
		}

		// 满AP优先探索
		if (Process.info.ap == Process.info.apMax) {
			return EXPLORE_URGENT;
		}

		// 判断是否可以行动
		if (Info.AllowBCInsuffient == false
				&& Process.info.bc < Info.fairyDeck.battleCost) {
			return Integer.MIN_VALUE;
		}

		return EXPLORE_NORMAL;
	}

}