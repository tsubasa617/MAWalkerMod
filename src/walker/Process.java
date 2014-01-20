package walker;

import action.Action;
import action.DropFriendshipPoint;
import action.Explore;
import action.GetFairyList;
import action.GetFairyReward;
import action.GetFloorInfo;
import action.GetRank;
import action.GetRewardBox;
import action.GotoFloor;
import action.PFBGood;
import action.GuildBattle;
import action.GuildTop;
import action.Login;
import action.LvUp;
import action.GetPartyRank;
import action.PrivateFairyBattle;
import action.RecvPFBGood;
import action.SellCard;
import action.Use;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.Network;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import walker.Go.LogType;
import walker.Info.EventType;
import walker.Info.TimeoutEntry;

public class Process {
	private static final double FIVE_MINUTES = 5 * 60 * 1000;

	public static Info info;
	public static Network network;

	public Process() {
		info = new Info();
		network = new Network();
	}

	public void auto() throws Exception {
		try {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none) {
				rescue();
			} else {
				long start = System.currentTimeMillis();

				execute(Think.doIt(getPossibleAction()));

				long delta = System.currentTimeMillis() - start;
				if (delta < 4000
						&& info.events.contains(EventType.fairyCanBattle) == false) {
					Thread.sleep(4000 - delta);

					if (Info.Debug == true) {
						Go.log("休息" + (4000 - delta) + "毫秒", LogType.DEBUG);
					}
				}
			}
		} catch (Exception ex) {
			throw ex;
		}

	}

	private void rescue() {
		ErrorData.clear();
	}

	private List<Action> getPossibleAction() {
		List<Action> result = new ArrayList<Action>();

		// 检测是否需刷新地图
		if (Info.AutoRefreshMap == true
				&& System.currentTimeMillis() - info.lastUpdateTime > FIVE_MINUTES) {
			info.lastUpdateTime = System.currentTimeMillis();
			info.events.push(EventType.needFloorInfo);
		}

		if (info.events.size() != 0) {
			switch (info.events.pop()) {
			case notLoggedIn:
			case cookieOutOfDate:
				result.add(Action.LOGIN);
				break;
			case fairyTransform:
				Go.log("強敵覺醒！稀有妖精出現", LogType.INFO);
				result.add(Action.GET_FAIRY_LIST);
				break;
			case privateFairyAppear:
				result.add(Action.GET_FAIRY_LIST);
				break;
			case fairyCanBattle:
				result.add(Action.PRIVATE_FAIRY_BATTLE);
				break;
			case fairyReward:
				if (info.ticket > 0) {
					result.add(Action.GUILD_TOP);
				} else if (info.ticket < 0) {
					// do nothing
				} else {
					result.add(Action.GET_FAIRY_REWARD);
				}
				break;
			case innerMapJump:
				Go.log("裏圖出現/消失", LogType.INFO);
			case needFloorInfo:
			case areaComplete:
				result.add(Action.GET_FLOOR_INFO);
				break;
			case cardFull:
				result.add(Action.SELL_CARD);
				break;
			case fairyAppear:
				result.add(Action.GET_FAIRY_LIST);
				break;
			case guildBattle:
				if (info.bc == info.bcMax && info.ticket < 10) {
					break;
				}

				result.add(Action.GUILD_BATTLE);
				break;
			case guildTopRetry:
			case guildTop:
			case ticketFull:
				result.add(Action.GUILD_TOP);
				break;
			case needAPBCInfo:
				result.add(Action.GOTO_FLOOR);
				break;
			case levelUp:
				result.add(Action.LV_UP);
				break;
			case fairyBattleEnd:
			case fairyBattleLose:
			case fairyBattleWin:
				break;
			case PFBGood:
				result.add(Action.PFB_GOOD);
				break;
			case recvPFBGood:
				result.add(Action.RECV_PFB_GOOD);
				break;
			case gotoFloor:
				result.add(Action.GOTO_FLOOR);
				break;
			}
		} // end if

		ArrayList<TimeoutEntry> te = info.CheckTimeout();
		for (Info.TimeoutEntry e : te) {
			switch (e) {
			case login:
				info.events.push(Info.EventType.cookieOutOfDate);

				break;
			default:
				break;
			}
		} // end for

		// 优先打妖
		if (Info.FairyBattleFirst
				|| (Info.StopExploreWhenFairyAlive == true && Process.info.ownFairyKilled == false)) {
			result.add(Action.GET_FAIRY_LIST);
		}

		// 探索：1.卡没满；2.设置自己妖没死不探索时自己妖已死; 3.跑图列表不为空
		if (Process.info.isCardFull == false
				&& (Info.StopExploreWhenFairyAlive == false || Process.info.ownFairyKilled == true)
				&& Process.info.front != null) {
			result.add(Action.EXPLORE);
		}

		// 自动吃药
		result.add(Action.USE);

		return result;
	}

	private void execute(Action action) throws Exception {
		if (Info.Debug == true) {
			Go.log(action.name(), LogType.DEBUG);
		}

		switch (action) {
		case LOGIN:
			try {
				if (Login.run()) {
					Go.log(String.format(
							"%s, 等級%d AP:%d/%d BC:%d/%d 卡:%d/%d 外援書:%d",
							info.username, info.lv, info.ap, info.apMax,
							info.bc, info.bcMax, info.cardList.size(),
							info.cardMax, info.ticket), LogType.INFO);
					Go.log(String.format("金幣:%d 絆點:%d 扭蛋券:%d 扭蛋點:%d",
							info.gold, info.friendshipPoint, info.gachaTicket,
							info.gachaPoint), LogType.INFO);

					info.events.push(Info.EventType.needFloorInfo);

					// 自动收箱子
					if (Info.AutoReceiveBox == true) {
						GetRewardBox.listRewardbox();
					}

					// 自动消耗伴点
					if (Info.DropFriendshipPoint == true) {
						// 十连
						if (info.friendshipPoint > 2000) {
							while (true) {
								int fsPoint = DropFriendshipPoint.run();

								if (fsPoint > 0) {
									Go.log("剩餘伴點：" + fsPoint, LogType.INFO);
								} else {
									break;
								}
							}
						}
					}

					// 个人排名
					int gatherRank = GetRank.gatherrank(
							Integer.parseInt(info.userId), 5);
					int guildRank = GetRank.gatherrank(
							Integer.parseInt(info.userId), 4);
					Go.log(String.format("收集排名：%d, 騎士團個人排名：%d", gatherRank,
							guildRank), LogType.INFO);

					// 团贡排名
					GetPartyRank.run();
				} else {
					info.events.push(Info.EventType.notLoggedIn);
				}
			} catch (Exception ex) {
				info.events.push(Info.EventType.notLoggedIn);

				if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
					throw ex;
				}
			}
			break;
		case GET_FLOOR_INFO:
			try {
				if (GetFloorInfo.run()) {
					if (info.area.get(Integer.parseInt(info.front.areaId)) != null) {
						// 表图
						Go.log(String.format("表圖:%s %s層 %d消耗",
								info.area.get(Integer
										.parseInt(info.front.areaId)).areaName,
								info.front.floorId, info.front.cost),
								LogType.INFO);
					} else {
						// 里图
						Go.log(String.format("裏圖:%s %s層 %d消耗",
								info.areaInner.get(Integer
										.parseInt(info.front.areaId)).areaName,
								info.front.floorId, info.front.cost),
								LogType.INFO);
					}
				}
			} catch (Exception ex) {
				if (Info.Debug == true) {
					Go.log("GET_FLOOR_INFO exception: " + ex.getMessage(),
							LogType.DEBUG);
				}

				if ((ex.getMessage() != null)
						&& (ex.getMessage().equals("302"))) {
					info.events.push(Info.EventType.innerMapJump);
					ErrorData.clear();
				} else if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
					throw ex;
				}
			}
			break;
		case GET_FAIRY_LIST:
			try {
				if (GetFairyList.run()) {
					if ((!info.events.empty())
							&& (info.events.peek() == Info.EventType.fairyCanBattle)) {
						// 内部已经打了不少信息，此处就没必要了
						// Go.log("發現其他人的強敵！");
					} else {
						// Go.log("沒有妖精了");
					}
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.ConnectionError) {
					info.events.push(Info.EventType.fairyAppear);
					Go.log("獲取結果超時，重試獲取妖精列表", LogType.WARN);
					ErrorData.clear();
				} else if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
					throw ex;
				}
			}
			break;
		case GOTO_FLOOR:
			try {
				if (GotoFloor.run()) {
					if (info.area.get(Integer.parseInt(info.front.areaId)) != null) {
						// 表图
						Go.log(String.format("前往 [%s] 第%s層 AP:%d/%d BC:%d/%d",
								info.area.get(Integer
										.parseInt(info.front.areaId)).areaName,
								info.front.floorId, info.ap, info.apMax,
								info.bc, info.bcMax), LogType.INFO);
					} else {
						// 里图
						Go.log(String.format("前往 [%s] 第%s層 AP:%d/%d BC:%d/%d",
								info.areaInner.get(Integer
										.parseInt(info.front.areaId)).areaName,
								info.front.floorId, info.ap, info.apMax,
								info.bc, info.bcMax), LogType.INFO);
					}
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none) {
					throw ex;
				}
			}
			break;
		case PRIVATE_FAIRY_BATTLE:
			try {
				if (PrivateFairyBattle.run()) {
					String result = "";

					if (!info.events.empty()) {
						switch (info.events.peek()) {
						case fairyBattleEnd:
							result = "來晚啦";
							info.events.pop();
							break;
						case fairyBattleLose:
							result = "慘敗";
							info.events.pop();
							info.fairyHit++;
							break;
						case fairyBattleWin:
							result = "勝利";
							info.events.pop();
							info.fairyKill++;
							break;
						case fairyTransform:
							result = "覺醒";
							info.fairyKill++;
							break;
						default:
							result = "未知";
							break;
						}
					}

					String str = String.format(
							"妖精戰 [%s] AP:%d/%d BC:%d/%d 外援書:%d", result,
							info.ap, info.apMax, info.bc, info.bcMax,
							info.ticket);

					if (info.gather != -1) {
						str += String.format(" 收集:%d", info.gather);
					}

					str += String.format(" 舔:%d 尾:%d", info.fairyHit,
							info.fairyKill);

					Go.log(str, LogType.INFO);
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
					throw ex;
			}
			break;
		case EXPLORE:
			try {
				if (Explore.run()) {
					if (info.area.get(Integer.parseInt(info.front.areaId)) != null) {
						// 表图
						Go.log(String.format(
								"探索 [%s 第%s層] AP:%d 金幣+%s 經驗+%s 進度:%s 結果:%s",
								info.area.get(Integer
										.parseInt(info.front.areaId)).areaName,
								info.front.floorId, info.ap, info.ExploreGold,
								info.ExploreExp, info.ExploreProgress,
								info.ExploreResult), LogType.INFO);
					} else {
						// 里图
						Go.log(String.format(
								"探索 [%s 第%s層] AP:%d 金幣+%s 經驗+%s 進度:%s 結果:%s",
								info.areaInner.get(Integer
										.parseInt(info.front.areaId)).areaName,
								info.front.floorId, info.ap, info.ExploreGold,
								info.ExploreExp, info.ExploreProgress,
								info.ExploreResult), LogType.INFO);
					}
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
					throw ex;
			}
			break;
		case GUILD_BATTLE:
			try {
				if (GuildBattle.run()) {
					String result = "";

					if (!info.events.empty()) {
						switch (info.events.peek()) {
						case guildTopRetry:
							result = "來晚啦";
							break;
						case fairyBattleLose:
							result = "慘敗";
							info.events.pop();
							break;
						case fairyBattleWin:
							result = "勝利";
							info.events.pop();
							break;
						default:
							break;
						}
					}

					String str = String
							.format("守衛戰 [%s] %s Lv%s AP:%d/%d BC:%d/%d 外援書:%d 周貢獻:%s Buff:%s",
									result, info.fairy.fairyName,
									info.fairy.fairyLevel, info.ap, info.apMax,
									info.bc, info.bcMax, info.ticket,
									info.week, info.fairy.Spp);

					Go.log(str, LogType.INFO);

					Thread.sleep(5000);
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
					throw ex;
			}
			break;
		case GUILD_TOP:
			try {
				GuildTop.run();
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
					throw ex;
			}
			break;
		case GET_FAIRY_REWARD:
			try {
				if (GetFairyReward.run()) {
					Go.log(ErrorData.text, LogType.INFO);
					ErrorData.clear();
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
					throw ex;
			}
			break;
		case LV_UP:
			try {
				if (LvUp.run()) {
					Go.log(String.format("恭喜升級! AP:%d BC:%d", info.apMax,
							info.bcMax), LogType.INFO);
				} else {
					Go.log("恭喜升級", LogType.INFO);
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
					throw ex;
			}
			break;
		case SELL_CARD:
			try {
				if (SellCard.run()) {
					Go.log(ErrorData.text.replace("\n", ""), LogType.INFO);
					ErrorData.clear();

					Process.info.isCardFull = false;
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
					throw ex;
			}
			break;
		case PFB_GOOD:
			try {
				if (PFBGood.run()) {
					Go.log(ErrorData.text, LogType.INFO);
					ErrorData.clear();
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
					throw ex;
			}
			break;
		case RECV_PFB_GOOD:
			try {
				if (RecvPFBGood.run()) {
					Go.log(ErrorData.text, LogType.INFO);
					ErrorData.clear();
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
					throw ex;
			}
			break;
		case USE:
			try {
				if (Use.run()) {
					Go.log(ErrorData.text.replace("\n", ""), LogType.INFO);
					ErrorData.clear();

					Go.log(String
							.format("藥: 全AP:%d 半AP:%d（今日剩餘:%d）  全BC:%d 半BC:%d（今日剩餘:%d）",
									info.fullAp, info.halfAp, info.halfApToday,
									info.fullBc, info.halfBc, info.halfBcToday),
							LogType.INFO);
				}
			} catch (Exception ex) {
				if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
					throw ex;
			}

			break;
		case NOTHING:
			// 无事可做休息10~15秒
			Random rand = new Random();
			int sleep = 10 + rand.nextInt(6);
			String str = "休息" + sleep + "秒, 連續第" + (Info.sleepCount + 1)
					+ "/20次";
			Go.log(str, LogType.INFO);
			Thread.sleep(sleep * 1000);

			break;
		default:
			break;
		}

		// 连续休眠超过20次，重新登录以更新AP和BC
		if (action == Action.NOTHING) {
			if (++Info.sleepCount >= 20) {
				Process.info.events.push(EventType.cookieOutOfDate);
				Info.sleepCount = 0;
			}
		}
	}

	public static Document ParseXMLBytes(byte[] in, String name)
			throws Exception {
		ByteArrayInputStream bais = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			bais = new ByteArrayInputStream(in);
			Document document = builder.parse(bais);

			if (Info.Debug == true) {
				doc2FormatString(document, name);
			}

			return document;
		} catch (ParserConfigurationException e) {
			throw e;
		} catch (SAXException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			if (bais != null)
				bais.close();
		}
	}

	private static void doc2FormatString(Document doc, String name) {
		String docString = "";
		if (doc != null) {
			StringWriter stringWriter = new StringWriter();
			try {
				OutputFormat format = new OutputFormat(doc, "UTF-8", true);

				XMLSerializer serializer = new XMLSerializer(stringWriter,
						format);
				serializer.asDOMSerializer();
				serializer.serialize(doc);
				docString = stringWriter.toString();
			} catch (Exception e) {
				e.printStackTrace();

				if (stringWriter != null)
					try {
						stringWriter.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
			} finally {
				if (stringWriter != null) {
					try {
						stringWriter.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		File f = new File("xml/");

		if (!f.exists()) {
			f.mkdirs();
		}

		File fp = new File(String.format("xml/%s-" + name + ".xml",
				new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date())
						.toString()));

		try {
			PrintWriter pfp = new PrintWriter(fp);
			pfp.print(docString);
			pfp.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
}