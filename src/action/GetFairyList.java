package action;

import info.BattleDB;
import info.FairyBattleInfo;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.NameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import walker.ErrorData;
import walker.Go;
import walker.Info;
import walker.Process;
import walker.Go.LogType;

/**
 * put_down含义：<br/>
 * <ul>
 * <li>1: 妖精</li>
 * <li>2: 已消灭</li>
 * <li>3: 超时</li>
 * <li>4: 活动妖精</li>
 * <li>5: 可赞</li>
 * </ul>
 */
public class GetFairyList {
	public static final Action Name = Action.GET_FAIRY_LIST;
	private static final String URL_FAIRY_LIST = "http://web.million-arthurs.com/connect/app/private_fairy/private_fairy_select?cyt=1";
	private static byte[] response;

	public static boolean run() throws Exception {
		try {
			response = Process.network.ConnectToServer(URL_FAIRY_LIST,
					new ArrayList<NameValuePair>(), false);
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.text;
			ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
			ErrorData.text = ex.getMessage();
			throw ex;
		}

		Document doc;
		try {
			doc = Process.ParseXMLBytes(response, Name.name());
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.FairyListDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		try {
			return parse(doc);
		} catch (Exception ex) {
			throw ex;
		}
	}

	private static boolean parse(Document doc) throws Exception {
		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		try {
			if (!xpath.evaluate("/response/header/error/code", doc).equals("0")) {
				ErrorData.currentErrorType = ErrorData.ErrorType.FairyListResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			if (!xpath.evaluate("//remaining_rewards", doc).equals("0")) {
				Process.info.events.push(Info.EventType.fairyReward);
			}

			// 获取放妖的玩家
			NodeList userNodeList = (NodeList) xpath.evaluate(
					"//fairy_select/user", doc, XPathConstants.NODESET);
			for (int i = 0; i < userNodeList.getLength(); i++) {
				String id = null;
				String name = null;
				Node f = userNodeList.item(i).getFirstChild();

				do {
					String nodeName = f.getNodeName();
					String nodeValue = f.getFirstChild().getNodeValue();

					if (nodeName.equals("name")) {
						name = nodeValue;
					} else if (nodeName.equals("id")) {
						id = nodeValue;
					}

					f = f.getNextSibling();
				} while (f != null);

				if ((id != null) && (!BattleDB.userMap.containsKey(id))) {
					BattleDB.userMap.put(id, name);
				}
			}// end for

			// 妖精
			NodeList fairy = (NodeList) xpath.evaluate(
					"//fairy_select/fairy_event[put_down=1]/fairy", doc,
					XPathConstants.NODESET);

			// 可赞的
			NodeList fairy1 = (NodeList) xpath.evaluate(
					"//fairy_select/fairy_event[put_down=5]/fairy", doc,
					XPathConstants.NODESET);

			// 活动妖精
			NodeList eventFairy = (NodeList) xpath.evaluate(
					"//fairy_select/fairy_event[put_down=4]/fairy", doc,
					XPathConstants.NODESET);

			if (fairy.getLength() <= 2) {
				if (fairy1.getLength() > 0) {
					Go.log("找到" + fairy1.getLength() + "个可赞的妹纸啦...",
							LogType.INFO);
				}

				for (int i = 0; i < fairy1.getLength(); i++) {
					Node f = fairy1.item(i).getFirstChild();
					String serial_Id = "";
					String user_Id = "";

					do {
						if (f.getNodeName().equals("serial_id")) {
							serial_Id = f.getFirstChild().getNodeValue();
						} else if (f.getNodeName().equals("discoverer_id")) {
							user_Id = f.getFirstChild().getNodeValue();
						}
						f = f.getNextSibling();
					} while (f != null);

					Process.info.PFBGoodList.push(new info.PFBGood(serial_Id,
							user_Id));
				}// end for

				if (!Process.info.PFBGoodList.isEmpty()) {
					Process.info.events.push(Info.EventType.PFBGood);
				}

				Process.info.SetTimeoutByAction(Name);
			}

			Process.info.ownFairyKilled = true;
			BattleDB.notBattleSortedSet.clear();
			for (int i = 0; i < fairy.getLength(); i++) {
				Node child = fairy.item(i).getFirstChild();
				FairyBattleInfo fbi = new FairyBattleInfo();

				do {
					String nodeName = child.getNodeName();
					String nodeValue = child.getFirstChild().getNodeValue();

					if (nodeName.equals("hp")) {
						fbi.hp = nodeValue;
					} else if (nodeName.equals("serial_id")) {
						Process.info.fairy.serialId = nodeValue;
						fbi.serialId = nodeValue;
					} else if (nodeName.equals("discoverer_id")) {
						Process.info.fairy.userId = nodeValue;
						fbi.discoverer_id = nodeValue;
						fbi.discoverer = ((String) BattleDB.userMap
								.get(fbi.discoverer_id));

						// 判断自己妖精是否已死
						if (Process.info.userId.equals(fbi.discoverer_id) == true) {
							Process.info.ownFairyKilled = false;
						}
					} else if (nodeName.equals("lv")) {
						Process.info.fairy.fairyLevel = nodeValue;
						fbi.fairyLevel = nodeValue;
					} else if (nodeName.equals("name")) {
						Process.info.fairy.fairyName = nodeValue;
						fbi.fairyName = nodeValue;
					} else if (nodeName.equals("rare_flg")) {
						fbi.rare_flg = nodeValue;
					} else if (nodeName.equals("hp_max")) {
						fbi.hp_max = nodeValue;
					} else if (nodeName.equals("time_limit")) {
						fbi.time_limit = String.valueOf(Integer
								.valueOf(nodeValue) / 60);
					}

					child = child.getNextSibling();
				} while (child != null); // end while

				if ((fbi.rare_flg.equals("1"))
						&& (Process.info.bc > Info.rareFairyDeck.battleCost + 20)) {
					fbi.deckNo = Info.rareFairyDeck.no;
				} else if ((Integer.valueOf(fbi.hp).intValue() < Info.killFairyHp)
						&& (Process.info.bc > Info.koDeck.battleCost + 20)) {
					fbi.deckNo = Info.koDeck.no;

					if (BattleDB.hadBattleArrayList.contains(fbi)) {
						BattleDB.hadBattleArrayList.remove(fbi);
					}
				} else {
					fbi.deckNo = Info.fairyDeck.no;
				}

				// 更新未战斗列表，将剩余时间大于1分钟且未打过的妖精放入
				if (BattleDB.hadBattleArrayList.contains(fbi) == false
						&& Integer.parseInt(fbi.time_limit) > 1) {
					BattleDB.notBattleSortedSet.add(fbi);
				}
			} // end of for

			// 活动妖精
			if (BattleDB.notBattleSortedSet.isEmpty() == true) {
				for (int i = 0; i < eventFairy.getLength(); i++) {
					Node child = eventFairy.item(i).getFirstChild();
					FairyBattleInfo fbi = new FairyBattleInfo();

					do {
						String nodeName = child.getNodeName();
						String nodeValue = child.getFirstChild().getNodeValue();

						if (nodeName.equals("hp")) {
							fbi.hp = nodeValue;
						} else if (nodeName.equals("serial_id")) {
							Process.info.fairy.serialId = nodeValue;
							fbi.serialId = nodeValue;
						} else if (nodeName.equals("discoverer_id")) {
							Process.info.fairy.userId = nodeValue;
							fbi.discoverer_id = nodeValue;
							fbi.discoverer = ((String) BattleDB.userMap
									.get(fbi.discoverer_id));

							// 判断自己妖精是否已死
							if (Process.info.userId.equals(fbi.discoverer_id) == true) {
								Process.info.ownFairyKilled = false;
							}
						} else if (nodeName.equals("lv")) {
							Process.info.fairy.fairyLevel = nodeValue;
							fbi.fairyLevel = nodeValue;
						} else if (nodeName.equals("name")) {
							Process.info.fairy.fairyName = nodeValue;
							fbi.fairyName = nodeValue;
						} else if (nodeName.equals("rare_flg")) {
							fbi.rare_flg = nodeValue;
						} else if (nodeName.equals("hp_max")) {
							fbi.hp_max = nodeValue;
						} else if (nodeName.equals("time_limit")) {
							fbi.time_limit = String.valueOf(Integer
									.valueOf(nodeValue) / 60);
						}

						child = child.getNextSibling();
					} while (child != null); // end while

					// 使用外敌卡组
					fbi.deckNo = Info.guildFairyDeck.no;

					// 更新未战斗列表，将剩余时间大于1分钟且未打过的妖精放入
					if (BattleDB.hadBattleArrayList.contains(fbi) == false
							&& Integer.parseInt(fbi.time_limit) > 1) {
						BattleDB.notBattleSortedSet.add(fbi);
					}
				} // end of for
			}// end if

			// 下次优先探索
			if (Info.FairyBattleFirst == true
					&& BattleDB.notBattleSortedSet.size() == 0) {
				BattleDB.wantBattleFlag = false;
				return true;
			}

			// 多于一个妖精，主要用在设置不优先打妖精的情况下
			if (BattleDB.notBattleSortedSet.size() > 1) {
				Process.info.events.push(Info.EventType.fairyAppear);
			}

			if (BattleDB.notBattleSortedSet.size() > 0) {
				Process.info.events.push(Info.EventType.fairyCanBattle);
				Process.info.SetTimeoutByAction(Name);

				Go.log("未戰鬥列表 :", LogType.INFO);
				Go.log("───────────────────", LogType.INFO);
				for (FairyBattleInfo fbi : BattleDB.notBattleSortedSet) {
					Go.log(fbi.toString(), LogType.INFO);
				}
				Go.log("───────────────────", LogType.INFO);
			}
		} catch (Exception ex) {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none)
				throw ex;
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.FairyListDataParseError;
			ErrorData.bytes = response;
			throw ex;
		}

		return true;
	}
}
